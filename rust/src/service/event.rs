use std::collections::{HashMap, hash_map::Entry};

use sqlx::PgConnection;
use uuid::Uuid;

use crate::{
    datetime::LocalDateTime,
    error::{Error, Result},
    persistence::{entity::EventEntity, repository::event},
};

/// Insert new events, giving those without a position the next positions for their assessment.
pub async fn save_all(conn: &mut PgConnection, events: &mut [EventEntity]) -> Result<()> {
    // (highest saved position, events seen so far) per assessment
    let mut positions: HashMap<Uuid, (i32, i32)> = HashMap::new();

    for entity in events.iter_mut() {
        let (max_position, index) = match positions.entry(entity.assessment.uuid) {
            Entry::Occupied(counter) => counter.into_mut(),
            Entry::Vacant(counter) => {
                let max_position = event::find_max_position(conn, *counter.key()).await?.unwrap_or(-1);
                counter.insert((max_position, 0))
            }
        };
        entity.position.get_or_insert(*max_position + 1 + *index);
        *index += 1;
    }

    for entity in events.iter() {
        event::insert(conn, entity).await?;
    }
    Ok(())
}

pub async fn soft_delete(conn: &mut PgConnection, assessment_uuid: Uuid, from: LocalDateTime) -> Result<()> {
    event::soft_delete_from(conn, assessment_uuid, from).await
}

/// Restore events deleted from the point in time onwards. Only allowed when no events have
/// been written since, because those were built without the deleted ones.
pub async fn undelete(conn: &mut PgConnection, assessment_uuid: Uuid, from: LocalDateTime) -> Result<()> {
    let deleted = event::find_deleted_from(conn, assessment_uuid, from).await?;
    let Some(first_deleted_position) = deleted.iter().map(|(_, position)| *position).min() else {
        return Ok(());
    };

    if event::exists_after_position(conn, assessment_uuid, first_deleted_position).await? {
        return Err(Error::undelete_not_at_tail(assessment_uuid, from));
    }

    let uuids: Vec<Uuid> = deleted.into_iter().map(|(uuid, _)| uuid).collect();
    event::undelete(conn, &uuids).await
}
