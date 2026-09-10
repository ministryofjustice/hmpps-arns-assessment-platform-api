package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.hook.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregateView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFlagsCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FormConfig
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.hook.UpdateOasysDataMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.DataMappingService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.time.LocalDateTime
import java.util.UUID

class UpdateOasysDataMappingHandlerTest {
  private val now: LocalDateTime = LocalDateTime.now()

  private val dataMappingService: DataMappingService = mockk()
  private val objectMapper = ObjectMapper()
  private val handler = UpdateOasysDataMappingHandler(dataMappingService, objectMapper)

  private val persistenceContext: PersistenceContext = mockk()
  private val eventBus: EventBus = mockk()
  private val clock: Clock = mockk()
  private val services = CommandHandlerServiceBundle(persistenceContext, eventBus, clock)

  private val assessment = AssessmentEntity(type = "TEST", createdAt = now)
  private val commandUser = UserDetails("FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  private val userEntity = UserDetailsEntity(1, UUID.randomUUID(), "FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)

  private val aggregateData = AssessmentAggregate().apply { formVersion = "1" }
  private val aggregateEntity = AggregateEntity(
    updatedAt = now,
    eventsFrom = now,
    eventsTo = now,
    assessment = assessment,
    data = aggregateData,
  )
  private val aggregateState = AssessmentState(aggregateEntity)

  private val formConfig = FormConfig(version = "1")
  private val hook = UpdateOasysDataMapping(formConfig = formConfig)

  private val command: RequestableCommand = UpdateFlagsCommand(
    user = commandUser,
    assessmentUuid = assessment.uuid.toReference(),
    flags = listOf("SAN_BETA"),
  )

  @BeforeEach
  fun setUp() {
    every { persistenceContext.findAssessment(assessment.uuid) } returns assessment
    every { persistenceContext.getAggregateState(assessment, AssessmentAggregate::class, null) } returns aggregateState
    every { persistenceContext.findUserDetails(commandUser) } returns userEntity
    every { clock.requestDateTime() } returns now
    every { eventBus.handle(any<EventEntity<*>>()) } returns mockk(relaxed = true)
  }

  @Test
  fun `it stores the type of the hook it is built to handle`() {
    assertThat(handler.type).isEqualTo(UpdateOasysDataMapping::class)
  }

  @Test
  fun `it maps the assessment's current state to its OASys equivalent using the hook's form config`() {
    val aggregateView = slot<AssessmentAggregateView>()
    every { dataMappingService.getOasysEquivalent(capture(aggregateView), formConfig) } returns emptyMap()

    handler.handle(hook, command, services)

    assertThat(aggregateView.captured).isEqualTo(aggregateData)
  }

  @Test
  fun `it raises an AssessmentPropertiesUpdatedEvent with the OASys equivalent, serialised as a single property value`() {
    val oasysEquivalent = mapOf("foo" to "bar", "baz" to 123)
    every { dataMappingService.getOasysEquivalent(any(), any()) } returns oasysEquivalent

    val handledEvent = slot<EventEntity<*>>()
    every { eventBus.handle(capture(handledEvent)) } returns mockk(relaxed = true)

    handler.handle(hook, command, services)

    val expectedEvent = AssessmentPropertiesUpdatedEvent(
      added = mapOf("oasys_equivalent" to SingleValue(objectMapper.writeValueAsString(oasysEquivalent))),
      removed = emptyList(),
    )
    assertThat(handledEvent.captured.data).isEqualTo(expectedEvent)
  }

  @Test
  fun `it raises the event against the resolved assessment and user, timestamped with the request time`() {
    every { dataMappingService.getOasysEquivalent(any(), any()) } returns emptyMap()

    val handledEvent = slot<EventEntity<*>>()
    every { eventBus.handle(capture(handledEvent)) } returns mockk(relaxed = true)

    handler.handle(hook, command, services)

    assertThat(handledEvent.captured.assessment).isEqualTo(assessment)
    assertThat(handledEvent.captured.user).isEqualTo(userEntity)
    assertThat(handledEvent.captured.createdAt).isEqualTo(now)

    verify(exactly = 1) { eventBus.handle(any<EventEntity<*>>()) }
  }
}
