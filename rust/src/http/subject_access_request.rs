//! The HMPPS subject access request (SAR) endpoints.

use axum::{
    Extension, Router,
    http::{StatusCode, header},
    response::{IntoResponse, Response},
    routing::get,
};
use chrono::NaiveDate;
use serde::{Deserialize, Serialize};
use utoipa::{IntoParams, ToSchema};

use super::{
    auth::{Scoped, SubjectAccessRequestUser},
    error::ErrorResponse,
    extract::{Json, Query},
};
use crate::{
    AppState,
    clock::Clock,
    error::Error,
    persistence::entity::{IdentifierPair, IdentifierType},
    query::{SubjectAccessRequestQuery, SubjectAccessRequestQueryResult, bus},
};

const TEMPLATE: &str = include_str!("../../templates/sar_template.mustache");

/// Returned when neither identifier is given, as the HMPPS SAR library does.
const NO_IDENTIFIER: StatusCode = match StatusCode::from_u16(209) {
    Ok(status) => status,
    Err(_) => panic!("209 is a valid status code"),
};

#[derive(Debug, Deserialize, IntoParams)]
#[serde(rename_all = "camelCase")]
#[into_params(parameter_in = Query)]
pub struct SubjectAccessRequestParams {
    /// Prison reference number
    prn: Option<String>,
    /// Case reference number
    crn: Option<String>,
    from_date: Option<NaiveDate>,
    to_date: Option<NaiveDate>,
}

#[derive(Debug, Serialize, ToSchema)]
pub struct SubjectAccessRequestContent {
    content: SubjectAccessRequestQueryResult,
}

/// Everything held about a person, by their prison or case reference number
#[utoipa::path(
    get,
    tag = "subject-access-request",
    path = "/subject-access-request",
    params(SubjectAccessRequestParams),
    responses(
        (status = 200, description = "The person's assessments", body = SubjectAccessRequestContent),
        (status = 204, description = "There are no assessments for the person"),
        (status = 209, description = "Neither a PRN nor a CRN was given", body = ErrorResponse),
    ),
    security(("bearer-jwt" = []))
)]
pub async fn subject_access_request(
    _user: Scoped<SubjectAccessRequestUser>,
    Extension(app): Extension<AppState>,
    Query(params): Query<SubjectAccessRequestParams>,
) -> Result<Response, Error> {
    if params.prn.is_none() && params.crn.is_none() {
        let message = "One of prn or crn must be supplied.";
        return Ok(ErrorResponse::new(NO_IDENTIFIER, message, message).into_response());
    }

    let clock = Clock::system();
    let assessment_identifiers = [
        params.crn.map(|id| IdentifierPair {
            identifier_type: IdentifierType::CRN,
            id,
        }),
        params.prn.map(|id| IdentifierPair {
            identifier_type: IdentifierType::PRN,
            id,
        }),
    ]
    .into_iter()
    .flatten()
    .collect();
    let query = SubjectAccessRequestQuery {
        timestamp: Some(clock.now()),
        assessment_identifiers,
        from: params.from_date,
        to: params.to_date,
    };

    match bus::dispatch_subject_access_request(&app, &clock, &query).await {
        Ok(content) => Ok(Json(SubjectAccessRequestContent { content }).into_response()),
        Err(error) if error.status() == StatusCode::NO_CONTENT => Ok(StatusCode::NO_CONTENT.into_response()),
        Err(error) => Err(error),
    }
}

/// The template the SAR service renders the content with
#[utoipa::path(
    get,
    tag = "subject-access-request",
    path = "/subject-access-request/template",
    responses((status = 200, description = "The Mustache template", content_type = "text/plain")),
    security(("bearer-jwt" = []))
)]
pub async fn template(_user: Scoped<SubjectAccessRequestUser>) -> impl IntoResponse {
    ([(header::CONTENT_TYPE, "text/plain; charset=utf-8")], TEMPLATE)
}

pub fn router() -> Router {
    Router::new()
        .route("/subject-access-request", get(subject_access_request))
        .route("/subject-access-request/template", get(template))
}
