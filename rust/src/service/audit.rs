//! Audit events, sent to the HMPPS audit SQS queue.

use std::sync::Arc;

use aws_sdk_sqs::types::SendMessageBatchRequestEntry;
use chrono::{SecondsFormat, Utc};
use serde::Serialize;
use serde_json::json;
use tokio::sync::OnceCell;

use crate::{
    command::Command,
    config::Config,
    query::{Query, SubjectAccessRequestQuery},
};

/// SQS accepts at most ten messages per batch.
const BATCH_SIZE: usize = 10;

#[derive(Serialize)]
struct AuditableEvent {
    who: String,
    what: String,
    when: String,
    service: String,
    details: String,
}

#[derive(Clone)]
pub struct AuditService {
    inner: Arc<Inner>,
}

struct Inner {
    service_name: String,
    queue: Option<AuditQueue>,
}

struct AuditQueue {
    client: aws_sdk_sqs::Client,
    name: String,
    url: OnceCell<String>,
}

impl AuditService {
    pub async fn new(config: &Config) -> Self {
        let queue = match &config.audit.queue_name {
            None => {
                tracing::warn!("No audit queue configured, so audit events will only be logged");
                None
            }
            Some(name) => {
                let region = std::env::var("AWS_REGION").unwrap_or_else(|_| "eu-west-2".to_owned());
                let mut loader =
                    aws_config::defaults(aws_config::BehaviorVersion::latest()).region(aws_config::Region::new(region));
                if let Some(url) = &config.audit.localstack_url {
                    loader = loader
                        .endpoint_url(url)
                        .credentials_provider(aws_sdk_sqs::config::Credentials::new(
                            "localstack",
                            "localstack",
                            None,
                            None,
                            "localstack",
                        ));
                }
                Some(AuditQueue {
                    client: aws_sdk_sqs::Client::new(&loader.load().await),
                    name: name.clone(),
                    url: OnceCell::new(),
                })
            }
        };

        Self {
            inner: Arc::new(Inner {
                service_name: config.service_name.clone(),
                queue,
            }),
        }
    }

    pub fn disabled(service_name: &str) -> Self {
        Self {
            inner: Arc::new(Inner {
                service_name: service_name.to_owned(),
                queue: None,
            }),
        }
    }

    pub fn audit_commands(&self, commands: &[Command]) {
        let events = commands
            .iter()
            .map(|command| {
                self.event(
                    &command.user().id,
                    command.name(),
                    json!({ "assessmentUuid": command.assessment_uuid() }),
                    now(),
                )
            })
            .collect();
        self.send(events);
    }

    pub fn audit_query(&self, query: &Query) {
        let details = match query {
            Query::AssessmentVersionQuery(query) => json!({ "assessmentIdentifier": query.assessment_identifier }),
            Query::CollectionItemQuery(query) => json!({ "assessmentIdentifier": query.assessment_identifier }),
            Query::CollectionQuery(query) => json!({ "assessmentIdentifier": query.assessment_identifier }),
            Query::DailyVersionsQuery(query) => json!({ "assessmentIdentifier": query.assessment_identifier }),
            Query::TimelineQuery(query) => {
                let mut details = serde_json::Map::new();
                if let Some(identifier) = &query.assessment_identifier {
                    details.insert("assessmentIdentifier".to_owned(), json!(identifier));
                }
                if let Some(subject) = &query.subject {
                    details.insert("subject".to_owned(), json!(subject.id));
                }
                serde_json::Value::Object(details)
            }
            Query::GetAssessmentsModifiedSinceQuery(query) => {
                json!({ "assessmentType": query.assessment_type, "since": query.since })
            }
            Query::GetAssessmentsSoftDeletedSinceQuery(query) => {
                json!({ "assessmentType": query.assessment_type, "since": query.since })
            }
        };
        self.send(vec![self.event(&query.user().id, query.name(), details, now())]);
    }

    pub fn audit_subject_access_request(&self, query: &SubjectAccessRequestQuery) {
        let details = json!({
            "subject": query.assessment_identifiers,
            "from": query.from,
            "to": query.to,
            "timestamp": query.timestamp.map(|timestamp| instant(timestamp.to_utc())),
        });
        self.send(vec![self.event("SAR", "SubjectAccessRequestQuery", details, now())]);
    }

    pub fn audit(&self, who: &str, what: &str, details: String) {
        let event = AuditableEvent {
            who: who.to_owned(),
            what: what.to_owned(),
            when: now(),
            service: self.inner.service_name.clone(),
            details,
        };
        self.send(vec![event]);
    }

    fn event(&self, who: &str, what: &str, details: serde_json::Value, when: String) -> AuditableEvent {
        AuditableEvent {
            who: who.to_owned(),
            what: what.to_owned(),
            when,
            service: self.inner.service_name.clone(),
            details: details.to_string(),
        }
    }

    /// Sent in the background, so auditing never fails or slows down a request.
    fn send(&self, events: Vec<AuditableEvent>) {
        if events.is_empty() {
            return;
        }
        let inner = Arc::clone(&self.inner);
        tokio::spawn(async move {
            let Some(queue) = &inner.queue else {
                for event in &events {
                    tracing::info!(who = %event.who, what = %event.what, details = %event.details, "Audit event");
                }
                return;
            };
            if let Err(error) = queue.send(&events).await {
                tracing::error!(%error, "Error during sending audit messages");
            }
        });
    }
}

impl AuditQueue {
    async fn send(&self, events: &[AuditableEvent]) -> Result<(), String> {
        let url = self
            .url
            .get_or_try_init(|| async {
                self.client
                    .get_queue_url()
                    .queue_name(&self.name)
                    .send()
                    .await
                    .map_err(|error| format!("Unable to find audit queue '{}': {error}", self.name))?
                    .queue_url()
                    .map(str::to_owned)
                    .ok_or_else(|| format!("Audit queue '{}' has no URL", self.name))
            })
            .await?;

        let batches: Vec<_> = events.chunks(BATCH_SIZE).collect();
        for (batch_index, batch) in batches.iter().enumerate() {
            let first = batch_index * BATCH_SIZE;
            tracing::info!(
                "Sending batch {}/{} - audit events {} to {}",
                batch_index + 1,
                batches.len(),
                first,
                first + batch.len()
            );

            let entries = batch
                .iter()
                .enumerate()
                .map(|(index, event)| {
                    let body = serde_json::to_string(event).map_err(|error| error.to_string())?;
                    SendMessageBatchRequestEntry::builder()
                        .id(format!("msg-{}", first + index))
                        .message_body(body)
                        .build()
                        .map_err(|error| error.to_string())
                })
                .collect::<Result<Vec<_>, _>>()?;

            self.client
                .send_message_batch()
                .queue_url(url)
                .set_entries(Some(entries))
                .send()
                .await
                .map_err(|error| error.to_string())?;
        }

        tracing::info!("Successfully sent {} audit events", events.len());
        Ok(())
    }
}

fn now() -> String {
    instant(Utc::now())
}

fn instant(value: chrono::DateTime<Utc>) -> String {
    value.to_rfc3339_opts(SecondsFormat::AutoSi, true)
}
