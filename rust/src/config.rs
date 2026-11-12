//! Configuration read from the environment, using the same variable names the Helm chart
//! already provides to the Kotlin service.

use std::{env, str::FromStr, time::Duration};

use chrono_tz::Tz;

#[derive(thiserror::Error, Debug)]
pub enum Error {
    #[error("Invalid value '{value}' for {name}: {reason}")]
    Invalid {
        name: &'static str,
        value: String,
        reason: String,
    },
}

#[derive(Clone, Debug)]
pub struct Config {
    pub server_port: u16,
    pub service_name: String,
    pub time_zone: Tz,
    pub database: DatabaseConfig,
    pub hmpps_auth_url: String,
    pub redis: RedisConfig,
    pub audit: AuditConfig,
    /// Oldest `since` accepted by the "modified/deleted since" queries; 0 disables the check.
    pub query_max_lookback_days: i64,
}

#[derive(Clone, Debug)]
pub struct DatabaseConfig {
    /// Overrides the individual connection settings when set.
    pub url: Option<String>,
    pub endpoint: String,
    pub name: String,
    pub username: String,
    pub password: String,
    pub schema: String,
    pub pool_size: u32,
}

#[derive(Clone, Debug)]
pub struct RedisConfig {
    pub host: String,
    pub port: u16,
    pub password: Option<String>,
    pub ssl: bool,
    pub timeout: Duration,
    pub connect_timeout: Duration,
}

#[derive(Clone, Debug)]
pub struct AuditConfig {
    /// Audit events are only logged when no queue is configured.
    pub queue_name: Option<String>,
    pub localstack_url: Option<String>,
}

impl Config {
    pub fn from_env() -> Result<Self, Error> {
        Ok(Self {
            server_port: parsed("SERVER_PORT", 8080)?,
            service_name: "hmpps-arns-assessment-platform-api".to_owned(),
            time_zone: parsed("TZ", chrono_tz::Europe::London)?,
            database: DatabaseConfig {
                url: optional("DATABASE_URL"),
                endpoint: string("DATABASE_ENDPOINT", "localhost:5432"),
                name: string("DATABASE_NAME", "postgres"),
                username: string("DATABASE_USERNAME", "root"),
                password: string("DATABASE_PASSWORD", "dev"),
                schema: string("DATABASE_SCHEMA_NAME", "assessment-platform"),
                pool_size: parsed("DATABASE_POOL_SIZE", 20)?,
            },
            hmpps_auth_url: string("HMPPS_AUTH_URL", "http://localhost:8090/auth"),
            redis: RedisConfig {
                host: string(spring_or("SPRING_DATA_REDIS_HOST", "CACHE_REDIS_HOST"), "localhost"),
                port: parsed(spring_or("SPRING_DATA_REDIS_PORT", "CACHE_REDIS_PORT"), 6379)?,
                password: optional(spring_or("SPRING_DATA_REDIS_PASSWORD", "CACHE_REDIS_AUTH_TOKEN")),
                ssl: parsed(
                    spring_or("SPRING_DATA_REDIS_SSL_ENABLED", "CACHE_REDIS_SSL_ENABLED"),
                    false,
                )?,
                timeout: duration(
                    spring_or("SPRING_DATA_REDIS_TIMEOUT", "CACHE_REDIS_TIMEOUT"),
                    Duration::from_secs(5),
                )?,
                connect_timeout: duration(
                    spring_or("SPRING_DATA_REDIS_CONNECT_TIMEOUT", "CACHE_REDIS_CONNECT_TIMEOUT"),
                    Duration::from_secs(2),
                )?,
            },
            audit: AuditConfig {
                queue_name: optional("HMPPS_SQS_QUEUES_AUDIT_QUEUE_NAME"),
                localstack_url: match optional("HMPPS_SQS_PROVIDER").as_deref() {
                    Some("localstack") => Some(string("HMPPS_SQS_LOCALSTACK_URL", "http://localhost:4566")),
                    _ => None,
                },
            },
            query_max_lookback_days: parsed("QUERY_MAX_LOOKBACK_DAYS", 1)?,
        })
    }
}

fn optional(name: &str) -> Option<String> {
    env::var(name).ok().filter(|value| !value.is_empty())
}

/// Spring Boot also reads its own property names from the environment, and they win over the
/// service's variables (the UI repo's compose file sets `SPRING_DATA_REDIS_HOST`, for example), so
/// they're honoured too, to keep this a drop-in replacement.
fn spring_or(spring_name: &'static str, name: &'static str) -> &'static str {
    if optional(spring_name).is_some() {
        spring_name
    } else {
        name
    }
}

fn string(name: &str, default: &str) -> String {
    optional(name).unwrap_or_else(|| default.to_owned())
}

fn parsed<T>(name: &'static str, default: T) -> Result<T, Error>
where
    T: FromStr,
    T::Err: std::fmt::Display,
{
    match optional(name) {
        None => Ok(default),
        Some(value) => value.parse().map_err(|err: T::Err| Error::Invalid {
            name,
            reason: err.to_string(),
            value,
        }),
    }
}

/// Parses Spring-style durations such as `5s`, `2000ms` or `1m`.
fn duration(name: &'static str, default: Duration) -> Result<Duration, Error> {
    let Some(value) = optional(name) else {
        return Ok(default);
    };
    parse_duration(&value).ok_or_else(|| Error::Invalid {
        name,
        value,
        reason: "expected a duration such as 5s or 2000ms".to_owned(),
    })
}

fn parse_duration(value: &str) -> Option<Duration> {
    let split = value.find(|c: char| !c.is_ascii_digit()).unwrap_or(value.len());
    let (amount, unit) = value.split_at(split);
    let amount: u64 = amount.parse().ok()?;
    match unit {
        "ms" => Some(Duration::from_millis(amount)),
        "" | "s" => Some(Duration::from_secs(amount)),
        "m" => Some(Duration::from_secs(amount * 60)),
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_spring_durations() {
        assert_eq!(parse_duration("5s"), Some(Duration::from_secs(5)));
        assert_eq!(parse_duration("2000ms"), Some(Duration::from_millis(2000)));
        assert_eq!(parse_duration("1m"), Some(Duration::from_secs(60)));
        assert_eq!(parse_duration("7"), Some(Duration::from_secs(7)));
        assert_eq!(parse_duration("5 seconds"), None);
    }
}
