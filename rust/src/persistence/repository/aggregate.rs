use chrono::{DateTime, Utc};
use sqlx::{PgConnection, types::Json};
use uuid::Uuid;

use crate::{
    aggregate::{Aggregate, AggregateEntity, AggregateType},
    datetime::LocalDateTime,
    error::{Error, Result},
};

/// The latest snapshot covering events up to the point in time.
pub async fn find_latest_up_to(
    conn: &mut PgConnection,
    assessment_uuid: Uuid,
    aggregate_type: AggregateType,
    point_in_time: LocalDateTime,
) -> Result<Option<AggregateEntity>> {
    let row = sqlx::query!(
        r#"
            SELECT id, uuid, position, version, updated_at, events_from, events_to, events_applied,
                   assessment_uuid, data AS "data: Json<Aggregate>"
            FROM aggregate
            WHERE assessment_uuid = $1
              AND data_type = $2
              AND events_to <= $3
            ORDER BY position DESC
            LIMIT 1
        "#,
        assessment_uuid,
        aggregate_type.name(),
        point_in_time.to_utc(),
    )
    .fetch_optional(&mut *conn)
    .await?;

    Ok(row.map(|row| AggregateEntity {
        id: Some(row.id),
        uuid: row.uuid,
        position: Some(row.position),
        version: row.version,
        updated_at: from_db(row.updated_at),
        events_from: from_db(row.events_from),
        events_to: from_db(row.events_to),
        events_applied: row.events_applied,
        assessment_uuid: row.assessment_uuid,
        data: row.data.0,
    }))
}

pub async fn find_max_position(
    conn: &mut PgConnection,
    assessment_uuid: Uuid,
    aggregate_type: AggregateType,
) -> Result<Option<i32>> {
    Ok(sqlx::query_scalar!(
        r#"
            SELECT position
            FROM aggregate
            WHERE assessment_uuid = $1 AND data_type = $2
            ORDER BY position DESC
            LIMIT 1
        "#,
        assessment_uuid,
        aggregate_type.name(),
    )
    .fetch_optional(&mut *conn)
    .await?)
}

/// Returns the generated id.
pub async fn insert(conn: &mut PgConnection, aggregate: &AggregateEntity) -> Result<i64> {
    let position = aggregate
        .position
        .ok_or_else(|| Error::Internal(format!("Aggregate {} has no position", aggregate.uuid)))?;

    Ok(sqlx::query_scalar!(
        r#"
            INSERT INTO aggregate (uuid, updated_at, events_from, events_to, events_applied, data, assessment_uuid, version, position)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
            RETURNING id
        "#,
        aggregate.uuid,
        aggregate.updated_at.to_utc(),
        aggregate.events_from.to_utc(),
        aggregate.events_to.to_utc(),
        aggregate.events_applied,
        serde_json::to_value(&aggregate.data)?,
        aggregate.assessment_uuid,
        aggregate.version,
        position,
    )
    .fetch_one(&mut *conn)
    .await?)
}

/// Fails with [`Error::OptimisticLock`] if the snapshot was changed since it was read.
pub async fn update(conn: &mut PgConnection, id: i64, aggregate: &AggregateEntity) -> Result<()> {
    let result = sqlx::query!(
        r#"
            UPDATE aggregate
            SET updated_at = $1, events_to = $2, events_applied = $3, data = $4, version = version + 1
            WHERE id = $5 AND version = $6
        "#,
        aggregate.updated_at.to_utc(),
        aggregate.events_to.to_utc(),
        aggregate.events_applied,
        serde_json::to_value(&aggregate.data)?,
        id,
        aggregate.version,
    )
    .execute(&mut *conn)
    .await?;

    if result.rows_affected() == 0 {
        return Err(Error::OptimisticLock(format!(
            "Aggregate {} was updated or deleted by another transaction",
            aggregate.uuid
        )));
    }
    Ok(())
}

pub async fn delete_by_assessment_uuid(conn: &mut PgConnection, assessment_uuid: Uuid) -> Result<()> {
    sqlx::query!("DELETE FROM aggregate WHERE assessment_uuid = $1", assessment_uuid)
        .execute(&mut *conn)
        .await?;
    Ok(())
}

fn from_db(value: DateTime<Utc>) -> LocalDateTime {
    LocalDateTime::from_utc(value)
}
