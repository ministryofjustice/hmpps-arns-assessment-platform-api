use sqlx::PgConnection;
use uuid::Uuid;

use super::parse_column;
use crate::{
    datetime::LocalDateTime,
    error::Result,
    persistence::entity::{AssessmentEntity, AssessmentIdentifierEntity, IdentifierPair, IdentifierType},
};

pub async fn find_by_uuid(conn: &mut PgConnection, uuid: Uuid) -> Result<Option<AssessmentEntity>> {
    let Some(assessment) = sqlx::query!(
        r#"
            SELECT id, uuid, created_at, type AS assessment_type
            FROM assessment
            WHERE uuid = $1
        "#,
        uuid
    )
    .fetch_optional(&mut *conn)
    .await?
    else {
        return Ok(None);
    };

    let identifiers = sqlx::query!(
        r#"
            SELECT id, uuid, created_at, identifier_type, identifier
            FROM assessment_identifier
            WHERE assessment_uuid = $1
            ORDER BY id
        "#,
        uuid
    )
    .fetch_all(&mut *conn)
    .await?
    .into_iter()
    .map(|row| {
        Ok(AssessmentIdentifierEntity {
            id: Some(row.id),
            uuid: row.uuid,
            created_at: LocalDateTime::from_utc(row.created_at),
            external_identifier: IdentifierPair {
                identifier_type: parse_column(&row.identifier_type)?,
                id: row.identifier,
            },
        })
    })
    .collect::<Result<Vec<_>>>()?;

    Ok(Some(AssessmentEntity {
        id: Some(assessment.id),
        uuid: assessment.uuid,
        created_at: LocalDateTime::from_utc(assessment.created_at),
        assessment_type: assessment.assessment_type,
        identifiers,
    }))
}

/// The assessment most recently given this identifier before the point in time.
pub async fn find_uuid_by_external_identifier(
    conn: &mut PgConnection,
    identifier_type: IdentifierType,
    identifier: &str,
    assessment_type: &str,
    point_in_time: LocalDateTime,
) -> Result<Option<Uuid>> {
    Ok(sqlx::query_scalar!(
        r#"
            SELECT ai.assessment_uuid
            FROM assessment_identifier ai
            JOIN assessment a ON a.uuid = ai.assessment_uuid
            WHERE ai.identifier_type = $1
              AND ai.identifier = $2
              AND a.type = $3
              AND ai.created_at < $4
            ORDER BY ai.created_at DESC
            LIMIT 1
        "#,
        identifier_type.as_str(),
        identifier,
        assessment_type,
        point_in_time.to_utc(),
    )
    .fetch_optional(&mut *conn)
    .await?)
}

/// Assessments with any of the identifiers (or all assessments when there are none), created
/// within the optional date range.
pub async fn find_uuids_by_external_identifiers(
    conn: &mut PgConnection,
    identifiers: &[IdentifierPair],
    from: Option<LocalDateTime>,
    to: Option<LocalDateTime>,
) -> Result<Vec<Uuid>> {
    let types: Vec<String> = identifiers
        .iter()
        .map(|pair| pair.identifier_type.as_str().to_owned())
        .collect();
    let ids: Vec<String> = identifiers.iter().map(|pair| pair.id.clone()).collect();

    Ok(sqlx::query_scalar!(
        r#"
            SELECT a.uuid
            FROM assessment a
            WHERE (
                cardinality($1::text[]) = 0
                OR EXISTS (
                    SELECT 1
                    FROM assessment_identifier ai
                    WHERE ai.assessment_uuid = a.uuid
                      AND (ai.identifier_type, ai.identifier) IN (SELECT * FROM UNNEST($1::text[], $2::text[]))
                )
            )
              AND ($3::timestamptz IS NULL OR a.created_at >= $3)
              AND ($4::timestamptz IS NULL OR a.created_at <= $4)
            ORDER BY a.id
        "#,
        &types,
        &ids,
        from.map(LocalDateTime::to_utc),
        to.map(LocalDateTime::to_utc),
    )
    .fetch_all(&mut *conn)
    .await?)
}

pub async fn insert(conn: &mut PgConnection, assessment: &AssessmentEntity) -> Result<()> {
    sqlx::query!(
        r#"
            INSERT INTO assessment (uuid, created_at, type)
            VALUES ($1, $2, $3)
        "#,
        assessment.uuid,
        assessment.created_at.to_utc(),
        assessment.assessment_type,
    )
    .execute(&mut *conn)
    .await?;

    for identifier in &assessment.identifiers {
        sqlx::query!(
            r#"
                INSERT INTO assessment_identifier (uuid, created_at, identifier_type, identifier, assessment_uuid)
                VALUES ($1, $2, $3, $4, $5)
            "#,
            identifier.uuid,
            identifier.created_at.to_utc(),
            identifier.external_identifier.identifier_type.as_str(),
            identifier.external_identifier.id,
            assessment.uuid,
        )
        .execute(&mut *conn)
        .await?;
    }

    Ok(())
}

/// Events, aggregates, timeline entries and identifiers are removed by cascading foreign keys.
pub async fn delete(conn: &mut PgConnection, uuid: Uuid) -> Result<()> {
    sqlx::query!("DELETE FROM assessment WHERE uuid = $1", uuid)
        .execute(&mut *conn)
        .await?;
    Ok(())
}
