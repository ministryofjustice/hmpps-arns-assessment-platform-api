use std::{sync::LazyLock, time::Duration};

use sqlx::{
    ConnectOptions,
    migrate::{MigrateError, Migrator},
    pool::PoolOptions,
    postgres::{PgConnectOptions, PgPool},
};
use tracing::info;

use crate::config::Config;

#[derive(thiserror::Error, Debug)]
pub enum Error {
    #[error("Invalid schema name: {0}")]
    InvalidSchemaName(String),
    #[error("Invalid database endpoint: {0}")]
    InvalidEndpoint(String),
    #[error("{0}")]
    Database(#[from] sqlx::Error),
    #[error("{0}")]
    Migrate(#[from] MigrateError),
}

pub static MIGRATOR: Migrator = sqlx::migrate!("./db/migrations");

/// Allows hyphens, because the default schema is `assessment-platform`. Names are always quoted.
static SCHEMA_RE: LazyLock<regex::Regex> = LazyLock::new(|| regex::Regex::new(r"^[A-Za-z_][0-9A-Za-z_-]*$").unwrap());

/// Arbitrary, but must stay the same so replicas starting together don't both adopt the history.
const ADOPT_FLYWAY_LOCK: i64 = 0x4141_5041_5049;

fn validate_schema_name(name: &str) -> Result<(), Error> {
    const MAX_LEN: usize = 63; // PostgreSQL limit on identifiers
    if name.len() > MAX_LEN {
        return Err(Error::InvalidSchemaName("Exceeds max length".into()));
    }

    if !SCHEMA_RE.is_match(name) {
        return Err(Error::InvalidSchemaName(
            "Must start with letter/underscore, and contain only letters, digits, underscores and hyphens".into(),
        ));
    }

    Ok(())
}

fn connect_options(config: &Config) -> Result<PgConnectOptions, Error> {
    let database = &config.database;
    if let Some(url) = &database.url {
        return Ok(url.parse()?);
    }

    let (host, port) = match database.endpoint.rsplit_once(':') {
        Some((host, port)) => (
            host,
            port.parse()
                .map_err(|_| Error::InvalidEndpoint(database.endpoint.clone()))?,
        ),
        None => (database.endpoint.as_str(), 5432),
    };

    Ok(PgConnectOptions::new()
        .host(host)
        .port(port)
        .database(&database.name)
        .username(&database.username)
        .password(&database.password))
}

/// Initialise the database pool, create the schema, and run migrations.
///
/// Sessions use the service's time zone, as the Kotlin service's did, so date functions such as
/// `DATE(created_at)` give the same results.
pub async fn init(config: &Config) -> Result<PgPool, Error> {
    let schema = &config.database.schema;
    validate_schema_name(schema)?;
    let quoted_schema = format!("\"{schema}\"");

    let options = connect_options(config)?
        .options([
            ("search_path", quoted_schema.as_str()),
            ("TimeZone", config.time_zone.name()),
        ])
        .log_statements(tracing::log::LevelFilter::Debug)
        .log_slow_statements(tracing::log::LevelFilter::Warn, Duration::from_millis(250));

    let pool = PoolOptions::new()
        .max_connections(config.database.pool_size)
        .min_connections(2)
        .acquire_timeout(Duration::from_secs(15))
        .max_lifetime(Some(Duration::from_secs(30 * 60)))
        .idle_timeout(Some(Duration::from_secs(10 * 60)))
        .connect_with(options)
        .await?;

    sqlx::query(&format!("CREATE SCHEMA IF NOT EXISTS {quoted_schema}"))
        .execute(&pool)
        .await?;

    adopt_flyway_history(&pool).await?;
    MIGRATOR.run(&pool).await?;

    info!("Database schema '{}' ready", schema);

    Ok(pool)
}

/// Databases created by the Kotlin service record their migrations in Flyway's history table.
/// The first time this service starts against one, record the same migrations as applied in
/// sqlx's history table, so they aren't run again.
async fn adopt_flyway_history(pool: &PgPool) -> Result<(), Error> {
    let mut tx = pool.begin().await?;
    sqlx::query("SELECT pg_advisory_xact_lock($1)")
        .bind(ADOPT_FLYWAY_LOCK)
        .execute(&mut *tx)
        .await?;

    let has_flyway_history: bool = sqlx::query_scalar("SELECT to_regclass('flyway_schema_history') IS NOT NULL")
        .fetch_one(&mut *tx)
        .await?;
    if !has_flyway_history {
        return Ok(());
    }

    sqlx::query(
        r#"
            CREATE TABLE IF NOT EXISTS _sqlx_migrations (
                version BIGINT PRIMARY KEY,
                description TEXT NOT NULL,
                installed_on TIMESTAMPTZ NOT NULL DEFAULT now(),
                success BOOLEAN NOT NULL,
                checksum BYTEA NOT NULL,
                execution_time BIGINT NOT NULL
            )
        "#,
    )
    .execute(&mut *tx)
    .await?;

    let already_adopted: bool = sqlx::query_scalar("SELECT EXISTS (SELECT 1 FROM _sqlx_migrations)")
        .fetch_one(&mut *tx)
        .await?;
    if already_adopted {
        return Ok(());
    }

    let flyway_versions: Vec<String> =
        sqlx::query_scalar("SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL")
            .fetch_all(&mut *tx)
            .await?;
    let flyway_versions: Vec<i64> = flyway_versions
        .iter()
        .filter_map(|version| version.parse().ok())
        .collect();

    let mut adopted = 0;
    for migration in MIGRATOR
        .iter()
        .filter(|migration| flyway_versions.contains(&migration.version))
    {
        sqlx::query(
            r#"
                INSERT INTO _sqlx_migrations (version, description, success, checksum, execution_time)
                VALUES ($1, $2, TRUE, $3, 0)
            "#,
        )
        .bind(migration.version)
        .bind(&*migration.description)
        .bind(&*migration.checksum)
        .execute(&mut *tx)
        .await?;
        adopted += 1;
    }

    tx.commit().await?;
    info!("Adopted {adopted} migration(s) already applied by Flyway");
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn accepts_the_default_schema_name() {
        assert!(validate_schema_name("assessment-platform").is_ok());
        assert!(validate_schema_name("public").is_ok());
    }

    #[test]
    fn rejects_unsafe_schema_names() {
        assert!(validate_schema_name("bad\"name").is_err());
        assert!(validate_schema_name("1starts_with_digit").is_err());
        assert!(validate_schema_name(&"a".repeat(64)).is_err());
    }
}
