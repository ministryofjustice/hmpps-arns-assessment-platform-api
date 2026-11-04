use std::{collections::HashMap, sync::Arc};

use chrono::{DateTime, Utc};
use sqlx::{PgConnection, types::Json};
use uuid::Uuid;

use super::{assessment, parse_column};
use crate::{
    datetime::LocalDateTime,
    error::{Error, Result},
    event::Event,
    persistence::entity::{AssessmentEntity, EventEntity, UserDetailsEntity},
};

struct EventRow {
    id: i64,
    uuid: Uuid,
    position: i32,
    created_at: DateTime<Utc>,
    data: Json<Event>,
    autosaved: bool,
    deleted: Option<bool>,
    assessment_uuid: Uuid,
    user_pk: i64,
    user_uuid: Uuid,
    user_id: String,
    display_name: String,
    auth_source: String,
}

impl EventRow {
    fn into_entity(self, assessment: Arc<AssessmentEntity>) -> Result<EventEntity> {
        Ok(EventEntity {
            id: Some(self.id),
            uuid: self.uuid,
            position: Some(self.position),
            created_at: LocalDateTime::from_utc(self.created_at),
            user: UserDetailsEntity {
                id: Some(self.user_pk),
                uuid: self.user_uuid,
                user_id: self.user_id,
                display_name: self.display_name,
                auth_source: parse_column(&self.auth_source)?,
            },
            assessment,
            data: self.data.0,
            autosaved: self.autosaved,
            deleted: self.deleted.unwrap_or(false),
        })
    }
}

fn into_entities(rows: Vec<EventRow>, assessment: &Arc<AssessmentEntity>) -> Result<Vec<EventEntity>> {
    rows.into_iter()
        .map(|row| row.into_entity(Arc::clone(assessment)))
        .collect()
}

/// Events up to and including the point in time, in order.
pub async fn find_all_for_point_in_time(
    conn: &mut PgConnection,
    assessment: &Arc<AssessmentEntity>,
    point_in_time: LocalDateTime,
) -> Result<Vec<EventEntity>> {
    let rows = sqlx::query_as!(
        EventRow,
        r#"
            SELECT e.id, e.uuid, e.position, e.created_at, e.data AS "data: Json<Event>", e.autosaved, e.deleted,
                   e.assessment_uuid, u.id AS user_pk, u.uuid AS user_uuid, u.user_id, u.display_name, u.auth_source
            FROM event e
            JOIN user_details u ON u.uuid = e.user_details_uuid
            WHERE e.assessment_uuid = $1
              AND e.created_at <= $2
              AND e.deleted IS FALSE
            ORDER BY e.position
        "#,
        assessment.uuid,
        point_in_time.to_utc(),
    )
    .fetch_all(&mut *conn)
    .await?;
    into_entities(rows, assessment)
}

/// Events after `from` up to and including `to`, in order.
pub async fn find_all_between(
    conn: &mut PgConnection,
    assessment: &Arc<AssessmentEntity>,
    from: LocalDateTime,
    to: LocalDateTime,
) -> Result<Vec<EventEntity>> {
    let rows = sqlx::query_as!(
        EventRow,
        r#"
            SELECT e.id, e.uuid, e.position, e.created_at, e.data AS "data: Json<Event>", e.autosaved, e.deleted,
                   e.assessment_uuid, u.id AS user_pk, u.uuid AS user_uuid, u.user_id, u.display_name, u.auth_source
            FROM event e
            JOIN user_details u ON u.uuid = e.user_details_uuid
            WHERE e.assessment_uuid = $1
              AND e.created_at > $2
              AND e.created_at <= $3
              AND e.deleted IS FALSE
            ORDER BY e.position
        "#,
        assessment.uuid,
        from.to_utc(),
        to.to_utc(),
    )
    .fetch_all(&mut *conn)
    .await?;
    into_entities(rows, assessment)
}

pub async fn find_all_including_deleted(
    conn: &mut PgConnection,
    assessment: &Arc<AssessmentEntity>,
) -> Result<Vec<EventEntity>> {
    let rows = sqlx::query_as!(
        EventRow,
        r#"
            SELECT e.id, e.uuid, e.position, e.created_at, e.data AS "data: Json<Event>", e.autosaved, e.deleted,
                   e.assessment_uuid, u.id AS user_pk, u.uuid AS user_uuid, u.user_id, u.display_name, u.auth_source
            FROM event e
            JOIN user_details u ON u.uuid = e.user_details_uuid
            WHERE e.assessment_uuid = $1
            ORDER BY e.position
        "#,
        assessment.uuid,
    )
    .fetch_all(&mut *conn)
    .await?;
    into_entities(rows, assessment)
}

pub async fn find_by_uuids_including_deleted(conn: &mut PgConnection, uuids: &[Uuid]) -> Result<Vec<EventEntity>> {
    let rows = sqlx::query_as!(
        EventRow,
        r#"
            SELECT e.id, e.uuid, e.position, e.created_at, e.data AS "data: Json<Event>", e.autosaved, e.deleted,
                   e.assessment_uuid, u.id AS user_pk, u.uuid AS user_uuid, u.user_id, u.display_name, u.auth_source
            FROM event e
            JOIN user_details u ON u.uuid = e.user_details_uuid
            WHERE e.uuid = ANY($1)
            ORDER BY e.position
        "#,
        uuids,
    )
    .fetch_all(&mut *conn)
    .await?;

    let mut assessments: HashMap<Uuid, Arc<AssessmentEntity>> = HashMap::new();
    let mut events = Vec::with_capacity(rows.len());
    for row in rows {
        let assessment = match assessments.get(&row.assessment_uuid) {
            Some(assessment) => Arc::clone(assessment),
            None => {
                let assessment = Arc::new(
                    assessment::find_by_uuid(conn, row.assessment_uuid)
                        .await?
                        .ok_or_else(|| Error::Internal(format!("Event {} has no assessment", row.uuid)))?,
                );
                assessments.insert(row.assessment_uuid, Arc::clone(&assessment));
                assessment
            }
        };
        events.push(row.into_entity(assessment)?);
    }
    Ok(events)
}

