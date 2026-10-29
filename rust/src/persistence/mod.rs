pub mod entity;
pub mod repository;

use std::{collections::HashMap, sync::Arc};

use sqlx::PgConnection;
use uuid::Uuid;

use crate::{
    aggregate::{AggregateState, AggregateType, StateCollection},
    clock::Clock,
    datetime::LocalDateTime,
    error::Result,
    model::{Timeline, UserDetails},
    service::{assessment, event, state, timeline},
};
use entity::{AssessmentEntity, EventEntity, TimelineDraft, TimelineEntity, UserDetailsEntity};
use repository::user_details;

/// Everything loaded or created while handling a batch of commands, written to the database
/// in one go by [`PersistenceContext::persist`].
#[derive(Default)]
pub struct PersistenceContext {
    pub state: StateCollection,
    pub events: Vec<EventEntity>,
    pub timeline: Vec<TimelineEntity>,
    pub user_details: Vec<UserDetailsEntity>,
    pub assessments: Vec<Arc<AssessmentEntity>>,
    /// Assessments whose cached latest version is stale once the transaction commits.
    pub evictions: Vec<Uuid>,
}

impl PersistenceContext {
    pub async fn find_assessment(&mut self, conn: &mut PgConnection, uuid: Uuid) -> Result<Arc<AssessmentEntity>> {
        if let Some(found) = self.assessments.iter().find(|assessment| assessment.uuid == uuid) {
            return Ok(Arc::clone(found));
        }
        let found = Arc::new(assessment::find_by_uuid(conn, uuid).await?);
        self.assessments.push(Arc::clone(&found));
        Ok(found)
    }

    /// The user's existing details, or new details that will be saved with everything else.
    pub async fn find_user_details(
        &mut self,
        conn: &mut PgConnection,
        user: &UserDetails,
    ) -> Result<UserDetailsEntity> {
        if let Some(found) = self
            .user_details
            .iter()
            .find(|entity| entity.user_id == user.id && entity.auth_source == user.auth_source)
        {
            return Ok(found.clone());
        }
        let found = match user_details::find_by_user_id_and_auth_source(conn, &user.id, user.auth_source).await? {
            Some(existing) => existing,
            None => UserDetailsEntity {
                id: None,
                uuid: Uuid::new_v4(),
                user_id: user.id.clone(),
                display_name: user.name.clone(),
                auth_source: user.auth_source,
            },
        };
        self.user_details.push(found.clone());
        Ok(found)
    }

    /// The aggregate state for the assessment, loading (or building) it the first time it's needed.
    pub async fn aggregate_state_mut(
        &mut self,
        conn: &mut PgConnection,
        clock: &Clock,
        assessment: &Arc<AssessmentEntity>,
        aggregate_type: AggregateType,
        point_in_time: Option<LocalDateTime>,
    ) -> Result<&mut AggregateState> {
        let assessment_state = self.state.entry(assessment.uuid).or_default();
        if !assessment_state.contains_key(&aggregate_type) {
            let fetched = state::fetch_or_create_state(conn, clock, assessment, aggregate_type, point_in_time).await?;
            assessment_state.insert(aggregate_type, fetched);
        }
        Ok(assessment_state
            .get_mut(&aggregate_type)
            .expect("aggregate state was just inserted"))
    }

    /// Complete the timeline entries produced by events with the command's custom timeline.
    pub fn add_timeline(&mut self, drafts: Vec<TimelineDraft>, custom: Option<&Timeline>) {
        self.timeline.extend(drafts.iter().map(|draft| draft.complete(custom)));
    }

    pub async fn persist(&mut self, conn: &mut PgConnection) -> Result<()> {
        let mut new_user_ids = HashMap::new();
        for user in self.user_details.iter_mut().filter(|user| user.id.is_none()) {
            let id = user_details::insert(conn, user).await?;
            user.id = Some(id);
            new_user_ids.insert(user.uuid, id);
        }
        self.fill_in_assignee_ids(&new_user_ids);

        for assessment in self.assessments.iter().filter(|assessment| assessment.id.is_none()) {
            repository::assessment::insert(conn, assessment).await?;
        }

        event::save_all(conn, &mut self.events).await?;
        let evicted = state::persist(conn, &mut self.state).await?;
        self.evictions.extend(evicted);
        timeline::save_all(conn, &mut self.timeline).await?;

        self.user_details.clear();
        self.assessments.clear();
        self.events.clear();
        self.state.clear();
        self.timeline.clear();
        Ok(())
    }

    /// Assignment timeline entries embed the assigned user's details. A user created in this unit
    /// of work only has an id once saved, so fill it in to record the same details as the Kotlin
    /// service did.
    fn fill_in_assignee_ids(&mut self, new_user_ids: &HashMap<Uuid, i64>) {
        if new_user_ids.is_empty() {
            return;
        }
        for entry in &mut self.timeline {
            let Some(serde_json::Value::Object(assignee)) = entry.data.get_mut("assignee") else {
                continue;
            };
            let id = assignee
                .get("uuid")
                .and_then(serde_json::Value::as_str)
                .and_then(|uuid| uuid.parse::<Uuid>().ok())
                .and_then(|uuid| new_user_ids.get(&uuid));
            if let Some(id) = id {
                assignee.insert("id".to_owned(), (*id).into());
            }
        }
    }
}
