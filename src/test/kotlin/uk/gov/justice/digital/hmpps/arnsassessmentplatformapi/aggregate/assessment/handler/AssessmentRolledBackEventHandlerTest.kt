package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import java.time.LocalDateTime
import java.util.UUID

class AssessmentRolledBackEventHandlerTest {
  private val aggregateUuid: UUID = UUID.randomUUID()
  private val assessment = AssessmentEntity(type = "TEST")
  private val mockClock: Clock = mockk()
  private val stateProvider: StateService.StateForType<AssessmentAggregate> = mockk()
  private val stateService: StateService = mockk()
  private val user = UserDetailsEntity(1, UUID.randomUUID(), "FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  private val timeline = Timeline("test", mapOf("foo" to listOf("bar")))

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { mockClock.now() } returns LocalDateTime.parse("2025-01-01T12:00:00")
  }

  private fun getHandler(): EventHandler<AssessmentRolledBackEvent, AssessmentState> = AssessmentRolledBackEventHandler(mockClock, stateService)

  private fun eventEntityFor(eventData: AssessmentRolledBackEvent) = EventEntity(
    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
    user = user,
    assessment = assessment,
    data = eventData,
  )

  @Test
  fun `it handles the correct event type`() {
    assertThat(getHandler().eventType).isEqualTo(AssessmentRolledBackEvent::class)
  }

  @Test
  fun `it handles the event`() {
    val event =
      eventEntityFor(
        AssessmentRolledBackEvent(
          rolledBackTo = LocalDateTime.parse("2025-01-01T09:00:00"),
          timeline = timeline,
        ),
      )

    val previousState = AssessmentState().also { state ->
      state.aggregates.add(
        AggregateEntity(
          uuid = aggregateUuid,
          eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
          data = AssessmentAggregate().apply {
            formVersion = "1"
            answers.put("foo", SingleValue("rolled_back"))
          },
          assessment = assessment,
        ),
      )
    }

    val currentState = AssessmentState().also { state ->
      state.aggregates.add(
        AggregateEntity(
          uuid = aggregateUuid,
          eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
          data = AssessmentAggregate().apply {
            formVersion = "1"
            answers.putAll(
              mapOf(
                "foo" to SingleValue("foo_current_value"),
                "bar" to SingleValue("bar_current_value"),
              ),
            )
          },
          assessment = assessment,
        ),
      )
    }
    val expectedState = AssessmentState().also { state ->
      state.aggregates.add(
        AggregateEntity(
          uuid = aggregateUuid,
          updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
          eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
          eventsTo = event.createdAt,
          numberOfEventsApplied = 1,
          assessment = assessment,
          data = AssessmentAggregate().apply {
            formVersion = "1"
            collaborators.add(user.uuid)
            answers.put("foo", SingleValue("rolled_back"))
            timeline.add(
              TimelineItem(
                "test",
                LocalDateTime.parse("2025-01-01T12:00:00"),
                mapOf("foo" to listOf("bar")),
              ),
            )
          },
        ),
      )
    }

    every { stateProvider.fetchOrCreateStateForExactPointInTime(assessment, event.data.rolledBackTo) } returns previousState
    every { stateService.stateForType(AssessmentAggregate::class) } returns stateProvider

    val result = getHandler().handle(event, currentState)

    assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(expectedState)
  }

  @Test
  fun `it handles when no timeline provided`() {
    val event =
      eventEntityFor(
        AssessmentRolledBackEvent(
          rolledBackTo = LocalDateTime.parse("2025-01-01T09:00:00"),
          timeline = null,
        ),
      )

    val previousState = AssessmentState().also { state ->
      state.aggregates.add(
        AggregateEntity(
          uuid = aggregateUuid,
          eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
          data = AssessmentAggregate().apply {
            formVersion = "1"
            answers.put("foo", SingleValue("rolled_back"))
          },
          assessment = assessment,
        ),
      )
    }

    val currentState = AssessmentState().also { state ->
      state.aggregates.add(
        AggregateEntity(
          uuid = aggregateUuid,
          eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
          data = AssessmentAggregate().apply {
            formVersion = "1"
            answers.putAll(
              mapOf(
                "foo" to SingleValue("foo_current_value"),
              ),
            )
          },
          assessment = assessment,
        ),
      )
    }
    val expectedState = AssessmentState().also { state ->
      state.aggregates.add(
        AggregateEntity(
          uuid = aggregateUuid,
          updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
          eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
          eventsTo = event.createdAt,
          numberOfEventsApplied = 1,
          assessment = assessment,
          data = AssessmentAggregate().apply {
            formVersion = "1"
            collaborators.add(user.uuid)
            answers.put("foo", SingleValue("rolled_back"))
          },
        ),
      )
    }

    every { stateProvider.fetchOrCreateStateForExactPointInTime(assessment, event.data.rolledBackTo) } returns previousState
    every { stateService.stateForType(AssessmentAggregate::class) } returns stateProvider

    val result = getHandler().handle(event, currentState)

    assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(expectedState)
  }
}
