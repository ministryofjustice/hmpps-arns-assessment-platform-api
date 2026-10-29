//! HMPPS Auth bearer tokens: signature checked against HMPPS Auth's JWKS, roles read from the
//! `authorities` claim.

use std::{
    ops::Deref,
    sync::Arc,
    time::{Duration, Instant},
};

use axum::{
    extract::FromRequestParts,
    http::{StatusCode, request::Parts},
    response::{IntoResponse, Response},
};
use axum_extra::{
    TypedHeader,
    headers::{Authorization, authorization::Bearer},
};
use jsonwebtoken::{
    Algorithm, DecodingKey, Validation, decode, decode_header,
    jwk::{AlgorithmParameters, Jwk, JwkSet},
};
use serde::Deserialize;
use tokio::sync::RwLock;
use tracing::error;

use super::error::ErrorResponse;
use crate::AppState;

const JWKS_TTL: Duration = Duration::from_secs(300);
/// Matches Spring Security's default allowance for clock skew.
const CLOCK_SKEW_SECONDS: u64 = 60;
const RSA_ALGORITHMS: [Algorithm; 6] = [
    Algorithm::RS256,
    Algorithm::RS384,
    Algorithm::RS512,
    Algorithm::PS256,
    Algorithm::PS384,
    Algorithm::PS512,
];

#[derive(thiserror::Error, Debug)]
pub enum AuthError {
    #[error("Missing token")]
    MissingToken,
    #[error("Invalid token")]
    InvalidToken,
    #[error("Token expired")]
    ExpiredToken,
    #[error("Unable to verify JWT token")]
    Unavailable,
    #[error("Access Denied")]
    MissingRole(&'static [&'static str]),
}

impl IntoResponse for AuthError {
    fn into_response(self) -> Response {
        match &self {
            AuthError::MissingToken | AuthError::InvalidToken | AuthError::ExpiredToken => ErrorResponse::new(
                StatusCode::UNAUTHORIZED,
                format!("Unauthorised: {self}"),
                self.to_string(),
            ),
            AuthError::Unavailable => {
                ErrorResponse::new(StatusCode::SERVICE_UNAVAILABLE, self.to_string(), self.to_string())
            }
            AuthError::MissingRole(roles) => ErrorResponse::new(
                StatusCode::FORBIDDEN,
                format!("Forbidden: {self}"),
                format!("Requires one of the roles: {}", roles.join(", ")),
            ),
        }
        .into_response()
    }
}

impl From<jsonwebtoken::errors::Error> for AuthError {
    fn from(err: jsonwebtoken::errors::Error) -> Self {
        match err.kind() {
            jsonwebtoken::errors::ErrorKind::ExpiredSignature => AuthError::ExpiredToken,
            _ => {
                tracing::debug!(error = %err, "JWT decode failed");
                AuthError::InvalidToken
            }
        }
    }
}

/// HMPPS Auth's signing keys, cached for five minutes and refreshed early when a token is
/// signed by a key that isn't in the cache (as happens when keys are rotated).
pub struct JwksCache {
    url: String,
    client: reqwest::Client,
    cached: RwLock<Option<(Arc<JwkSet>, Instant)>>,
}

impl JwksCache {
    pub fn new(hmpps_auth_url: &str, client: reqwest::Client) -> Self {
        Self {
            url: format!("{}/.well-known/jwks.json", hmpps_auth_url.trim_end_matches('/')),
            client,
            cached: RwLock::new(None),
        }
    }

    async fn get(&self, force_refresh: bool) -> Result<Arc<JwkSet>, AuthError> {
        if !force_refresh
            && let Some((jwks, fetched)) = self.cached.read().await.as_ref()
            && fetched.elapsed() < JWKS_TTL
        {
            return Ok(Arc::clone(jwks));
        }

        let fresh = self
            .client
            .get(&self.url)
            .send()
            .await
            .and_then(reqwest::Response::error_for_status)
            .map_err(|e| {
                error!(error = %e, url = %self.url, "Failed to fetch JWKS");
                AuthError::Unavailable
            })?
            .json::<JwkSet>()
            .await
            .map_err(|e| {
                error!(error = %e, url = %self.url, "Failed to parse JWKS");
                AuthError::Unavailable
            })?;

        let fresh = Arc::new(fresh);
        *self.cached.write().await = Some((Arc::clone(&fresh), Instant::now()));
        Ok(fresh)
    }

    /// The key with the given id, or the only key when the token doesn't name one.
    async fn find(&self, kid: Option<&str>) -> Result<Jwk, AuthError> {
        let select = |jwks: &JwkSet| match kid {
            Some(kid) => jwks.find(kid).cloned(),
            None if jwks.keys.len() == 1 => jwks.keys.first().cloned(),
            None => None,
        };

        if let Some(jwk) = select(&*self.get(false).await?) {
            return Ok(jwk);
        }
        select(&*self.get(true).await?).ok_or(AuthError::InvalidToken)
    }
}

#[derive(Clone, Debug, Deserialize)]
pub struct Claims {
    #[serde(default)]
    pub sub: Option<String>,
    #[serde(default)]
    pub user_name: Option<String>,
    #[serde(default)]
    pub client_id: Option<String>,
    /// HMPPS Auth roles, such as `ROLE_AAP__FRONTEND_RW`.
    #[serde(default)]
    pub authorities: Vec<String>,
}

