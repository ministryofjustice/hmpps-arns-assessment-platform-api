//! Building and saving aggregate state.

use std::{future::Future, pin::Pin, sync::Arc};

use sqlx::PgConnection;
use uuid::Uuid;

use crate::{
    aggregate::{AggregateEntity, AggregateState, AggregateType, State, StateCollection},
    clock::Clock,
    datetime::LocalDateTime,
    error::{Error, Result},
    event::bus,
    persistence::{
        PersistenceContext,
        entity::AssessmentEntity,
        repository::{aggregate, event},
    },
};

type BoxFuture<'a, T> = Pin<Box<dyn Future<Output = T> + Send + 'a>>;

/// The state of the aggregate at the point in time, or the latest state when there isn't one.
///
/// Boxed because building state replays events, and replaying a rollback event builds state.
pub fn fetch_or_create_state<'a>(
    conn: &'a mut PgConnection,
    clock: &'a Clock,
    assessment: &'a Arc<AssessmentEntity>,
    aggregate_type: AggregateType,
    point_in_time: Option<LocalDateTime>,
) -> BoxFuture<'a, Result<AggregateState>> {
    Box::pin(async move {
        match point_in_time.map(LocalDateTime::to_database_precision) {
            None => fetch_or_create_latest_state(conn, clock, assessment, aggregate_type).await,
            Some(point_in_time) if point_in_time < assessment.created_at => Err(Error::invalid_timestamp(
                point_in_time,
                "Timestamp cannot be before the assessment created date",
            )),
            Some(point_in_time) => {
                create_point_in_time_state_from_aggregate(conn, clock, assessment, aggregate_type, point_in_time).await
            }
        }
    })
}

async fn fetch_or_create_latest_state(
    conn: &mut PgConnection,
    clock: &Clock,
    assessment: &Arc<AssessmentEntity>,
    aggregate_type: AggregateType,
) -> Result<AggregateState> {
    match aggregate::find_latest_up_to(conn, assessment.uuid, aggregate_type, clock.now()).await? {
        Some(latest) => Ok(AggregateState::new(latest)),
        None => create_point_in_time_state_from_events(conn, clock, assessment, aggregate_type, clock.now()).await,
    }
}

/// Start from the latest snapshot before the point in time and replay any events since.
async fn create_point_in_time_state_from_aggregate(
    conn: &mut PgConnection,
    clock: &Clock,
    assessment: &Arc<AssessmentEntity>,
    aggregate_type: AggregateType,
    point_in_time: LocalDateTime,
) -> Result<AggregateState> {
    let Some(snapshot) = aggregate::find_latest_up_to(conn, assessment.uuid, aggregate_type, point_in_time).await?
    else {
        return create_point_in_time_state_from_events(conn, clock, assessment, aggregate_type, point_in_time).await;
    };

    if snapshot.events_to == point_in_time {
        return Ok(AggregateState::new(snapshot));
    }

    let events_between = event::find_all_between(conn, assessment, snapshot.events_to, point_in_time).await?;
    if events_between.is_empty() {
        return Ok(AggregateState::new(snapshot));
    }

    let mut context = PersistenceContext::default();
    context.state.insert(
        assessment.uuid,
        State::from([(aggregate_type, AggregateState::new(snapshot))]),
    );
    bus::handle_all(&mut context, conn, clock, events_between).await?;

    context
        .state
        .swap_remove(&assessment.uuid)
        .and_then(|mut state| state.swap_remove(&aggregate_type))
        .ok_or_else(|| Error::Internal("Replaying events lost the aggregate state".to_owned()))
}

async fn create_point_in_time_state_from_events(
    conn: &mut PgConnection,
    clock: &Clock,
    assessment: &Arc<AssessmentEntity>,
    aggregate_type: AggregateType,
    point_in_time: LocalDateTime,
) -> Result<AggregateState> {
    let mut rebuilt = rebuild_from_events(conn, clock, assessment, Some(point_in_time)).await?;
    Ok(rebuilt
        .swap_remove(&aggregate_type)
        .unwrap_or_else(|| blank_state(clock, assessment, aggregate_type)))
}

/// Replay every event up to the point in time (or now) onto blank aggregates.
pub async fn rebuild_from_events(
    conn: &mut PgConnection,
    clock: &Clock,
    assessment: &Arc<AssessmentEntity>,
    point_in_time: Option<LocalDateTime>,
) -> Result<State> {
    let events =
        event::find_all_for_point_in_time(conn, assessment, point_in_time.unwrap_or_else(|| clock.now())).await?;

    let blank = State::from([(
        AggregateType::AssessmentAggregate,
        blank_state(clock, assessment, AggregateType::AssessmentAggregate),
    )]);
    let mut context = PersistenceContext::default();
    context.state.insert(assessment.uuid, blank.clone());

    bus::handle_all(&mut context, conn, clock, events).await?;

    Ok(context.state.swap_remove(&assessment.uuid).unwrap_or(blank))
}

fn blank_state(clock: &Clock, assessment: &AssessmentEntity, aggregate_type: AggregateType) -> AggregateState {
    AggregateState::new(AggregateEntity::new(
        assessment.uuid,
        aggregate_type.blank(),
        assessment.created_at,
        assessment.created_at,
        clock.now(),
    ))
}

/// Save new snapshots and update changed ones. Returns the assessments whose cached latest
/// version is now stale.
pub async fn persist(conn: &mut PgConnection, state: &mut StateCollection) -> Result<Vec<Uuid>> {
    for (assessment_uuid, assessment_state) in state.iter_mut() {
        for (aggregate_type, aggregate_state) in assessment_state.iter_mut() {
            let max_position = aggregate::find_max_position(conn, *assessment_uuid, *aggregate_type)
                .await?
                .unwrap_or(-1);

            for (index, snapshot) in aggregate_state.aggregates.iter_mut().enumerate() {
                match snapshot.id {
                    Some(id) => aggregate::update(conn, id, snapshot).await?,
                    None => {
                        snapshot.position = Some(max_position + 1 + index as i32);
                        snapshot.id = Some(aggregate::insert(conn, snapshot).await?);
                    }
                }
            }
        }
    }
    Ok(state.keys().copied().collect())
}

pub async fn delete(conn: &mut PgConnection, assessment_uuid: Uuid) -> Result<()> {
    aggregate::delete_by_assessment_uuid(conn, assessment_uuid).await
}
