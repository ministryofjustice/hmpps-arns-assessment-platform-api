package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
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
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

abstract class AbstractCommandHandlerTest {
  // Stub service bundle and other mocks
  val services: CommandHandlerServiceBundle = mockk()
  val assessmentAggregate: AssessmentAggregate = mockk()
  val collection: Collection = mockk()
  val collectionItem: CollectionItem = mockk()

  // Basic mock data
  val assessment = AssessmentEntity(type = "TEST")
  val commandUser = UserDetails("FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  val user = UserDetailsEntity(1, UUID.randomUUID(), "FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  val timeline = Timeline(type = "test", data = mapOf("foo" to listOf("bar")))
  val assessmentState: AssessmentState = AssessmentState(
    AggregateEntity(
      assessment = assessment,
      data = assessmentAggregate,
    ),
  )

  // Abstract values to be implemented by concrete tests
  abstract val handler: KClass<out CommandHandler<out Command>>
  abstract val command: RequestableCommand
  abstract val expectedEvent: Event
  abstract val expectedResult: CommandResult

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  private fun getHandler() = handler.primaryConstructor!!.call(services)

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(getHandler().type).isEqualTo(command::class)
  }

  @Test
  fun `it handles the command`() {
    every { services.assessment.findBy(assessment.uuid) } returns assessment

    val handledEvent = slot<EventEntity<out Event>>()
    val persistedEvent = slot<EventEntity<out Event>>()
    val savedTimeline = slot<TimelineEntity>()
    val state: State = mockk()
    val stateForType: StateService.StateForType<AssessmentAggregate> = mockk()

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

    val result = getHandler().execute(command)

//    verify(exactly = assessmentFindByCallCount) { services.assessment.findBy(assessment.uuid) }
    verify(exactly = 1) { services.eventBus.handle(any<EventEntity<out Event>>()) }
//    verify(exactly = 1) { services.state.persist(state) }
//    verify(exactly = 1) { services.event.save(any<EventEntity<out Event>>()) }
    verify(exactly = 1) { services.userDetails.findOrCreate(commandUser) }

    assertThat(handledEvent.captured.assessment.uuid).isEqualTo(assessment.uuid)
    assertThat(handledEvent.captured.user.userId).isEqualTo(command.user.id)
    assertThat(handledEvent.captured.user.displayName).isEqualTo(command.user.name)
    assertThat(handledEvent.captured.user.authSource).isEqualTo(command.user.authSource)
    assertThat(handledEvent.captured.data).isEqualTo(expectedEvent)

    assertThat(handledEvent.captured).isEqualTo(persistedEvent.captured)

    assertThat(result).isEqualTo(expectedResult)
  }
}