impl Claims {
    /// Who made the request: the user for user tokens, otherwise the client.
    pub fn principal(&self) -> String {
        self.user_name
            .clone()
            .or_else(|| self.client_id.clone())
            .or_else(|| self.sub.clone())
            .unwrap_or_default()
    }

    fn has_any_role(&self, roles: &[&str]) -> bool {
        self.authorities
            .iter()
            .any(|authority| roles.contains(&authority.as_str()))
    }
}

impl<S> FromRequestParts<S> for Claims
where
    S: Send + Sync,
{
    type Rejection = AuthError;

    async fn from_request_parts(parts: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        let TypedHeader(Authorization(bearer)) = TypedHeader::<Authorization<Bearer>>::from_request_parts(parts, state)
            .await
            .map_err(|_| AuthError::MissingToken)?;
        let app = parts.extensions.get::<AppState>().ok_or(AuthError::Unavailable)?;

        let header = decode_header(bearer.token())?;
        if !RSA_ALGORITHMS.contains(&header.alg) {
            return Err(AuthError::InvalidToken);
        }

        let jwk = app.jwks.find(header.kid.as_deref()).await?;
        let decoding_key = match &jwk.algorithm {
            AlgorithmParameters::RSA(rsa) => DecodingKey::from_rsa_components(&rsa.n, &rsa.e).map_err(|e| {
                error!(error = %e, "Failed to build RSA decoding key");
                AuthError::InvalidToken
            })?,
            _ => return Err(AuthError::InvalidToken),
        };

        let mut validation = Validation::new(header.alg);
        validation.leeway = CLOCK_SKEW_SECONDS;
        validation.validate_aud = false;
        validation.set_required_spec_claims(&["exp"]);

        Ok(decode::<Claims>(bearer.token(), &decoding_key, &validation)?.claims)
    }
}

pub trait RequiredRoles {
    /// The caller must have at least one of these roles.
    fn required_roles() -> &'static [&'static str];
    fn from_claims(claims: Claims) -> Self;
}

pub struct Scoped<T>(pub T);

impl<S, T> FromRequestParts<S> for Scoped<T>
where
    S: Send + Sync,
    T: RequiredRoles + Send,
{
    type Rejection = AuthError;

    async fn from_request_parts(parts: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        let claims = Claims::from_request_parts(parts, state).await?;
        let required = T::required_roles();

        if claims.has_any_role(required) {
            Ok(Scoped(T::from_claims(claims)))
        } else {
            Err(AuthError::MissingRole(required))
        }
    }
}

impl<T> Deref for Scoped<T> {
    type Target = T;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

macro_rules! define_user {
    ($name:ident, [$($role:literal),+ $(,)?]) => {
        #[derive(Debug)]
        pub struct $name {
            pub username: String,
        }

        impl RequiredRoles for $name {
            fn required_roles() -> &'static [&'static str] {
                &[$($role),+]
            }

            fn from_claims(claims: Claims) -> Self {
                Self {
                    username: claims.principal(),
                }
            }
        }
    };
}

define_user!(
    CommandUser,
    [
        "ROLE_AAP__FRONTEND_RW",
        "ROLE_AAP__COORDINATOR_RW",
        "ROLE_SENTENCE_PLAN_WRITE"
    ]
);
define_user!(
    QueryUser,
    [
        "ROLE_AAP__FRONTEND_RW",
        "ROLE_AAP__COORDINATOR_RW",
        "ROLE_MPOP__FRONTEND_RO",
        "ROLE_AAP__VIEW_R"
    ]
);
define_user!(
    DeleteAssessmentUser,
    ["ROLE_AAP__COORDINATOR_RW", "ROLE_SENTENCE_PLAN_WRITE"]
);
define_user!(
    RebuildAssessmentUser,
    [
        "ROLE_AAP__COORDINATOR_RW",
        "ROLE_SENTENCE_PLAN_WRITE",
        "ROLE_AAP_DATA_DELETION"
    ]
);
define_user!(DataDeletionUser, ["ROLE_AAP_DATA_DELETION"]);
define_user!(PlanUser, ["ROLE_AAP__FRONTEND_RW", "ROLE_AAP__COORDINATOR_RW"]);
define_user!(SubjectAccessRequestUser, ["ROLE_SAR_DATA_ACCESS"]);

#[cfg(test)]
mod tests {
    use super::*;

    fn claims(json: serde_json::Value) -> Claims {
        serde_json::from_value(json).unwrap()
    }

    #[test]
    fn principal_prefers_the_user_then_the_client() {
        assert_eq!(
            claims(serde_json::json!({"user_name": "AUSER", "client_id": "client", "sub": "AUSER"})).principal(),
            "AUSER"
        );
        assert_eq!(
            claims(serde_json::json!({"client_id": "client", "sub": "client"})).principal(),
            "client"
        );
        assert_eq!(claims(serde_json::json!({})).principal(), "");
    }

    #[test]
    fn requires_any_one_of_the_roles() {
        let claims = claims(serde_json::json!({"authorities": ["ROLE_AAP__VIEW_R"]}));

        assert!(claims.has_any_role(QueryUser::required_roles()));
        assert!(!claims.has_any_role(CommandUser::required_roles()));
    }
}
