use axum::{Extension, Router, routing::post};
use tracing::info;

use super::{
    auth::{PlanUser, Scoped},
    error::ErrorResponse,
    extract::Json,
};
use crate::{
    AppState,
    clock::Clock,
    domain::plan::{self, NewPeriodOfSupervisionRequest},
    error::Error,
};

/// Starts a new period of supervision for a Sentence Plan, removing Active and Future goals
#[utoipa::path(
    post,
    tag = "sentence-plan",
    path = "/plan/start-new-period-of-supervision",
    request_body = NewPeriodOfSupervisionRequest,
    responses(
        (status = 200, description = "Started new period of supervision"),
        (status = 400, description = "The Assessment is not a Sentence Plan", body = ErrorResponse),
        (status = 404, description = "Assessment not found", body = ErrorResponse),
        (status = 500, description = "Unexpected error", body = ErrorResponse),
    ),
    security(("bearer-jwt" = []))
)]
pub async fn start_new_period_of_supervision(
    user: Scoped<PlanUser>,
    Extension(app): Extension<AppState>,
    clock: Clock,
    Json(request): Json<NewPeriodOfSupervisionRequest>,
) -> Result<(), Error> {
    let assessment_uuid = request.assessment_uuid;
    plan::start_new_period_of_supervision(&app, &clock, request).await?;
    info!(
        "Client '{}' started a new period of supervision for plan '{}'",
        user.username, assessment_uuid
    );
    Ok(())
}

pub fn router() -> Router {
    Router::new().route(
        "/plan/start-new-period-of-supervision",
        post(start_new_period_of_supervision),
    )
}
