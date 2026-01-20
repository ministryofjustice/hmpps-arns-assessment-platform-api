package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.File

class ResourceSecurityTest : IntegrationTestBase() {
  @Autowired
  private lateinit var context: ApplicationContext

  private val unprotectedDefaultMethods = setOf(
    "GET /v3/api-docs.yaml",
    "GET /swagger-ui.html",
    "GET /v3/api-docs",
    "GET /v3/api-docs/swagger-config",
    " /error",
  )

  @Test
  fun `Ensure all endpoints protected with PreAuthorize`() {
    // need to exclude any that are forbidden in helm configuration
    val exclusions = File("helm_deploy").walk().filter { it.name.equals("values.yaml") }.flatMap { file ->
      file.readLines().map { line ->
        line.takeIf { it.contains("location") }?.substringAfter("location ")?.substringBefore(" {")
      }
    }.filterNotNull().flatMap { path -> listOf("GET", "POST", "PUT", "DELETE").map { "$it $path" } }
      .toMutableSet().also {
        it.addAll(unprotectedDefaultMethods)
      }

    val beans = context.getBeansOfType(RequestMappingHandlerMapping::class.java)
    beans.forEach { (_, mapping) ->
      mapping.handlerMethods.forEach { (mappingInfo, method) ->
        val classAnnotation = method.beanType.getAnnotation(PreAuthorize::class.java)
        val annotation = method.getMethodAnnotation(PreAuthorize::class.java)
        if (classAnnotation == null && annotation == null) {
          mappingInfo.getMappings().forEach {
            assertThat(exclusions.contains(it)).withFailMessage {
              "Found $mappingInfo of type $method with no PreAuthorize annotation"
            }.isTrue()
          }
        }
      }
    }
  }

  @Test
  fun `Ensure command and query endpoints require ROLE_AAP__FRONTEND_RW or ROLE_AAP__COORDINATOR_RW`() {
    val beans = context.getBeansOfType(RequestMappingHandlerMapping::class.java)
    beans.forEach { (_, mapping) ->
      mapping.handlerMethods.forEach { (mappingInfo, method) ->
        val paths = mappingInfo.pathPatternsCondition?.patternValues ?: emptySet()

        if (paths.any { it == "/command" || it == "/query" }) {
          val classAnnotation = method.beanType.getAnnotation(PreAuthorize::class.java)
          val methodAnnotation = method.getMethodAnnotation(PreAuthorize::class.java)
          val annotation = methodAnnotation ?: classAnnotation

          assertThat(annotation).withFailMessage {
            "Endpoint ${paths.first()} must have @PreAuthorize annotation"
          }.isNotNull()

          assertThat(annotation?.value).withFailMessage {
            "Endpoint ${paths.first()} must require ROLE_AAP__FRONTEND_RW or ROLE_AAP__COORDINATOR_RW, but has: ${annotation?.value}"
          }.isEqualTo("hasAnyRole('ROLE_AAP__FRONTEND_RW', 'ROLE_AAP__COORDINATOR_RW')")
        }
      }
    }
  }
}

private fun RequestMappingInfo.getMappings() = methodsCondition.methods
  .map { it.name }
  .ifEmpty { listOf("") } // if no methods defined then match all rather than none
  .flatMap { method ->
    pathPatternsCondition?.patternValues?.map { "$method $it" } ?: emptyList()
  }
