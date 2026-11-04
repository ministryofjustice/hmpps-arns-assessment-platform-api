//! Health and info endpoints, in the shape Spring Boot Actuator returns them.

use std::time::Duration;

use axum::{Extension, Json, Router, http::StatusCode, routing::get};
use serde_json::{Value, json};

use crate::AppState;

const AUTH_HEALTH_TIMEOUT: Duration = Duration::from_secs(2);

fn up() -> Value {
    json!({ "status": "UP" })
}

fn down(error: impl ToString) -> Value {
    json!({ "status": "DOWN", "details": { "error": error.to_string() } })
}

async fn database(app: &AppState) -> Value {
    match sqlx::query("SELECT 1").execute(&app.pool).await {
        Ok(_) => json!({ "status": "UP", "details": { "database": "PostgreSQL" } }),
        Err(error) => down(error),
    }
}

async fn hmpps_auth(app: &AppState) -> Value {
    let url = format!("{}/health/ping", app.config.hmpps_auth_url.trim_end_matches('/'));
    match app.http.get(&url).timeout(AUTH_HEALTH_TIMEOUT).send().await {
        Ok(response) if response.status().is_success() => {
            json!({ "status": "UP", "details": { "HttpStatus": response.status().as_u16() } })
        }
        Ok(response) => json!({ "status": "DOWN", "details": { "HttpStatus": response.status().as_u16() } }),
        Err(error) => down(error),
    }
}

/// Overall health, which is down when any component is.
async fn health(Extension(app): Extension<AppState>) -> (StatusCode, Json<Value>) {
    let (db, auth, redis) = tokio::join!(database(&app), hmpps_auth(&app), app.cache.ping());

    let mut components = serde_json::Map::new();
    components.insert("db".to_owned(), db);
    components.insert("hmppsAuth".to_owned(), auth);
    components.insert("ping".to_owned(), up());
    if let Some(redis) = redis {
        components.insert("redis".to_owned(), redis.map_or_else(down, |()| up()));
    }

    let all_up = components.values().all(|component| component["status"] == "UP");
    let status = if all_up {
        StatusCode::OK
    } else {
        StatusCode::SERVICE_UNAVAILABLE
    };
    (
        status,
        Json(json!({ "status": if all_up { "UP" } else { "DOWN" }, "components": components })),
    )
}

async fn ping() -> Json<Value> {
    Json(up())
}

/// Ready once the database can be reached.
async fn readiness(Extension(app): Extension<AppState>) -> (StatusCode, Json<Value>) {
    let db = database(&app).await;
    let status = if db["status"] == "UP" {
        StatusCode::OK
    } else {
        StatusCode::SERVICE_UNAVAILABLE
    };
    (status, Json(json!({ "status": db["status"] })))
}

async fn info() -> Json<Value> {
    Json(json!({
        "app": { "name": "HMPPS Arns Assessment Platform Api", "version": "1.0" },
        "build": {
            "name": env!("CARGO_PKG_NAME"),
            "version": option_env!("BUILD_NUMBER").unwrap_or(env!("CARGO_PKG_VERSION")),
        },
    }))
}

pub fn router() -> Router {
    Router::new()
        .route("/health", get(health))
        .route("/health/ping", get(ping))
        .route("/health/liveness", get(ping))
        .route("/health/readiness", get(readiness))
        .route("/info", get(info))
}
