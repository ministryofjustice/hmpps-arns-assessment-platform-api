//! A test instance of the API: a fresh database per test, and a stand-in for HMPPS Auth that
//! serves the public key test tokens are signed with.

use std::{
    sync::LazyLock,
    time::{Duration, SystemTime, UNIX_EPOCH},
};

use axum::{
    Router,
    body::Body,
    http::{Method, Request, StatusCode, header},
    routing::get,
};
use base64::{Engine, engine::general_purpose::URL_SAFE_NO_PAD};
use hmpps_arns_assessment_platform_api::{
    AppState, app,
    config::{AuditConfig, Config, DatabaseConfig, RedisConfig},
    service::{audit::AuditService, cache::CacheService},
};
use http_body_util::BodyExt;
use jsonwebtoken::{Algorithm, EncodingKey, Header, encode};
use rsa::{
    RsaPrivateKey,
    pkcs1::{EncodeRsaPrivateKey, LineEnding},
    traits::PublicKeyParts,
};
use serde_json::{Value, json};
use sqlx::PgPool;
use tower::ServiceExt;

pub const FRONTEND: &str = "ROLE_AAP__FRONTEND_RW";
pub const COORDINATOR: &str = "ROLE_AAP__COORDINATOR_RW";
pub const VIEWER: &str = "ROLE_AAP__VIEW_R";
pub const DATA_DELETION: &str = "ROLE_AAP_DATA_DELETION";
pub const SAR: &str = "ROLE_SAR_DATA_ACCESS";

struct Keys {
    encoding: EncodingKey,
    jwks: Value,
}

static KEYS: LazyLock<Keys> = LazyLock::new(|| {
    let key = RsaPrivateKey::new(&mut rand::rngs::OsRng, 2048).expect("RSA key");
    let pem = key.to_pkcs1_pem(LineEnding::LF).expect("PEM");
    let public = key.to_public_key();
    Keys {
        encoding: EncodingKey::from_rsa_pem(pem.as_bytes()).expect("encoding key"),
        jwks: json!({
            "keys": [{
                "kty": "RSA",
                "kid": "test-key",
                "use": "sig",
                "alg": "RS256",
                "n": URL_SAFE_NO_PAD.encode(public.n().to_bytes_be()),
                "e": URL_SAFE_NO_PAD.encode(public.e().to_bytes_be()),
            }]
        }),
    }
});

pub struct TestApp {
    router: Router,
    pub pool: PgPool,
}

impl TestApp {
    pub async fn new(pool: PgPool) -> Self {
        let auth_url = start_fake_hmpps_auth().await;
        let config = Config {
            server_port: 0,
            service_name: "hmpps-arns-assessment-platform-api".to_owned(),
            time_zone: chrono_tz::Europe::London,
            database: DatabaseConfig {
                url: None,
                endpoint: String::new(),
                name: String::new(),
                username: String::new(),
                password: String::new(),
                schema: "public".to_owned(),
                pool_size: 5,
            },
            hmpps_auth_url: auth_url,
            redis: RedisConfig {
                host: String::new(),
                port: 0,
                password: None,
                ssl: false,
                timeout: Duration::from_secs(1),
                connect_timeout: Duration::from_secs(1),
            },
            audit: AuditConfig {
                queue_name: None,
                localstack_url: None,
            },
            query_max_lookback_days: 1,
        };
        let state = AppState::with_services(
            config,
            pool.clone(),
            AuditService::disabled("test"),
            CacheService::disabled(),
        );
        Self {
            router: app(state),
            pool,
        }
    }

