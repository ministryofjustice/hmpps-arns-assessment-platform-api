use axum::{
    Extension, Router,
    routing::{delete, post},
};
use serde::Deserialize;
use tracing::info;
use utoipa::ToSchema;
use uuid::Uuid;

use super::{
    auth::{CommandUser, DeleteAssessmentUser, QueryUser, RebuildAssessmentUser, Scoped},
    error::ErrorResponse,
    extract::{Json, Path},
};
use crate::{
    AppState,
    clock::Clock,
    command::{self, Command, CommandsResponse},
    error::Error,
    query::{self, QueriesResponse, Query},
    service::assessment,
};

#[derive(Debug, Deserialize, ToSchema)]
pub struct CommandsRequest {
    commands: Vec<Command>,
}

#[derive(Debug, Deserialize, ToSchema)]
pub struct QueriesRequest {
    queries: Vec<Query>,
}

/// Execute commands on an assessment
///
/// Commands are handled in order, in one transaction. A UUID in a later command can be given as
/// `@index` to refer to something created by an earlier one.
#[utoipa::path(
    post,
    tag = "assessment",
    path = "/command",
    request_body = CommandsRequest,
    params(
        ("backdateTo" = Option<String>, Query, description = "Backdate the request to a specific timestamp", example = "2025-01-01T09:00:00")
    ),
    responses(
        (status = 200, description = "Commands successfully executed", body = CommandsResponse),
        (status = 400, description = "Unable to process commands", body = ErrorResponse),
        (status = 404, description = "Assessment not found", body = ErrorResponse),
        (status = 500, description = "Unexpected error", body = ErrorResponse),
    ),
    security(("bearer-jwt" = []))
)]
pub async fn execute_commands(
    user: Scoped<CommandUser>,
    Extension(app): Extension<AppState>,
    clock: Clock,
    Json(request): Json<CommandsRequest>,
) -> Result<Json<CommandsResponse>, Error> {
    let count = request.commands.len();
    let response = command::bus::dispatch(&app, &clock, request.commands).await?;
    info!("Client '{}' executed {} command(s)", user.username, count);
    Ok(Json(response))
}

/// Execute queries on an assessment
#[utoipa::path(
    post,
    tag = "assessment",
    path = "/query",
    request_body = QueriesRequest,
    params(
        ("backdateTo" = Option<String>, Query, description = "Backdate the request to a specific timestamp", example = "2025-01-01T09:00:00")
    ),
    responses(
        (status = 200, description = "Queries successfully executed", body = QueriesResponse),
        (status = 400, description = "Unable to process queries", body = ErrorResponse),
        (status = 404, description = "Assessment not found", body = ErrorResponse),
        (status = 500, description = "Unexpected error", body = ErrorResponse),
    ),
    security(("bearer-jwt" = []))
)]
pub async fn execute_queries(
    _user: Scoped<QueryUser>,
    Extension(app): Extension<AppState>,
    clock: Clock,
    Json(request): Json<QueriesRequest>,
) -> Result<Json<QueriesResponse>, Error> {
    Ok(Json(query::bus::dispatch_all(&app, &clock, request.queries).await?))
}

/// Deletes an assessment and all related entities
#[utoipa::path(
    delete,
    tag = "assessment",
    path = "/assessment/{assessmentUuid}",
    params(("assessmentUuid" = Uuid, Path, description = "The UUID of the assessment")),
    responses(
        (status = 200, description = "Assessment deleted"),
        (status = 404, description = "Assessment not found", body = ErrorResponse),
        (status = 500, description = "Unexpected error", body = ErrorResponse),
    ),
    security(("bearer-jwt" = []))
)]
pub async fn delete_assessment(
    user: Scoped<DeleteAssessmentUser>,
    Extension(app): Extension<AppState>,
    Path(assessment_uuid): Path<Uuid>,
) -> Result<(), Error> {
    assessment::delete(&app, assessment_uuid).await?;
    info!("Client '{}' deleted assessment '{}'", user.username, assessment_uuid);
    Ok(())
}

/// Deletes all of the assessment's aggregates and rebuilds new ones from events
#[utoipa::path(
    post,
    tag = "assessment",
    path = "/assessment/{assessmentUuid}/rebuild",
    params(("assessmentUuid" = Uuid, Path, description = "The UUID of the assessment")),
    responses(
        (status = 200, description = "Assessment aggregates rebuilt"),
        (status = 400, description = "Unable to rebuild aggregates", body = ErrorResponse),
        (status = 404, description = "Assessment not found", body = ErrorResponse),
        (status = 500, description = "Unexpected error", body = ErrorResponse),
    ),
    security(("bearer-jwt" = []))
)]
pub async fn rebuild_aggregates(
    user: Scoped<RebuildAssessmentUser>,
    Extension(app): Extension<AppState>,
    clock: Clock,
    Path(assessment_uuid): Path<Uuid>,
) -> Result<(), Error> {
    assessment::rebuild_aggregates(&app, &clock, &user.username, assessment_uuid).await
}

pub fn router() -> Router {
    Router::new()
        .route("/command", post(execute_commands))
        .route("/query", post(execute_queries))
        .route("/assessment/{assessment_uuid}", delete(delete_assessment))
        .route("/assessment/{assessment_uuid}/rebuild", post(rebuild_aggregates))
}
