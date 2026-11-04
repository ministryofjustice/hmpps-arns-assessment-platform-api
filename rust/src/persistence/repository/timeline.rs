use chrono::{DateTime, Utc};
use sqlx::{PgConnection, types::Json};
use uuid::Uuid;

use super::parse_column;
use crate::{
    datetime::LocalDateTime,
    error::{Error, Result},
    model::{DailyVersionDetails, JsonMap},
    persistence::entity::{TimelineEntity, UserDetailsEntity},
};

struct TimelineRow {
    id: i64,
    uuid: Uuid,
    position: i32,
    created_at: DateTime<Utc>,
    event_type: Option<String>,
    data: Json<JsonMap>,
    custom_type: Option<String>,
    custom_data: Option<Json<JsonMap>>,
    deleted: Option<bool>,
    assessment_uuid: Uuid,
    user_pk: i64,
    user_uuid: Uuid,
    user_id: String,
    display_name: String,
    auth_source: String,
}

impl TimelineRow {
    fn into_entity(self) -> Result<TimelineEntity> {
        Ok(TimelineEntity {
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
            assessment_uuid: self.assessment_uuid,
            event_type: self.event_type,
            data: self.data.0,
            custom_type: self.custom_type,
            custom_data: self.custom_data.map(|data| data.0),
            deleted: self.deleted.unwrap_or(false),
        })
    }
}

fn into_entities(rows: Vec<TimelineRow>) -> Result<Vec<TimelineEntity>> {
    rows.into_iter().map(TimelineRow::into_entity).collect()
}

/// Filters for timeline queries. Empty type lists don't filter.
#[derive(Debug, Default)]
pub struct TimelineCriteria {
    pub assessment_uuid: Option<Uuid>,
    pub user_uuid: Option<Uuid>,
    pub from: Option<LocalDateTime>,
    pub to: Option<LocalDateTime>,
    pub include_event_types: Vec<String>,
    pub exclude_event_types: Vec<String>,
    pub include_custom_types: Vec<String>,
    pub exclude_custom_types: Vec<String>,
}

/// A page of the timeline, most recent position first, with the total number of matching entries.
pub async fn find_page(
    conn: &mut PgConnection,
    criteria: &TimelineCriteria,
    page_number: i64,
    page_size: i64,
) -> Result<(Vec<TimelineEntity>, i64)> {
    if criteria.assessment_uuid.is_none() && criteria.user_uuid.is_none() {
        return Err(Error::timeline_bad_criteria());
    }

    let from = criteria.from.map(LocalDateTime::to_utc);
    let to = criteria.to.map(LocalDateTime::to_utc);

    let rows = sqlx::query_as!(
        TimelineRow,
        r#"
            SELECT t.id, t.uuid, t.position, t.created_at, t.event_type, t.data AS "data: Json<JsonMap>",
                   t.custom_type, t.custom_data AS "custom_data: Json<JsonMap>", t.deleted, t.assessment_uuid,
                   u.id AS user_pk, u.uuid AS user_uuid, u.user_id, u.display_name, u.auth_source
            FROM timeline t
            JOIN user_details u ON u.uuid = t.user_details_uuid
            WHERE t.deleted IS FALSE
              AND ($1::uuid IS NULL OR t.assessment_uuid = $1)
              AND ($2::uuid IS NULL OR t.user_details_uuid = $2)
              AND ($3::timestamptz IS NULL OR t.created_at >= $3)
              AND ($4::timestamptz IS NULL OR t.created_at <= $4)
              AND (cardinality($5::text[]) = 0 OR t.event_type = ANY($5))
              AND (cardinality($6::text[]) = 0 OR NOT (t.event_type = ANY($6)))
              AND (cardinality($7::text[]) = 0 OR t.custom_type = ANY($7))
              AND (cardinality($8::text[]) = 0 OR NOT (t.custom_type = ANY($8)))
            ORDER BY t.position DESC
            LIMIT $9 OFFSET $10
        "#,
        criteria.assessment_uuid,
        criteria.user_uuid,
        from,
        to,
        &criteria.include_event_types,
        &criteria.exclude_event_types,
        &criteria.include_custom_types,
        &criteria.exclude_custom_types,
        page_size,
        page_number * page_size,
    )
    .fetch_all(&mut *conn)
    .await?;

    let total = sqlx::query_scalar!(
        r#"
            SELECT count(*) AS "count!"
            FROM timeline t
            WHERE t.deleted IS FALSE
              AND ($1::uuid IS NULL OR t.assessment_uuid = $1)
              AND ($2::uuid IS NULL OR t.user_details_uuid = $2)
              AND ($3::timestamptz IS NULL OR t.created_at >= $3)
              AND ($4::timestamptz IS NULL OR t.created_at <= $4)
              AND (cardinality($5::text[]) = 0 OR t.event_type = ANY($5))
              AND (cardinality($6::text[]) = 0 OR NOT (t.event_type = ANY($6)))
              AND (cardinality($7::text[]) = 0 OR t.custom_type = ANY($7))
              AND (cardinality($8::text[]) = 0 OR NOT (t.custom_type = ANY($8)))
        "#,
        criteria.assessment_uuid,
        criteria.user_uuid,
        from,
        to,
        &criteria.include_event_types,
        &criteria.exclude_event_types,
        &criteria.include_custom_types,
        &criteria.exclude_custom_types,
    )
    .fetch_one(&mut *conn)
    .await?;

    Ok((into_entities(rows)?, total))
}

