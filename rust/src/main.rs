use hmpps_arns_assessment_platform_api::{AppState, config::Config, datetime, db, serve};
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() {
    dotenvy::dotenv().ok();
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")))
        .init();

    let config = Config::from_env().expect("Invalid configuration");
    datetime::init_zone(config.time_zone);

    let database_pool = db::init(&config).await.expect("Unable to initialise the database");

    serve(AppState::new(config, database_pool).await).await;
}
