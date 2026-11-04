use hmpps_arns_assessment_platform_api::{AppState, config::Config, datetime, db, serve};
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() {
    // The image has no shell or curl, so container healthchecks run "/app/app healthcheck"
    if std::env::args().nth(1).as_deref() == Some("healthcheck") {
        std::process::exit(healthcheck().await);
    }
    dotenvy::dotenv().ok();
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")))
        .init();

    let config = Config::from_env().expect("Invalid configuration");
    datetime::init_zone(config.time_zone);

    let database_pool = db::init(&config).await.expect("Unable to initialise the database");

    serve(AppState::new(config, database_pool).await).await;
}

/// Pings the running server, returning 0 if it's up and 1 if not.
async fn healthcheck() -> i32 {
    let port = std::env::var("SERVER_PORT").unwrap_or_else(|_| "8080".to_string());
    let client = reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(5))
        .build()
        .expect("Unable to build the HTTP client");
    match client.get(format!("http://localhost:{port}/health/ping")).send().await {
        Ok(response) if response.status().is_success() => 0,
        Ok(response) => {
            eprintln!("{}", response.status());
            1
        }
        Err(error) => {
            eprintln!("{error}");
            1
        }
    }
}
