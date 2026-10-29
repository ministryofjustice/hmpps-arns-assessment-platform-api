use axum::{
    Json,
    http::StatusCode,
    response::{IntoResponse, Response},
};
use serde::Serialize;
use utoipa::ToSchema;

use crate::error::Error;

/// The HMPPS standard error body.
#[derive(Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct ErrorResponse {
    pub status: u16,
    pub error_code: Option<String>,
    pub user_message: String,
    pub developer_message: String,
    pub more_info: Option<String>,
}

impl ErrorResponse {
    pub fn new(status: StatusCode, user_message: impl Into<String>, developer_message: impl Into<String>) -> Self {
        Self {
            status: status.as_u16(),
            error_code: None,
            user_message: user_message.into(),
            developer_message: developer_message.into(),
            more_info: None,
        }
    }
}

impl IntoResponse for ErrorResponse {
    fn into_response(self) -> Response {
        let status = StatusCode::from_u16(self.status).unwrap_or(StatusCode::INTERNAL_SERVER_ERROR);
        (status, Json(self)).into_response()
    }
}

impl IntoResponse for Error {
    fn into_response(self) -> Response {
        let status = self.status();
        let body = match &self {
            Error::Platform { .. } | Error::EventHandling(_) => {
                tracing::debug!(status = status.as_u16(), developer_message = %self.developer_message(), "{self}");
                ErrorResponse::new(status, self.to_string(), self.developer_message())
            }
            Error::OptimisticLock(_) | Error::Database(_) | Error::Serialization(_) | Error::Internal(_) => {
                tracing::error!(error = ?self, "Unexpected error");
                ErrorResponse::new(status, format!("Unexpected error: {self}"), self.to_string())
            }
        };
        body.into_response()
    }
}

/// Invalid JSON, a body that doesn't match the expected shape, or an invalid path or query.
pub fn invalid_payload(status: StatusCode, detail: String) -> Response {
    let status = match status {
        StatusCode::UNSUPPORTED_MEDIA_TYPE => status,
        _ => StatusCode::BAD_REQUEST,
    };
    tracing::debug!(status = status.as_u16(), detail, "Invalid payload");
    ErrorResponse::new(status, format!("Invalid payload: {detail}"), detail).into_response()
}

pub async fn not_found(uri: axum::http::Uri) -> Response {
    let message = format!("No static resource {}.", uri.path().trim_start_matches('/'));
    ErrorResponse::new(
        StatusCode::NOT_FOUND,
        format!("No resource found failure: {message}"),
        message,
    )
    .into_response()
}
