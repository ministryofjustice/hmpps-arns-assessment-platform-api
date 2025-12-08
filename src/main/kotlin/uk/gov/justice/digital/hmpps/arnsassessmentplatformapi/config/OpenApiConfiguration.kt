package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version ?: "Unknown"

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://arns-assessment-platform-api-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("https://arns-assessment-platform-api-preprod.hmpps.service.justice.gov.uk").description("Pre-Production"),
        Server().url("https://arns-assessment-platform-api.hmpps.service.justice.gov.uk").description("Production"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .tags(
      listOf(),
    )
    .info(
      Info().title("HMPPS Arns Assessment Platform Api").version(version)
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
    .components(
      Components()
        .addSchemas("AnswerValue", answerValueSchema())
        .addSchemas("Answers", answersSchema()),
    )
  // TODO Add security schema and roles in `.components()` and `.addSecurityItem()`

  companion object {
    fun answerValueSchema(): Schema<*> = ComposedSchema()
      .description("A single string or an array of strings")
      .oneOf(
        listOf(
          StringSchema(),
          ArraySchema().items(StringSchema()),
        ),
      )

    fun answersSchema(): Schema<*> = MapSchema()
      .description("Map of field codes to answer values. Each value can be a single string or an array of strings.")
      .additionalProperties(Schema<Any>().`$ref`("#/components/schemas/AnswerValue"))
  }
}

private fun SecurityScheme.addBearerJwtRequirement(role: String): SecurityScheme = type(SecurityScheme.Type.HTTP)
  .scheme("bearer")
  .bearerFormat("JWT")
  .`in`(SecurityScheme.In.HEADER)
  .name("Authorization")
  .description("A HMPPS Auth access token with the `$role` role.")
