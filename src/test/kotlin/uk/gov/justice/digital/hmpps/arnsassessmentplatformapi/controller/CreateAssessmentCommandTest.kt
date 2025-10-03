package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CreateAssessmentRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CreateAssessmentResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
import kotlin.test.assertIs

class CreateAssessmentCommandTest(
  @Autowired
  val assessmentRepository: AssessmentRepository,
  @Autowired
  val eventRepository: EventRepository,
) : IntegrationTestBase() {

  val user = User("FOO_USER", "Foo User")

  @BeforeEach
  fun setUp() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun `it creates an assessment`() {
    val request = CreateAssessmentRequest(user)

    val response = webTestClient.post().uri("/assessment/create")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody(CreateAssessmentResponse::class.java)
      .returnResult()
      .responseBody

    val assessmentUuid = requireNotNull(response?.assessmentUuid) { "An assessmentUuid should be present on the response" }

    val assessment = assessmentRepository.findByUuid(assessmentUuid)

    assertThat(assessment).isNotNull

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentUuid)

    assertThat(eventsForAssessment.size).isEqualTo(1)
    assertIs<AssessmentCreated>(eventsForAssessment.last().data)
  }
}