/// One entry per day with timeline activity, pointing at the last timeline entry of that day.
pub async fn find_daily_versions(conn: &mut PgConnection, assessment_uuid: Uuid) -> Result<Vec<DailyVersionDetails>> {
    Ok(sqlx::query!(
        r#"
            SELECT
                min_created_at AS "created_at!",
                max_created_at AS "updated_at!",
                uuid AS "last_timeline_uuid!"
            FROM (
                SELECT
                    DATE(t.created_at) AS day,
                    t.created_at,
                    t.uuid,
                    MIN(t.created_at) OVER (PARTITION BY DATE(t.created_at)) AS min_created_at,
                    MAX(t.created_at) OVER (PARTITION BY DATE(t.created_at)) AS max_created_at,
                    ROW_NUMBER() OVER (
                        PARTITION BY DATE(t.created_at)
                        ORDER BY t.created_at DESC, t.position DESC
                    ) AS rn
                FROM timeline t
                WHERE t.assessment_uuid = $1
                  AND t.deleted IS FALSE
            ) ranked
            WHERE rn = 1
            ORDER BY day
        "#,
        assessment_uuid,
    )
    .fetch_all(&mut *conn)
    .await?
    .into_iter()
    .map(|row| DailyVersionDetails {
        created_at: LocalDateTime::from_utc(row.created_at),
        updated_at: LocalDateTime::from_utc(row.updated_at),
        last_timeline_item_uuid: row.last_timeline_uuid,
    })
    .collect())
}

pub async fn find_all_including_deleted(conn: &mut PgConnection, assessment_uuid: Uuid) -> Result<Vec<TimelineEntity>> {
    let rows = sqlx::query_as!(
        TimelineRow,
        r#"
            SELECT t.id, t.uuid, t.position, t.created_at, t.event_type, t.data AS "data: Json<JsonMap>",
                   t.custom_type, t.custom_data AS "custom_data: Json<JsonMap>", t.deleted, t.assessment_uuid,
                   u.id AS user_pk, u.uuid AS user_uuid, u.user_id, u.display_name, u.auth_source
            FROM timeline t
            JOIN user_details u ON u.uuid = t.user_details_uuid
            WHERE t.assessment_uuid = $1
            ORDER BY t.position
        "#,
        assessment_uuid,
    )
    .fetch_all(&mut *conn)
    .await?;
    into_entities(rows)
}

pub async fn find_by_uuids_including_deleted(conn: &mut PgConnection, uuids: &[Uuid]) -> Result<Vec<TimelineEntity>> {
    let rows = sqlx::query_as!(
        TimelineRow,
        r#"
            SELECT t.id, t.uuid, t.position, t.created_at, t.event_type, t.data AS "data: Json<JsonMap>",
                   t.custom_type, t.custom_data AS "custom_data: Json<JsonMap>", t.deleted, t.assessment_uuid,
                   u.id AS user_pk, u.uuid AS user_uuid, u.user_id, u.display_name, u.auth_source
            FROM timeline t
            JOIN user_details u ON u.uuid = t.user_details_uuid
            WHERE t.uuid = ANY($1)
            ORDER BY t.position
        "#,
        uuids,
    )
    .fetch_all(&mut *conn)
    .await?;
    into_entities(rows)
}

/// Highest position used by the assessment, including deleted entries.
pub async fn find_max_position(conn: &mut PgConnection, assessment_uuid: Uuid) -> Result<Option<i32>> {
    Ok(sqlx::query_scalar!(
        r#"
            SELECT position
            FROM timeline
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
            UPDATE timeline SET deleted = TRUE
            WHERE assessment_uuid = $1 AND created_at >= $2 AND deleted IS FALSE
        "#,
        assessment_uuid,
        from.to_utc(),
    )
    .execute(&mut *conn)
    .await?;
    Ok(())
}

pub async fn undelete_from(conn: &mut PgConnection, assessment_uuid: Uuid, from: LocalDateTime) -> Result<()> {
    sqlx::query!(
        r#"
            UPDATE timeline SET deleted = FALSE
            WHERE assessment_uuid = $1 AND created_at >= $2 AND deleted IS TRUE
        "#,
        assessment_uuid,
        from.to_utc(),
    )
    .execute(&mut *conn)
    .await?;
    Ok(())
}

pub async fn insert(conn: &mut PgConnection, timeline: &TimelineEntity) -> Result<()> {
    let position = timeline
        .position
        .ok_or_else(|| Error::Internal(format!("Timeline {} has no position", timeline.uuid)))?;

    sqlx::query!(
        r#"
            INSERT INTO timeline (uuid, created_at, event_type, data, custom_type, custom_data, user_details_uuid, assessment_uuid, position, deleted)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
        "#,
        timeline.uuid,
        timeline.created_at.to_utc(),
        timeline.event_type,
        serde_json::Value::Object(timeline.data.clone()),
        timeline.custom_type,
        timeline.custom_data.clone().map(serde_json::Value::Object),
        timeline.user.uuid,
        timeline.assessment_uuid,
        position,
        timeline.deleted,
    )
    .execute(&mut *conn)
    .await?;
    Ok(())
}

pub async fn delete_by_uuids(conn: &mut PgConnection, uuids: &[Uuid]) -> Result<()> {
    sqlx::query!("DELETE FROM timeline WHERE uuid = ANY($1)", uuids)
        .execute(&mut *conn)
        .await?;
    Ok(())
}
