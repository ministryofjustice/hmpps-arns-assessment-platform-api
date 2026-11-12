use std::{net::SocketAddr, sync::Arc};

use axum::{Extension, Router, extract::DefaultBodyLimit};
use sqlx::PgPool;
use tower_http::trace::TraceLayer;

pub mod aggregate;
pub mod clock;
pub mod command;
pub mod config;
pub mod datetime;
pub mod db;
pub mod domain;
pub mod error;
pub mod event;
pub mod hook;
pub mod http;
pub mod model;
pub mod persistence;
pub mod query;
pub mod service;

use config::Config;
use http::auth::JwksCache;
use service::{audit::AuditService, cache::CacheService};

/// Request bodies can be large: commands carry whole sections of answers.
const MAX_BODY_BYTES: usize = 10 * 1024 * 1024;

/// Everything handlers need, available to them as an [`Extension`].
#[derive(Clone)]
pub struct AppState {
    pub pool: PgPool,
    pub config: Arc<Config>,
    pub audit: AuditService,
    pub cache: CacheService,
    pub jwks: Arc<JwksCache>,
    pub http: reqwest::Client,
}

impl AppState {
    pub async fn new(config: Config, pool: PgPool) -> Self {
        let audit = AuditService::new(&config).await;
        let cache = CacheService::new(&config.redis);
        Self::with_services(config, pool, audit, cache)
    }

    pub fn with_services(config: Config, pool: PgPool, audit: AuditService, cache: CacheService) -> Self {
        let http = reqwest::Client::new();
        Self {
            jwks: Arc::new(JwksCache::new(&config.hmpps_auth_url, http.clone())),
            config: Arc::new(config),
            pool,
            audit,
            cache,
            http,
        }
    }
}

pub fn app(state: AppState) -> Router {
    Router::new()
        .merge(http::openapi::router())
        .merge(http::health::router())
        .merge(http::assessment::router())
        .merge(http::data_deletion::router())
        .merge(http::plan::router())
        .merge(http::subject_access_request::router())
        .fallback(http::error::not_found)
        .layer(DefaultBodyLimit::max(MAX_BODY_BYTES))
        .layer(TraceLayer::new_for_http())
        .layer(Extension(state))
}

pub async fn serve(state: AppState) {
    let addr = SocketAddr::from(([0, 0, 0, 0], state.config.server_port));

    tracing::info!("Server listening on: {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app(state).into_make_service())
        .with_graceful_shutdown(shutdown_signal())
        .await
        .expect("Failed to start server")
}

async fn shutdown_signal() {
    let ctrl_c = async {
        tokio::signal::ctrl_c().await.expect("Failed to listen for ctrl-c");
    };
    let terminate = async {
        tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
            .expect("Failed to listen for SIGTERM")
            .recv()
            .await;
    };

    tokio::select! {
        () = ctrl_c => {},
        () = terminate => {},
    }
    tracing::info!("Shutting down");
}
