use sqlx::PgConnection;

use crate::{
    aggregate::{AggregateType, assessment},
    clock::Clock,
    error::{Error, Result},
    persistence::{
        PersistenceContext,
        entity::{EventEntity, TimelineDraft},
    },
};

/// Apply an event to every aggregate derived from it, and record it in the context. Returns the
/// timeline entries the event produced, for the caller to complete with any custom timeline.
pub async fn handle(
    context: &mut PersistenceContext,
    conn: &mut PgConnection,
    clock: &Clock,
    event: EventEntity,
) -> Result<Vec<TimelineDraft>> {
    let mut timeline = Vec::new();

    for &aggregate_type in event.data.aggregate_types() {
        let state = context
            .aggregate_state_mut(conn, clock, &event.assessment, aggregate_type, Some(event.created_at))
            .await?;

        let result = match aggregate_type {
            AggregateType::AssessmentAggregate => assessment::handle(&event, state, conn, clock).await,
        };

        timeline.extend(result.map_err(|cause| Error::event_handling(event.uuid, event.data.name(), cause))?);
    }

    context.events.push(event);
    Ok(timeline)
}

pub async fn handle_all(
    context: &mut PersistenceContext,
    conn: &mut PgConnection,
    clock: &Clock,
    events: Vec<EventEntity>,
) -> Result<()> {
    for event in events {
        handle(context, conn, clock, event).await?;
    }
    Ok(())
}
