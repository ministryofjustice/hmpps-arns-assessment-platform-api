package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import org.apache.hc.core5.http.HttpHeaders
import org.apache.hc.core5.http.HttpRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentFlagsUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.UserDetailsRepository
import java.time.LocalDateTime
import java.util.UUID

@Provider("hmpps-arns-assessment-platform-api")
@PactBroker(url = $$"${pactbroker.url}")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AssessmentVersionQueryPactTest(
  @Autowired
  private val assessmentRepository: AssessmentRepository,
  @Autowired
  private val eventRepository: EventRepository,
  @Autowired
  private val userDetailsRepository: UserDetailsRepository,
  @Autowired
  private val aggregateRepository: AggregateRepository,
) : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", port)
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  fun pactVerificationTestTemplate(context: PactVerificationContext, request: HttpRequest) {
    val token = jwtAuthHelper.createJwtAccessToken(clientId = "hmpps-arns-assessment-platform-api", username = "AUTH_ADM", roles = listOf("ROLE_AAP__FRONTEND_RW"))
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    context.verifyInteraction()
  }

  @State("I have a sentence plan")
  fun `I have a sentence plan`(): Map<String, UUID> {
    val assessment = AssessmentEntity(type = "SENTENCE_PLAN", createdAt = clock.now())
      .apply {
        identifiers.add(
          AssessmentIdentifierEntity(
            externalIdentifier = IdentifierPair(IdentifierType.CRN, "21256"),
            assessment = this,
            createdAt = clock.now(),
          ),
        )
      }
      .run(assessmentRepository::save)
    userDetailsRepository.save(testUserDetailsEntity)

    val events = listOf(
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(
          formVersion = "1",
          properties = mapOf(),
        ),
        position = 1,
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent(version = "1"),
        position = 2,
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("foo_value")),
          removed = emptyList(),
        ),
        position = 3,
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AssessmentPropertiesUpdatedEvent(
          added = mapOf("PLAN_TYPE" to SingleValue("INITIAL")),
          removed = emptyList(),
        ),
        position = 4,
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AssessmentFlagsUpdatedEvent(
          flags = listOf("SAN_BETA")
        ),
        position = 5,
      ),
    ).run(eventRepository::saveAll)

    val sanBeta = listOf("SAN_BETA")

    val aggregateData = AssessmentAggregate().apply {
      collaborators.add(testUserDetailsEntity.uuid)
      formVersion = "1"
      flags.addAll(sanBeta)
      properties.putAll(mapOf("PLAN_TYPE" to SingleValue("INITIAL")))
    }

    AggregateEntity(
      assessment = assessment,
      eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsTo = LocalDateTime.parse("2025-01-01T12:05:00"),
      updatedAt = clock.now(),
      data = aggregateData,
      position = 1,
    )
    .apply { numberOfEventsApplied = 1 }
    .run(aggregateRepository::save)

    return mapOf("assessmentUuid" to assessment.uuid)
  }
}
