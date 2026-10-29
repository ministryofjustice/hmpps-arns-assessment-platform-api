use axum::Router;
use utoipa::{
    Modify, OpenApi,
    openapi::security::{HttpAuthScheme, HttpBuilder, SecurityScheme},
};
use utoipa_swagger_ui::SwaggerUi;

#[derive(OpenApi)]
#[openapi(
    info(
        title = "HMPPS Arns Assessment Platform Api",
        contact(name = "HMPPS Digital Studio", email = "feedback@digital.justice.gov.uk")
    ),
    servers(
        (url = "https://arns-assessment-platform-api-dev.hmpps.service.justice.gov.uk", description = "Development"),
        (url = "https://arns-assessment-platform-api-preprod.hmpps.service.justice.gov.uk", description = "Pre-Production"),
        (url = "https://arns-assessment-platform-api.hmpps.service.justice.gov.uk", description = "Production"),
        (url = "http://localhost:8080", description = "Local"),
    ),
    paths(
        super::assessment::execute_commands,
        super::assessment::execute_queries,
        super::assessment::delete_assessment,
        super::assessment::rebuild_aggregates,
        super::data_deletion::get_data,
        super::data_deletion::update_data,
        super::plan::start_new_period_of_supervision,
        super::subject_access_request::subject_access_request,
        super::subject_access_request::template,
    ),
    components(schemas(
        crate::datetime::LocalDateTime,
        crate::command::Reference,
        super::error::ErrorResponse,
    )),
    modifiers(&SecurityAddon),
    security(("bearer-jwt" = []))
)]
struct ApiDoc;

struct SecurityAddon;

impl Modify for SecurityAddon {
    fn modify(&self, openapi: &mut utoipa::openapi::OpenApi) {
        if let Some(components) = openapi.components.as_mut() {
            components.add_security_scheme(
                "bearer-jwt",
                SecurityScheme::Http(
                    HttpBuilder::new()
                        .scheme(HttpAuthScheme::Bearer)
                        .bearer_format("JWT")
                        .build(),
                ),
            );
        }
    }
}

/// Swagger UI at `/swagger-ui`, and the OpenAPI document where the Kotlin service served it.
pub fn router() -> Router {
    let mut openapi = ApiDoc::openapi();
    openapi.info.version = option_env!("BUILD_NUMBER")
        .unwrap_or(env!("CARGO_PKG_VERSION"))
        .to_owned();

    Router::new().merge(SwaggerUi::new("/swagger-ui").url("/v3/api-docs", openapi))
}