/// UUIDs and positions of the deleted events created at or after `from`.
pub async fn find_deleted_from(
    conn: &mut PgConnection,
    assessment_uuid: Uuid,
    from: LocalDateTime,
) -> Result<Vec<(Uuid, i32)>> {
    Ok(sqlx::query!(
        r#"
            SELECT uuid, position
            FROM event
            WHERE assessment_uuid = $1
              AND created_at >= $2
              AND deleted IS TRUE
            ORDER BY position
        "#,
        assessment_uuid,
        from.to_utc(),
    )
    .fetch_all(&mut *conn)
    .await?
    .into_iter()
    .map(|row| (row.uuid, row.position))
    .collect())
}

/// Whether there are any events that haven't been deleted after the position.
pub async fn exists_after_position(conn: &mut PgConnection, assessment_uuid: Uuid, position: i32) -> Result<bool> {
    Ok(sqlx::query_scalar!(
        r#"
            SELECT EXISTS (
                SELECT 1 FROM event
                WHERE assessment_uuid = $1 AND position > $2 AND deleted IS FALSE
            ) AS "exists!"
        "#,
        assessment_uuid,
        position,
    )
    .fetch_one(&mut *conn)
    .await?)
}

/// Highest position used by the assessment, including deleted events.
pub async fn find_max_position(conn: &mut PgConnection, assessment_uuid: Uuid) -> Result<Option<i32>> {
    Ok(sqlx::query_scalar!(
        r#"
            SELECT position
            FROM event
            WHERE assessment_uuid = $1
            ORDER BY position DESC
            LIMIT 1
        "#,
        assessment_uuid,
    )
    .fetch_optional(&mut *conn)
    .await?)
}

pub async fn soft_delete_from(conn: &mut PgConnection, assessment_uuid: Uuid, from: LocalDateTime) -> Result<()> {
    sqlx::query!(
        r#"
            UPDATE event SET deleted = TRUE
            WHERE assessment_uuid = $1 AND created_at >= $2 AND deleted IS FALSE
        "#,
        assessment_uuid,
        from.to_utc(),
    )
    .execute(&mut *conn)
    .await?;
    Ok(())
}

pub async fn undelete(conn: &mut PgConnection, uuids: &[Uuid]) -> Result<()> {
    sqlx::query!("UPDATE event SET deleted = FALSE WHERE uuid = ANY($1)", uuids)
        .execute(&mut *conn)
        .await?;
    Ok(())
}

pub async fn insert(conn: &mut PgConnection, event: &EventEntity) -> Result<()> {
    let position = event
        .position
        .ok_or_else(|| Error::Internal(format!("Event {} has no position", event.uuid)))?;

    sqlx::query!(
        r#"
            INSERT INTO event (uuid, created_at, data, assessment_uuid, user_details_uuid, position, autosaved, deleted)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
        "#,
        event.uuid,
        event.created_at.to_utc(),
        serde_json::to_value(&event.data)?,
        event.assessment.uuid,
        event.user.uuid,
        position,
        event.autosaved,
        event.deleted,
    )
    .execute(&mut *conn)
    .await?;
    Ok(())
}

pub async fn delete_by_uuids(conn: &mut PgConnection, uuids: &[Uuid]) -> Result<()> {
    sqlx::query!("DELETE FROM event WHERE uuid = ANY($1)", uuids)
        .execute(&mut *conn)
        .await?;
    Ok(())
}

/// Assessments of the type with events created after `since`, in UUID order, starting after
/// the `after` cursor. A cursor is used rather than offset pagination because a large result
/// set can change while it is being paged through, and offsets would then drop rows.
pub async fn find_assessment_uuids_modified_since(
    conn: &mut PgConnection,
    assessment_type: &str,
    since: LocalDateTime,
    after: Option<Uuid>,
    limit: i64,
) -> Result<Vec<Uuid>> {
    Ok(sqlx::query_scalar!(
        r#"
            SELECT DISTINCT a.uuid
            FROM event e
            JOIN assessment a ON a.uuid = e.assessment_uuid
            WHERE a.type = $1
              AND e.created_at > $2
              AND e.deleted IS FALSE
              AND ($3::uuid IS NULL OR a.uuid > $3)
            ORDER BY a.uuid
            LIMIT $4
        "#,
        assessment_type,
        since.to_utc(),
        after,
        limit,
    )
    .fetch_all(&mut *conn)
    .await?)
}

/// Assessments of the type that have deleted events and had their aggregates rebuilt since.
pub async fn find_assessment_uuids_soft_deleted_since(
    conn: &mut PgConnection,
    assessment_type: &str,
    since: LocalDateTime,
) -> Result<Vec<Uuid>> {
    Ok(sqlx::query_scalar!(
        r#"
            SELECT DISTINCT a.uuid
            FROM event e
            JOIN assessment a ON a.uuid = e.assessment_uuid
            JOIN aggregate ag ON ag.assessment_uuid = a.uuid
            WHERE a.type = $1
              AND ag.updated_at > $2
              AND e.deleted = TRUE
            ORDER BY a.uuid
        "#,
        assessment_type,
        since.to_utc(),
    )
    .fetch_all(&mut *conn)
    .await?)
}
