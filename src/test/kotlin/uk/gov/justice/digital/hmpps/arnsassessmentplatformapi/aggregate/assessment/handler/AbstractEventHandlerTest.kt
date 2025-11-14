package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

abstract class AbstractEventHandlerTest<E : Event, S : AggregateState<*>> {
  val assessment = AssessmentEntity()
  val mockClock: Clock = mockk()

  val user = User("FOO_USER", "Foo User")
  val timeline = Timeline(type = "test", data = mapOf("foo" to listOf("bar")))

  abstract val handler: KClass<out EventHandler<E, S>>
  abstract val events: List<EventEntity<E>>
  abstract val initialState: S
  abstract val expectedState: S

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { mockClock.now() } returns LocalDateTime.parse("2025-01-01T12:00:00")
  }

  private fun getHandler() = handler.primaryConstructor!!.call(mockClock)
  protected fun eventEntityFor(eventData: E) = EventEntity(
    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
    user = user,
    assessment = assessment,
    data = eventData,
  )

  @Test
  fun `it stores the type of the event it is built to handle`() {
    assertThat(getHandler().eventType).isEqualTo(events.first().data::class)
  }

  @Test
  fun `it handles the event`() {
    val result = events.fold(initialState) { state: S, event -> getHandler().handle(event, initialState) }
    assertThat(result.type).isEqualTo(expectedState.type)
    assertThat(result.aggregates)
      .usingRecursiveComparison()
      .isEqualTo(expectedState.aggregates)
  }
}
