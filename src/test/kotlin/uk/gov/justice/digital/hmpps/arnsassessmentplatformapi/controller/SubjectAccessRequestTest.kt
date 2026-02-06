package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.RenderedValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestAssessmentVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestQueryResult
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubjectAccessRequestTest(
  @Autowired
  val assessmentRepository: AssessmentRepository,
  @Autowired
  val aggregateRepository: AggregateRepository,
  @Autowired
  val objectMapper: ObjectMapper,
) : IntegrationTestBase() {
  private val testCrn = UUID.randomUUID().toString()
  private val testPrn = UUID.randomUUID().toString()

  private val expectedContent = HmppsSubjectAccessRequestContent(
    content = SubjectAccessRequestQueryResult(
      results = listOf(
        SubjectAccessRequestAssessmentVersion(
          assessmentType = "TEST",
          createdAt = LocalDateTime.parse("2026-01-01T12:00:00"),
          updatedAt = LocalDateTime.parse("2026-01-02T12:00:00"),
          answers = listOf(
            RenderedValue("foo", single = "foo_value"),
          ),
          properties = emptyList(),
          collections = emptyList(),
          identifiers = mapOf(
            IdentifierType.CRN to testCrn,
            IdentifierType.PRN to testPrn,
          ),
        ),
      ),
    ),
  )

  @BeforeAll
  fun setup() {
    val assessment = assessmentRepository.save(
      AssessmentEntity(
        type = "TEST",
      ).apply {
        identifiers.add(
          AssessmentIdentifierEntity(
            externalIdentifier = IdentifierPair(IdentifierType.CRN, testCrn),
            assessment = this,
          ),
        )
        identifiers.add(
          AssessmentIdentifierEntity(
            externalIdentifier = IdentifierPair(IdentifierType.PRN, testPrn),
            assessment = this,
          ),
        )
      },
    )

    aggregateRepository.save(
      AggregateEntity(
        updatedAt = LocalDateTime.parse("2026-01-02T12:00:00"),
        eventsFrom = LocalDateTime.parse("2026-01-01T12:00:00"),
        eventsTo = LocalDateTime.parse("2026-01-02T12:00:00"),
        assessment = assessment,
        numberOfEventsApplied = 3,
        data = AssessmentAggregate().apply {
          answers["foo"] = SingleValue("foo_value")
          formVersion = "v1.0"
        },
      ),
    )
  }

  @Nested
  inner class Security {
    @Test
    fun `access unauthorized when there is no token`() {
      webTestClient.get().uri("/subject-access-request?crn=$testCrn")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when there are no roles on the token`() {
      webTestClient.get().uri("/subject-access-request?crn=$testCrn")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when the token has the wrong role`() {
      webTestClient.get().uri("/subject-access-request?crn=$testCrn")
        .headers(setAuthorisation(roles = listOf("ROLE_IS_WRONG")))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  inner class ByCrn {
    @Test
    fun `it returns the assessments for a CRN`() {
      val response = webTestClient.get().uri("/subject-access-request?crn=$testCrn")
        .headers(setAuthorisation(roles = listOf("SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(HmppsSubjectAccessRequestContent::class.java)
        .returnResult()

      assertNotNull(response.responseBody)
      val actualResponseBody = assertInstanceOf<HmppsSubjectAccessRequestContent>(response.responseBody)

      val expectedResponseBody: Any =
        objectMapper.convertValue(expectedContent, HmppsSubjectAccessRequestContent::class.java)

      assertEquals(expectedResponseBody, actualResponseBody)
    }

    @Test
    fun `it returns no content when there is no assessment for the CRN identifier`() {
      webTestClient.get().uri("/subject-access-request?crn=DOES_NOT_EXIST")
        .headers(setAuthorisation(roles = listOf("SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Nested
  inner class ByPrn {
    @Test
    fun `it returns the assessments for a PRN`() {
      val response = webTestClient.get().uri("/subject-access-request?prn=$testPrn")
        .headers(setAuthorisation(roles = listOf("SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(HmppsSubjectAccessRequestContent::class.java)
        .returnResult()

      assertNotNull(response.responseBody)
      val actualResponseBody = assertInstanceOf<HmppsSubjectAccessRequestContent>(response.responseBody)

      val expectedResponseBody: Any =
        objectMapper.convertValue(expectedContent, HmppsSubjectAccessRequestContent::class.java)

      assertEquals(expectedResponseBody, actualResponseBody)
    }

    @Test
    fun `it returns no content when there is no assessment for the CRN identifier`() {
      webTestClient.get().uri("/subject-access-request?prn=DOES_NOT_EXIST")
        .headers(setAuthorisation(roles = listOf("SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Nested
  inner class ByCrnAndPrn {
    @Test
    fun `it returns the assessments for a CRN and PRN`() {
      val response = webTestClient.get().uri("/subject-access-request?crn=$testCrn&prn=$testPrn")
        .headers(setAuthorisation(roles = listOf("SAR_DATA_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(HmppsSubjectAccessRequestContent::class.java)
        .returnResult()

      assertNotNull(response.responseBody)
      val actualResponseBody = assertInstanceOf<HmppsSubjectAccessRequestContent>(response.responseBody)

      val expectedResponseBody: Any =
        objectMapper.convertValue(expectedContent, HmppsSubjectAccessRequestContent::class.java)

      assertEquals(expectedResponseBody, actualResponseBody)
    }
  }
}
