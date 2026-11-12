use std::time::Duration;

use sqlx::PgConnection;
use uuid::Uuid;

use super::{Command, CommandResponse, CommandsResponse, handler};
use crate::{
    AppState,
    clock::Clock,
    error::{Error, Result},
    hook,
    persistence::PersistenceContext,
};

const MAX_ATTEMPTS: u32 = 3;
const RETRY_BACKOFF: Duration = Duration::from_millis(50);

/// Handle a batch of commands in one transaction, retrying when a concurrent request wrote
/// the same assessment first. Audit events are sent once the transaction has committed.
pub async fn dispatch(app: &AppState, clock: &Clock, mut commands: Vec<Command>) -> Result<CommandsResponse> {
    if commands.is_empty() {
        return Err(Error::invalid_command("No commands received"));
    }

    let mut attempt = 1;
    loop {
        match dispatch_in_transaction(app, clock, &mut commands).await {
            Ok((responses, evictions)) => {
                for assessment_uuid in evictions {
                    app.cache.evict_latest(assessment_uuid);
                }
                app.audit.audit_commands(&commands);
                return Ok(CommandsResponse { commands: responses });
            }
            Err(error) if error.is_concurrency_conflict() && attempt < MAX_ATTEMPTS => {
                tracing::debug!(attempt, %error, "Retrying commands after a concurrent update");
                attempt += 1;
                tokio::time::sleep(RETRY_BACKOFF).await;
            }
            Err(error) => return Err(error),
        }
    }
}

async fn dispatch_in_transaction(
    app: &AppState,
    clock: &Clock,
    commands: &mut [Command],
) -> Result<(Vec<CommandResponse>, Vec<Uuid>)> {
    let mut tx = app.pool.begin().await?;
    let mut context = PersistenceContext::default();

    let responses = dispatch_all(&mut context, &mut tx, clock, commands).await?;
    context.persist(&mut tx).await?;

    tx.commit().await?;
    Ok((responses, context.evictions))
}

/// Handle each command in turn, resolving its references against the earlier results and
/// running its hooks.
async fn dispatch_all(
    context: &mut PersistenceContext,
    conn: &mut PgConnection,
    clock: &Clock,
    commands: &mut [Command],
) -> Result<Vec<CommandResponse>> {
    let mut responses: Vec<CommandResponse> = Vec::with_capacity(commands.len());

    for command in commands.iter_mut() {
        command.resolve_references(&responses)?;
        let result = handler::handle(command, context, conn, clock).await?;
        hook::dispatch(command, context, conn, clock).await?;
        responses.push(CommandResponse {
            request: command.clone(),
            result,
        });
    }

    Ok(responses)
}
