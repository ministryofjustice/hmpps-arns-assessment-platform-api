use axum::{Extension, Router, routing::get};
use tracing::info;
use uuid::Uuid;

use super::{
    auth::{DataDeletionUser, Scoped},
    error::ErrorResponse,
    extract::{Json, Path},
};
use crate::{
    AppState,
    clock::Clock,
    error::Error,
    service::data_deletion::{self, DataDeletionDataResponse, DataDeletionRequest, DataDeletionResponse},
};

/// Fetch all events and timeline items for an assessment, including deleted ones
#[utoipa::path(
    get,
    tag = "data-deletion",
    path = "/data-deletion/{assessmentUuid}",
    params(("assessmentUuid" = Uuid, Path, description = "The UUID of the assessment")),
    responses(
        (status = 200, description = "Events/timeline retrieved successfully", body = DataDeletionDataResponse),
        (status = 500, description = "Unexpected error", body = ErrorResponse),
    ),
    security(("bearer-jwt" = []))
)]
pub async fn get_data(
    _user: Scoped<DataDeletionUser>,
    Extension(app): Extension<AppState>,
    Path(assessment_uuid): Path<Uuid>,
) -> Result<Json<DataDeletionDataResponse>, Error> {
    Ok(Json(data_deletion::get_data(&app, assessment_uuid).await?))
}

/// Updates events, timeline and aggregates
///
/// An update is always attempted; check `success` in the response. Requests are dry runs unless
/// `dryRun` is false.
#[utoipa::path(
    post,
    tag = "data-deletion",
    path = "/data-deletion/{assessmentUuid}",
    params(("assessmentUuid" = Uuid, Path, description = "The UUID of the assessment")),
    request_body = DataDeletionRequest,
    responses(
        (status = 200, description = "An update was attempted - check the `success` property in the response.", body = DataDeletionResponse),
        (status = 404, description = "Assessment not found", body = ErrorResponse),
        (status = 500, description = "Unexpected error", body = ErrorResponse),
    ),
    security(("bearer-jwt" = []))
)]
pub async fn update_data(
    user: Scoped<DataDeletionUser>,
    Extension(app): Extension<AppState>,
    clock: Clock,
    Path(assessment_uuid): Path<Uuid>,
    Json(request): Json<DataDeletionRequest>,
) -> Result<Json<DataDeletionResponse>, Error> {
    let response = data_deletion::update_data(&app, &clock, &user.username, assessment_uuid, request).await?;
    info!(
        "Client '{}' updated data for assessment '{}' (dry run: {}, success: {})",
        user.username, assessment_uuid, response.dry_run, response.success
    );
    Ok(Json(response))
}

pub fn router() -> Router {
    Router::new().route("/data-deletion/{assessment_uuid}", get(get_data).post(update_data))
}
