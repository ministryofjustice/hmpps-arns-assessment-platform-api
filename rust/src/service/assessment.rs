use std::sync::Arc;

use sqlx::PgConnection;
use uuid::Uuid;

use crate::{
    AppState,
    clock::Clock,
    datetime::LocalDateTime,
    error::{Error, Result},
    persistence::{entity::AssessmentEntity, repository::assessment},
    query::AssessmentIdentifier,
    service::state,
};

pub async fn find_by_uuid(conn: &mut PgConnection, uuid: Uuid) -> Result<AssessmentEntity> {
    assessment::find_by_uuid(conn, uuid)
        .await?
        .ok_or_else(|| Error::assessment_not_found(&uuid.into()))
}

/// Find an assessment by UUID, or by the external identifier it had at the point in time.
pub async fn find_by(
    conn: &mut PgConnection,
    identifier: &AssessmentIdentifier,
    point_in_time: LocalDateTime,
) -> Result<Arc<AssessmentEntity>> {
    let uuid = match identifier {
        AssessmentIdentifier::Uuid(by_uuid) => Some(by_uuid.uuid),
        AssessmentIdentifier::External(external) => {
            assessment::find_uuid_by_external_identifier(
                conn,
                external.identifier_type,
                &external.identifier,
                &external.assessment_type,
                point_in_time,
            )
            .await?
        }
    };

    let found = match uuid {
        Some(uuid) => assessment::find_by_uuid(conn, uuid).await?,
        None => None,
    };
    found
        .map(Arc::new)
        .ok_or_else(|| Error::assessment_not_found(identifier))
}

pub async fn delete(app: &AppState, uuid: Uuid) -> Result<()> {
    let mut conn = app.pool.acquire().await?;
    find_by_uuid(&mut conn, uuid).await?;
    assessment::delete(&mut conn, uuid).await
}

/// Replace all of the assessment's snapshots with ones rebuilt from its events.
pub async fn rebuild_aggregates(app: &AppState, clock: &Clock, principal: &str, uuid: Uuid) -> Result<()> {
    let mut tx = app.pool.begin().await?;
    let assessment = Arc::new(find_by_uuid(&mut tx, uuid).await?);

    state::delete(&mut tx, uuid).await?;
    let rebuilt = state::rebuild_from_events(&mut tx, clock, &assessment, Some(Clock::system().now())).await?;
    let aggregate_count: usize = rebuilt.values().map(|state| state.aggregates.len()).sum();
    let evicted = state::persist(&mut tx, &mut [(uuid, rebuilt)].into_iter().collect()).await?;

    tx.commit().await?;

    for assessment_uuid in evicted {
        app.cache.evict_latest(assessment_uuid);
    }
    app.audit.audit(
        principal,
        "RebuiltAggregates",
        format!("Rebuilt all ({aggregate_count}) aggregates for assessment {uuid}"),
    );
    Ok(())
}
