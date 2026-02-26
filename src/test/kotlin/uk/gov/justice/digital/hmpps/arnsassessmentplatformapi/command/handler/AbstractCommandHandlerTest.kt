package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

sealed interface Scenario<C : RequestableCommand> {
  val name: String
  var setupMocks: () -> Unit

  class Executes<C : RequestableCommand>(
    override val name: String,
  ) : Scenario<C> {
    lateinit var command: C
    override var setupMocks: () -> Unit = {}
    lateinit var expectedEvent: Event
    lateinit var expectedResult: CommandResult
  }

  class Throws<C : RequestableCommand, T : Throwable>(
    override val name: String,
  ) : Scenario<C> {
    lateinit var command: C
    override var setupMocks: () -> Unit = {}
    lateinit var expectedException: KClass<out T>
  }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractCommandHandlerTest<C : RequestableCommand> {
  val now: LocalDateTime = LocalDateTime.now()
  val clock: Clock = mockk()

  abstract val scenarios: List<Scenario<C>>
  abstract val handler: KClass<out CommandHandler<C>>

  // Stub service bundle and other mocks
  val services: CommandHandlerServiceBundle = mockk()
  val assessmentAggregate: AssessmentAggregate = mockk()
  val collection: Collection = mockk()
  val collectionItem: CollectionItem = mockk()

  // Basic mock data
  val assessment = AssessmentEntity(
    type = "TEST",
    createdAt = now,
  )
  val commandUser = UserDetails("FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  val user = UserDetailsEntity(1, UUID.randomUUID(), "FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  val timeline = Timeline(type = "test", data = mapOf("foo" to listOf("bar")))
  val assessmentState: AssessmentState = AssessmentState(
    AggregateEntity(
      assessment = assessment,
      data = assessmentAggregate,
      updatedAt = now,
      eventsFrom = now,
      eventsTo = now,
    ),
  )

  @BeforeAll
  fun init() {
  }

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { clock.now() } returns now
  }

  private fun getHandler() = handler.primaryConstructor!!.call(services)

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(getHandler())
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scenarioProvider")
  fun runScenario(
    @Suppress("UNUSED_PARAMETER") name: String,
    scenario: Scenario<C>,
  ) {
    val handledEvent = slot<EventEntity<out Event>>()
    val persistedEvent = slot<EventEntity<out Event>>()
    val savedTimeline = slot<TimelineEntity>()
    val state: State = mockk()
    val stateForType: StateService.StateForType<AssessmentAggregate> = mockk()

    every { services.assessment.findBy(assessment.uuid) } returns assessment
    every { services.eventBus.handle(capture(handledEvent)) } returns state
    every { services.state.persist(state) } just Runs
    every { services.state.stateForType(AssessmentAggregate::class) } returns stateForType
    every { stateForType.fetchOrCreateLatestState(assessment) } returns assessmentState
    every { services.event.save(capture(persistedEvent)) } answers { firstArg() }
    every { services.userDetails.findOrCreate(commandUser) } returns user
    every { state[AssessmentAggregate::class] } returns assessmentState
    every { collection.name } returns "TEST_COLLECTION_NAME"
    every { collection.findItem(any()) } returns collectionItem
    every { collection.items } returns mutableListOf(collectionItem)
    every { assessmentAggregate.getCollection(any()) } returns collection
    every { assessmentAggregate.getCollectionWithItem(any()) } returns collection
    every { assessmentAggregate.getCollectionItem(any()) } returns collectionItem
    every { services.timeline.save(capture(savedTimeline)) } answers { firstArg() }
    every { services.clock } returns clock

    scenario.setupMocks()

    when (scenario) {
      is Scenario.Executes<C> -> {
        val result = getHandler().execute(scenario.command)

        verify(exactly = 1) { services.eventBus.handle(any<EventEntity<out Event>>()) }
        verify(exactly = 1) { services.userDetails.findOrCreate(commandUser) }

        assertThat(handledEvent.captured.assessment.uuid).isEqualTo(assessment.uuid)
        assertThat(handledEvent.captured.user.userId).isEqualTo(scenario.command.user.id)
        assertThat(handledEvent.captured.user.displayName).isEqualTo(scenario.command.user.name)
        assertThat(handledEvent.captured.user.authSource).isEqualTo(scenario.command.user.authSource)
        assertThat(handledEvent.captured.data).isEqualTo(scenario.expectedEvent)

        assertThat(handledEvent.captured).isEqualTo(persistedEvent.captured)

        assertThat(result).isEqualTo(scenario.expectedResult)
      }

      is Scenario.Throws<C, *> -> {
        assertThrows(scenario.expectedException.java) {
          getHandler().handle(scenario.command)
        }
      }
    }
  }

  fun scenarioProvider(): List<Arguments> = scenarios.map { Arguments.of(it.name, it) }
}
