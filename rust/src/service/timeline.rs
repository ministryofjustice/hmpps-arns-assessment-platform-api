use std::collections::{HashMap, hash_map::Entry};

use sqlx::PgConnection;
use uuid::Uuid;

use crate::{
    error::Result,
    persistence::{entity::TimelineEntity, repository::timeline},
};

/// Insert timeline entries after the existing entries for their assessment. Unlike events, any
/// position an entry already had is replaced.
pub async fn save_all(conn: &mut PgConnection, entries: &mut [TimelineEntity]) -> Result<()> {
    let mut next_positions: HashMap<Uuid, i32> = HashMap::new();

    for entry in entries.iter_mut() {
        let next_position = match next_positions.entry(entry.assessment_uuid) {
            Entry::Occupied(next) => next.into_mut(),
            Entry::Vacant(next) => {
                let max_position = timeline::find_max_position(conn, *next.key()).await?.unwrap_or(-1);
                next.insert(max_position + 1)
            }
        };
        entry.position = Some(*next_position);
        *next_position += 1;
    }

    for entry in entries.iter() {
        timeline::insert(conn, entry).await?;
    }
    Ok(())
}
