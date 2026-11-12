//! Hooks are extra work a command asks for once it has been handled.

use serde::{Deserialize, Serialize};
use sqlx::PgConnection;
use utoipa::ToSchema;

use crate::{
    clock::Clock,
    command::Command,
    domain::san::hook::{self as san, UpdateOasysDataMapping},
    error::Result,
    persistence::PersistenceContext,
};

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(tag = "type")]
pub enum Hook {
    UpdateOasysDataMapping(UpdateOasysDataMapping),
}

pub async fn dispatch(
    command: &Command,
    context: &mut PersistenceContext,
    conn: &mut PgConnection,
    clock: &Clock,
) -> Result<()> {
    for hook in command.hooks() {
        match hook {
            Hook::UpdateOasysDataMapping(hook) => {
                san::update_oasys_data_mapping(hook, command, context, conn, clock).await?;
            }
        }
    }
    Ok(())
}