    pub async fn request(&self, method: Method, uri: &str, roles: &[&str], body: Option<Value>) -> (StatusCode, Value) {
        let mut request = Request::builder().method(method).uri(uri);
        if !roles.is_empty() {
            request = request.header(header::AUTHORIZATION, format!("Bearer {}", token(roles)));
        }
        let request = match body {
            Some(body) => request
                .header(header::CONTENT_TYPE, "application/json")
                .body(Body::from(body.to_string())),
            None => request.body(Body::empty()),
        }
        .unwrap();

        let response = self.router.clone().oneshot(request).await.unwrap();
        let status = response.status();
        let bytes = response.into_body().collect().await.unwrap().to_bytes();
        let body = serde_json::from_slice(&bytes)
            .unwrap_or_else(|_| Value::String(String::from_utf8_lossy(&bytes).into_owned()));
        (status, body)
    }

    pub async fn commands(&self, commands: Value) -> Value {
        self.commands_at("/command", commands).await
    }

    pub async fn commands_at(&self, uri: &str, commands: Value) -> Value {
        let (status, body) = self
            .request(Method::POST, uri, &[FRONTEND], Some(json!({ "commands": commands })))
            .await;
        assert_eq!(status, StatusCode::OK, "commands failed: {body:#}");
        body
    }

    pub async fn query(&self, query: Value) -> Value {
        let (status, body) = self
            .request(Method::POST, "/query", &[VIEWER], Some(json!({ "queries": [query] })))
            .await;
        assert_eq!(status, StatusCode::OK, "query failed: {body:#}");
        body["queries"][0]["result"].clone()
    }

    /// Creates an assessment and returns its UUID.
    pub async fn create_assessment(&self, assessment_type: &str, crn: &str) -> String {
        let response = self
            .commands(json!([{
                "type": "CreateAssessmentCommand",
                "user": user(),
                "formVersion": "1",
                "assessmentType": assessment_type,
                "identifiers": { "CRN": crn },
                "properties": { "status": { "type": "Single", "value": "NEW" } },
            }]))
            .await;
        response["commands"][0]["result"]["assessmentUuid"]
            .as_str()
            .unwrap()
            .to_owned()
    }

    pub async fn answer(&self, assessment: &str, code: &str, value: &str) {
        self.commands(json!([{
            "type": "UpdateAssessmentAnswersCommand",
            "user": user(),
            "assessmentUuid": assessment,
            "added": { code: single(value) },
            "removed": [],
        }]))
        .await;
    }

    pub async fn version(&self, assessment: &str) -> Value {
        self.version_at(assessment, None).await
    }

    pub async fn version_at(&self, assessment: &str, timestamp: Option<&str>) -> Value {
        self.query(json!({
            "type": "AssessmentVersionQuery",
            "user": user(),
            "assessmentIdentifier": { "type": "UUID", "uuid": assessment },
            "timestamp": timestamp,
        }))
        .await
    }
}

pub fn user() -> Value {
    json!({ "id": "test-user", "name": "Test User", "authSource": "HMPPS_AUTH" })
}

pub fn single(value: &str) -> Value {
    json!({ "type": "Single", "value": value })
}

pub fn token(roles: &[&str]) -> String {
    let now = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_secs();
    let mut header = Header::new(Algorithm::RS256);
    header.kid = Some("test-key".to_owned());
    encode(
        &header,
        &json!({
            "sub": "test-client",
            "client_id": "test-client",
            "authorities": roles,
            "iat": now,
            "exp": now + 300,
            "aud": "oauth2-resource",
        }),
        &KEYS.encoding,
    )
    .unwrap()
}

async fn start_fake_hmpps_auth() -> String {
    let jwks = KEYS.jwks.clone();
    let router = Router::new().route(
        "/auth/.well-known/jwks.json",
        get(move || {
            let jwks = jwks.clone();
            async move { axum::Json(jwks) }
        }),
    );
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let address = listener.local_addr().unwrap();
    tokio::spawn(async move { axum::serve(listener, router).await.unwrap() });
    format!("http://{address}/auth")
}

/// Wait long enough that the next request gets a later timestamp.
pub async fn tick() {
    tokio::time::sleep(Duration::from_millis(20)).await;
}
