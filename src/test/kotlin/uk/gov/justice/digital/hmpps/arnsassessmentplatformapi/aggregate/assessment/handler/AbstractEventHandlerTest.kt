package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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

sealed interface Scenario<E : Event, S : AggregateState<*>> {
  val name: String

  class Executes<E : Event, S : AggregateState<*>>(
    override val name: String,
  ) : Scenario<E, S> {
    lateinit var events: List<EventEntity<E>>
    lateinit var initialState: S
    lateinit var expectedState: S
  }

  class Throws<E : Event, S : AggregateState<*>, T : Throwable>(
    override val name: String,
  ) : Scenario<E, S> {
    lateinit var events: List<EventEntity<E>>
    lateinit var initialState: S
    lateinit var expectedException: KClass<out T>
  }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractEventHandlerTest<E : Event, S : AggregateState<*>> {

  abstract val handler: KClass<out EventHandler<E, S>>
  abstract val eventType: KClass<out E>
  abstract val scenarios: List<Scenario<E, S>>

  protected val assessment = AssessmentEntity()
  protected val mockClock: Clock = mockk()
  protected val user = User("FOO_USER", "Foo User")
  protected val timeline = Timeline("test", mapOf("foo" to listOf("bar")))

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { mockClock.now() } returns LocalDateTime.parse("2025-01-01T12:00:00")
  }

  protected fun getHandler(): EventHandler<E, S> = handler.primaryConstructor!!.call(mockClock)

  protected fun eventEntityFor(eventData: E) = EventEntity(
    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
    user = user,
    assessment = assessment,
    data = eventData,
  )

  @Test
  fun `it handles the correct event type`() {
    assertThat(getHandler().eventType).isEqualTo(eventType)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scenarioProvider")
  fun runScenario(
    @Suppress("UNUSED_PARAMETER") name: String,
    scenario: Scenario<E, S>,
  ) {
    when (scenario) {
      is Scenario.Executes -> {
        val result = scenario.events.fold(scenario.initialState) { state, event ->
          getHandler().handle(event, state)
        }

        assertThat(result.type).isEqualTo(scenario.expectedState.type)
        assertThat(result.aggregates)
          .usingRecursiveComparison()
          .isEqualTo(scenario.expectedState.aggregates)
      }

      is Scenario.Throws<*, *, *> -> {
        @Suppress("UNCHECKED_CAST")
        val th = scenario as Scenario.Throws<E, S, Throwable>

        assertThrows(th.expectedException.java) {
          th.events.fold(th.initialState) { state, event ->
            getHandler().handle(event, state)
          }
        }
      }
    }
  }

  fun scenarioProvider(): List<Arguments> = scenarios.map { Arguments.of(it.name, it) }
}
