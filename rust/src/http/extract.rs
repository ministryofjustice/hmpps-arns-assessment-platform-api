//! Extractors that reject with the standard JSON error body rather than axum's plain text.

use axum::{
    extract::{FromRequest, FromRequestParts, Request},
    http::request::Parts,
    response::{IntoResponse, Response},
};
use serde::{Serialize, de::DeserializeOwned};

use super::error::invalid_payload;
use crate::{clock::Clock, datetime::LocalDateTime};

pub struct Json<T>(pub T);

impl<S, T> FromRequest<S> for Json<T>
where
    T: DeserializeOwned,
    S: Send + Sync,
{
    type Rejection = Response;

    async fn from_request(req: Request, state: &S) -> Result<Self, Self::Rejection> {
        axum::Json::<T>::from_request(req, state)
            .await
            .map(|axum::Json(value)| Self(value))
            .map_err(|rejection| invalid_payload(rejection.status(), rejection.body_text()))
    }
}

impl<T: Serialize> IntoResponse for Json<T> {
    fn into_response(self) -> Response {
        axum::Json(self.0).into_response()
    }
}

pub struct Path<T>(pub T);

impl<S, T> FromRequestParts<S> for Path<T>
where
    T: DeserializeOwned + Send,
    S: Send + Sync,
{
    type Rejection = Response;

    async fn from_request_parts(parts: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        axum::extract::Path::<T>::from_request_parts(parts, state)
            .await
            .map(|axum::extract::Path(value)| Self(value))
            .map_err(|rejection| invalid_payload(rejection.status(), rejection.body_text()))
    }
}

pub struct Query<T>(pub T);

impl<S, T> FromRequestParts<S> for Query<T>
where
    T: DeserializeOwned,
    S: Send + Sync,
{
    type Rejection = Response;

    async fn from_request_parts(parts: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        axum::extract::Query::<T>::from_request_parts(parts, state)
            .await
            .map(|axum::extract::Query(value)| Self(value))
            .map_err(|rejection| invalid_payload(rejection.status(), rejection.body_text()))
    }
}

#[derive(serde::Deserialize)]
struct BackdateParams {
    #[serde(rename = "backdateTo")]
    backdate_to: Option<String>,
}

/// The request's clock, moved back to the `backdateTo` query parameter when one is given.
impl<S: Send + Sync> FromRequestParts<S> for Clock {
    type Rejection = Response;

    async fn from_request_parts(parts: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        let Query(params) = Query::<BackdateParams>::from_request_parts(parts, state).await?;
        let Some(raw) = params.backdate_to else {
            return Ok(Clock::system());
        };
        let backdate_to: LocalDateTime = raw.parse().map_err(|error: crate::datetime::ParseLocalDateTimeError| {
            crate::error::Error::invalid_backdate(error.to_string()).into_response()
        })?;
        Clock::backdated_to(backdate_to).map_err(IntoResponse::into_response)
    }
}
