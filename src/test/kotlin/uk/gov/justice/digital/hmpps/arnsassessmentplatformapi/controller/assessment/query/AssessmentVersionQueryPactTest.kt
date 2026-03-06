package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.HttpHeaders
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.spring.spring7.PactVerificationSpring7Provider
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.UserDetailsRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

@Provider("hmpps-arns-assessment-platform-api")
@PactBroker
@ExtendWith(PactVerificationSpring7Provider::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AssessmentVersionQueryPactTest(
  @Autowired
  private val assessmentRepository: AssessmentRepository,
  @Autowired
  private val aggregateRepository: AggregateRepository,
  @Autowired
  private val eventRepository: EventRepository,
  @Autowired
  private val userDetailsRepository: UserDetailsRepository,
) : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", port)
  }

  @TestTemplate
  fun pactVerificationTestTemplate(context: PactVerificationContext, request: HttpRequest) {
    val token = jwtAuthHelper.createJwtAccessToken(clientId = "hmpps-arns-assessment-platform-api", username = "AUTH_ADM", roles = listOf("ROLE_AAP__FRONTEND_RW"))
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    context.verifyInteraction()
  }

  @State("I have a sentence plan")
  fun `I have a sentence plan`(data: Map<String, String>) {
    val assessment = AssessmentEntity(type = "TEST", createdAt = clock.now(), uuid = UUID.fromString("0cb5ffb3-2572-423d-97cd-4a05b681e6c0"))
    assessment.apply {
      identifiers.add(
        AssessmentIdentifierEntity(
          externalIdentifier = IdentifierPair(IdentifierType.CRN, UUID.randomUUID().toString()),
          assessment = this,
          createdAt = clock.now(),
        ),
      )
    }.let { assessment ->
      listOf(
        Arguments.of(assessment, UuidIdentifier(UUID.fromString("0cb5ffb3-2572-423d-97cd-4a05b681e6c0"))),
        Arguments.of(
          assessment,
          with(assessment.identifiers.first()) {
            ExternalIdentifier(
              identifier = externalIdentifier.id,
              identifierType = externalIdentifier.type,
              assessmentType = assessment.type,
            )
          },
        ),
      )
    }
    assessmentRepository.save(assessment)

    listOf(
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(
          formVersion = "1",
          properties = mapOf(),
        ),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent(version = "1"),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("foo_value")),
          removed = emptyList(),
        ),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("updated_foo_value")),
          removed = emptyList(),
        ),
      ),
    ).run(eventRepository::saveAll)
  }
}
