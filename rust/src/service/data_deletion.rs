//! Rewriting an assessment's history, to remove data that should never have been recorded.

use std::{collections::HashMap, sync::Arc};

use indexmap::{IndexMap, IndexSet};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

use crate::{
    AppState,
    clock::Clock,
    error::{Error, Result},
    event::{Event, RedactedEvent},
    model::{AggregateDto, EventDto, TimelineItem},
    persistence::{
        entity::{EventEntity, TimelineEntity},
        repository::{assessment, event, timeline},
    },
    service::{self, state},
};

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum DataDeletionOperation {
    Update,
    Delete,
}

#[derive(Clone, Debug, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct DataDeletionRequest {
    #[serde(default)]
    pub events: Vec<EventUpdate>,
    #[serde(default)]
    pub timeline: Vec<TimelineUpdate>,
    /// Defaults to true: nothing is saved unless this is explicitly false.
    #[serde(default = "dry_run_by_default")]
    pub dry_run: bool,
}

fn dry_run_by_default() -> bool {
    true
}

#[derive(Clone, Debug, Deserialize, ToSchema)]
pub struct EventUpdate {
    pub uuid: Uuid,
    pub operation: DataDeletionOperation,
    /// The replacement event for an update. For a delete, only its type is kept.
    pub event: Event,
}

