use sqlx::PgConnection;
use uuid::Uuid;

use super::parse_column;
use crate::{
    error::Result,
    persistence::entity::{AuthSource, UserDetailsEntity},
};

struct UserDetailsRow {
    id: i64,
    uuid: Uuid,
    user_id: String,
    display_name: String,
    auth_source: String,
}

impl UserDetailsRow {
    fn into_entity(self) -> Result<UserDetailsEntity> {
        Ok(UserDetailsEntity {
            id: Some(self.id),
            uuid: self.uuid,
            user_id: self.user_id,
            display_name: self.display_name,
            auth_source: parse_column(&self.auth_source)?,
        })
    }
}

pub async fn find_by_user_id_and_auth_source(
    conn: &mut PgConnection,
    user_id: &str,
    auth_source: AuthSource,
) -> Result<Option<UserDetailsEntity>> {
    sqlx::query_as!(
        UserDetailsRow,
        r#"
            SELECT id, uuid, user_id, display_name, auth_source
            FROM user_details
            WHERE user_id = $1 AND auth_source = $2
        "#,
        user_id,
        auth_source.as_str(),
    )
    .fetch_optional(&mut *conn)
    .await?
    .map(UserDetailsRow::into_entity)
    .transpose()
}

pub async fn find_by_uuid(conn: &mut PgConnection, uuid: Uuid) -> Result<Option<UserDetailsEntity>> {
    sqlx::query_as!(
        UserDetailsRow,
        r#"
            SELECT id, uuid, user_id, display_name, auth_source
            FROM user_details
            WHERE uuid = $1
        "#,
        uuid,
    )
    .fetch_optional(&mut *conn)
    .await?
    .map(UserDetailsRow::into_entity)
    .transpose()
}

pub async fn find_all_by_uuids(conn: &mut PgConnection, uuids: &[Uuid]) -> Result<Vec<UserDetailsEntity>> {
    sqlx::query_as!(
        UserDetailsRow,
        r#"
            SELECT id, uuid, user_id, display_name, auth_source
            FROM user_details
            WHERE uuid = ANY($1)
            ORDER BY id
        "#,
        uuids,
    )
    .fetch_all(&mut *conn)
    .await?
    .into_iter()
    .map(UserDetailsRow::into_entity)
    .collect()
}

/// Returns the generated id.
pub async fn insert(conn: &mut PgConnection, user: &UserDetailsEntity) -> Result<i64> {
    Ok(sqlx::query_scalar!(
        r#"
            INSERT INTO user_details (uuid, user_id, display_name, auth_source)
            VALUES ($1, $2, $3, $4)
            RETURNING id
        "#,
        user.uuid,
        user.user_id,
        user.display_name,
        user.auth_source.as_str(),
    )
    .fetch_one(&mut *conn)
    .await?)
}
