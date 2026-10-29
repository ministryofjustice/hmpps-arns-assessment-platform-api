//! Caches the latest version of each assessment in Redis, for other services to read.

use std::sync::Arc;

use redis::{
    AsyncCommands,
    aio::{ConnectionManager, ConnectionManagerConfig},
};
use tokio::sync::OnceCell;
use uuid::Uuid;

use crate::{config::RedisConfig, query::AssessmentVersionQueryResult};

const CACHE_TTL_SECONDS: u64 = 10 * 60;

pub fn cache_key(assessment_uuid: Uuid) -> String {
    format!("assessment:{assessment_uuid}:latest")
}

#[derive(Clone)]
pub struct CacheService {
    redis: Option<Arc<Redis>>,
}

struct Redis {
    client: redis::Client,
    config: ConnectionManagerConfig,
    connection: OnceCell<ConnectionManager>,
}

impl Redis {
    /// Connects on first use, so the service can start while Redis is unavailable.
    async fn connection(&self) -> redis::RedisResult<ConnectionManager> {
        self.connection
            .get_or_try_init(|| ConnectionManager::new_with_config(self.client.clone(), self.config.clone()))
            .await
            .cloned()
    }
}

impl CacheService {
    pub fn new(config: &RedisConfig) -> Self {
        let scheme = if config.ssl { "rediss" } else { "redis" };
        let credentials = config
            .password
            .as_deref()
            .map(|password| format!(":{}@", percent_encode(password)))
            .unwrap_or_default();
        let url = format!("{scheme}://{credentials}{}:{}", config.host, config.port);

        match redis::Client::open(url) {
            Ok(client) => {
                let redis = Arc::new(Redis {
                    client,
                    config: ConnectionManagerConfig::new()
                        .set_connection_timeout(config.connect_timeout)
                        .set_response_timeout(config.timeout),
                    connection: OnceCell::new(),
                });
                // Connect now rather than on the first request, so the first eviction and the
                // first write to the cache aren't both waiting on the connection, racing each other.
                if let Ok(runtime) = tokio::runtime::Handle::try_current() {
                    let warm_up = Arc::clone(&redis);
                    runtime.spawn(async move {
                        if let Err(error) = warm_up.connection().await {
                            tracing::warn!(%error, "Unable to connect to Redis, will retry when caching");
                        }
                    });
                }
                Self { redis: Some(redis) }
            }
            Err(error) => {
                tracing::error!(%error, "Invalid Redis configuration, so assessments won't be cached");
                Self::disabled()
            }
        }
    }

    pub fn disabled() -> Self {
        Self { redis: None }
    }

    /// Cache the result in the background. Failures are logged rather than returned.
    pub fn cache_latest(&self, result: &AssessmentVersionQueryResult) {
        let Some(redis) = self.redis.clone() else {
            return;
        };
        let assessment_uuid = result.assessment_uuid;
        let payload = match serde_json::to_string(result) {
            Ok(payload) => payload,
            Err(error) => {
                tracing::error!(%error, %assessment_uuid, "Failed to cache latest assessment");
                return;
            }
        };

        tokio::spawn(async move {
            let outcome = async {
                let mut connection = redis.connection().await?;
                connection
                    .set_ex::<_, _, ()>(cache_key(assessment_uuid), payload, CACHE_TTL_SECONDS)
                    .await
            };
            if let Err(error) = outcome.await {
                tracing::error!(%error, %assessment_uuid, "Failed to cache latest assessment");
            }
        });
    }

    /// Remove the cached latest version in the background, once it has changed.
    pub fn evict_latest(&self, assessment_uuid: Uuid) {
        let Some(redis) = self.redis.clone() else {
            return;
        };
        tokio::spawn(async move {
            let outcome = async {
                let mut connection = redis.connection().await?;
                connection.del::<_, ()>(cache_key(assessment_uuid)).await
            };
            if let Err(error) = outcome.await {
                tracing::error!(%error, %assessment_uuid, "Failed to evict cached latest assessment");
            }
        });
    }

    /// For the health check. `None` when caching is disabled.
    pub async fn ping(&self) -> Option<Result<(), String>> {
        let redis = self.redis.as_ref()?;
        let outcome = async {
            let mut connection = redis.connection().await?;
            redis::cmd("PING").query_async::<String>(&mut connection).await
        };
        Some(outcome.await.map(|_| ()).map_err(|error| error.to_string()))
    }
}

fn percent_encode(value: &str) -> String {
    value
        .bytes()
        .map(|byte| match byte {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'.' | b'_' | b'~' => (byte as char).to_string(),
            other => format!("%{other:02X}"),
        })
        .collect()
}