#[derive(Clone, Debug, Deserialize, ToSchema)]
pub struct TimelineUpdate {
    pub uuid: Uuid,
    pub operation: DataDeletionOperation,
    pub timeline: TimelineItem,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
pub struct DataDeletionDataResponse {
    pub events: Vec<EventDto>,
    pub timeline: Vec<TimelineItem>,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct DataDeletionResponse {
    pub success: bool,
    pub dry_run: bool,
    /// Why the rebuilt history couldn't be replayed, when `success` is false.
    pub exception: Option<EventHandlingFailure>,
    /// The aggregates rebuilt from the updated events, by aggregate type.
    pub rebuilt_state: Option<IndexMap<String, Vec<AggregateDto>>>,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct EventHandlingFailure {
    pub event_uuid: Uuid,
    pub event_name: String,
    pub handler_name: String,
    pub message: String,
    pub developer_message: String,
    pub status_code: String,
}

/// Every event and timeline entry for the assessment, including deleted ones.
pub async fn get_data(app: &AppState, assessment_uuid: Uuid) -> Result<DataDeletionDataResponse> {
    let mut conn = app.pool.acquire().await?;
    let Some(found) = assessment::find_by_uuid(&mut conn, assessment_uuid).await? else {
        return Ok(DataDeletionDataResponse {
            events: Vec::new(),
            timeline: Vec::new(),
        });
    };

    Ok(DataDeletionDataResponse {
        events: event::find_all_including_deleted(&mut conn, &Arc::new(found))
            .await?
            .iter()
            .map(EventDto::from_entity)
            .collect::<Result<_>>()?,
        timeline: timeline::find_all_including_deleted(&mut conn, assessment_uuid)
            .await?
            .iter()
            .map(TimelineItem::from_entity)
            .collect::<Result<_>>()?,
    })
}

/// Replace or redact events and timeline entries, then rebuild the aggregates from the result.
/// A dry run makes the same changes but rolls them back, to preview the rebuilt aggregates.
pub async fn update_data(
    app: &AppState,
    clock: &Clock,
    principal: &str,
    assessment_uuid: Uuid,
    request: DataDeletionRequest,
) -> Result<DataDeletionResponse> {
    let mut tx = app.pool.begin().await?;
    let assessment = Arc::new(service::assessment::find_by_uuid(&mut tx, assessment_uuid).await?);

    let timeline_uuids: Vec<Uuid> = request
        .timeline
        .iter()
        .map(|update| update.uuid)
        .collect::<IndexSet<_>>()
        .into_iter()
        .collect();
    let existing_timeline: HashMap<Uuid, TimelineEntity> =
        timeline::find_by_uuids_including_deleted(&mut tx, &timeline_uuids)
            .await?
            .into_iter()
            .map(|entry| (entry.uuid, entry))
            .collect();
    let mut replacement_timeline = request
        .timeline
        .iter()
        .filter(|update| update.operation == DataDeletionOperation::Update)
        .map(|update| {
            let existing = existing_timeline
                .get(&update.uuid)
                .ok_or_else(|| Error::timeline_not_found(update.uuid))?;
            Ok(TimelineEntity {
                id: None,
                uuid: update.uuid,
                position: existing.position,
                created_at: existing.created_at,
                user: existing.user.clone(),
                assessment_uuid: existing.assessment_uuid,
                event_type: update.timeline.event.clone(),
                data: update.timeline.data.clone(),
                custom_type: update.timeline.custom_type.clone(),
                custom_data: update.timeline.custom_data.clone(),
                deleted: existing.deleted,
            })
        })
        .collect::<Result<Vec<_>>>()?;

    let event_uuids: Vec<Uuid> = request
        .events
        .iter()
        .map(|update| update.uuid)
        .collect::<IndexSet<_>>()
        .into_iter()
        .collect();
    let existing_events: HashMap<Uuid, EventEntity> = event::find_by_uuids_including_deleted(&mut tx, &event_uuids)
        .await?
        .into_iter()
        .map(|entity| (entity.uuid, entity))
        .collect();
    let mut replacement_events = request
        .events
        .iter()
        .map(|update| {
            let existing = existing_events
                .get(&update.uuid)
                .ok_or_else(|| Error::event_not_found(update.uuid))?;
            let data = match update.operation {
                DataDeletionOperation::Update => update.event.clone(),
                DataDeletionOperation::Delete => Event::RedactedEvent(RedactedEvent {
                    event_type: update.event.name().to_owned(),
                    date_redacted: clock.now(),
                    redacted_by: principal.to_owned(),
                }),
            };
            Ok(EventEntity {
                id: None,
                data,
                ..existing.clone()
            })
        })
        .collect::<Result<Vec<_>>>()?;

    let existing_timeline_uuids: Vec<Uuid> = existing_timeline.keys().copied().collect();
    timeline::delete_by_uuids(&mut tx, &existing_timeline_uuids).await?;
    service::timeline::save_all(&mut tx, &mut replacement_timeline).await?;

    let existing_event_uuids: Vec<Uuid> = existing_events.keys().copied().collect();
    event::delete_by_uuids(&mut tx, &existing_event_uuids).await?;
    service::event::save_all(&mut tx, &mut replacement_events).await?;

    let mut evicted = Vec::new();
    let rebuilt_state = if replacement_events.is_empty() {
        None
    } else {
        state::delete(&mut tx, assessment.uuid).await?;
        let mut rebuilt = match state::rebuild_from_events(&mut tx, clock, &assessment, None).await {
            Ok(rebuilt) => IndexMap::from([(assessment.uuid, rebuilt)]),
            Err(Error::EventHandling(failure)) => {
                tx.rollback().await?;
                let exception = EventHandlingFailure {
                    event_uuid: failure.event_uuid,
                    event_name: failure.event_name.clone(),
                    handler_name: failure.handler_name.clone(),
                    developer_message: failure.cause.developer_message(),
                    status_code: "BAD_REQUEST".to_owned(),
                    message: Error::EventHandling(failure).to_string(),
                };
                return Ok(DataDeletionResponse {
                    success: false,
                    dry_run: request.dry_run,
                    exception: Some(exception),
                    rebuilt_state: None,
                });
            }
            Err(other) => return Err(other),
        };
        evicted = state::persist(&mut tx, &mut rebuilt).await?;

        let assessment_state = rebuilt.swap_remove(&assessment.uuid).unwrap_or_default();
        let mut by_type = IndexMap::new();
        for (aggregate_type, aggregate_state) in assessment_state {
            let aggregates = aggregate_state
                .aggregates
                .iter()
                .map(AggregateDto::from_entity)
                .collect::<Result<Vec<_>>>()?;
            by_type.insert(aggregate_type.name().to_owned(), aggregates);
        }
        Some(by_type)
    };

    if request.dry_run {
        tx.rollback().await?;
    } else {
        tx.commit().await?;
        for assessment_uuid in evicted {
            app.cache.evict_latest(assessment_uuid);
        }
        app.audit.audit(
            principal,
            "RewroteHistory",
            format!(
                "Updated {} events and {} timelines.Event UUIDs: [{}] ; Timeline UUIDs: [{}]",
                existing_event_uuids.len(),
                existing_timeline_uuids.len(),
                join(&existing_event_uuids),
                join(&existing_timeline_uuids),
            ),
        );
    }

    Ok(DataDeletionResponse {
        success: true,
        dry_run: request.dry_run,
        exception: None,
        rebuilt_state,
    })
}

fn join(uuids: &[Uuid]) -> String {
    uuids.iter().map(Uuid::to_string).collect::<Vec<_>>().join(", ")
}
