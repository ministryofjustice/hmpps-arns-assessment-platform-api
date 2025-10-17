package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

class RollbackAnswersCommandHandlerTest {
  val assessmentService: AssessmentService = mockk()
  val aggregateService: AggregateService = mockk()
  val eventBus: EventBus = mockk()
  val clock: Clock = mockk(relaxed = true)

  val handler = RollbackAnswersCommandHandler(
    assessmentService = assessmentService,
    aggregateService = aggregateService,
    eventBus = eventBus,
    clock = clock,
  )

  @BeforeEach
  fun setUp() {
    every { clock.instant() } returns Instant.parse("2025-01-02T12:00:00Z")
  }

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(handler.type).isEqualTo(RollbackAnswersCommand::class)
  }

  @Test
  fun `it handles the RollbackAnswers command`() {
    val command = RollbackAnswersCommand(
      user = User("FOO_USER", "Foo User"),
      assessmentUuid = UUID.randomUUID(),
      pointInTime = LocalDateTime.parse("2025-01-01T12:00:00"),
    )

    val assessment = AssessmentEntity(uuid = command.assessmentUuid)

    every { assessmentService.findByUuid(command.assessmentUuid) } returns assessment
    // current version
    every {
      aggregateService.fetchAggregateForExactPointInTime(
        assessment,
        AssessmentVersionAggregate.aggregateType,
        LocalDateTime.parse("2025-01-02T12:00:00"),
      )
    } returns AggregateEntity(
      assessment = assessment,
      data = AssessmentVersionAggregate(
        answers = mutableMapOf(
          "foo" to listOf("updated_foo_value"),
          "baz" to listOf("baz_value"),
          "unchanged" to listOf("unchanged_value"),
        ),
        deletedAnswers = mutableMapOf(
          "bar" to listOf("bar_value"),
        ),
      ),
    )
    // previous version
    every {
      aggregateService.fetchAggregateForExactPointInTime(
        assessment,
        AssessmentVersionAggregate.aggregateType,
        LocalDateTime.parse("2025-01-01T12:00:00"),
      )
    } returns AggregateEntity(
      assessment = assessment,
      data = AssessmentVersionAggregate(
        answers = mutableMapOf(
          "foo" to listOf("foo_value"),
          "bar" to listOf("bar_value"),
          "unchanged" to listOf("unchanged_value"),
        ),
      ),
    )

    val event = slot<EventEntity>()
    every { eventBus.add(capture(event)) } just Runs

    handler.execute(command)
    verify(exactly = 1) { eventBus.add(any<EventEntity>()) }

    assertThat(event.captured.assessment.uuid).isEqualTo(command.assessmentUuid)
    assertThat(event.captured.user).isEqualTo(command.user)
    val eventData = assertIs<AnswersRolledBackEvent>(event.captured.data)
    assertThat(eventData.added).isEqualTo(
      mapOf(
        "foo" to listOf("foo_value"),
        "bar" to listOf("bar_value"),
      ),
    )
    assertThat(eventData.removed).isEqualTo(listOf("baz"))
    assertThat(eventData.rolledBackTo).isEqualTo(command.pointInTime)
  }

  @Test
  fun `it creates the previous version if it does not exist`() {
    val command = RollbackAnswersCommand(
      user = User("FOO_USER", "Foo User"),
      assessmentUuid = UUID.randomUUID(),
      pointInTime = LocalDateTime.parse("2025-01-01T12:00:00"),
    )

    val assessment = AssessmentEntity(uuid = command.assessmentUuid)

    every { assessmentService.findByUuid(command.assessmentUuid) } returns assessment
    // current version
    every {
      aggregateService.fetchAggregateForExactPointInTime(
        assessment,
        AssessmentVersionAggregate.aggregateType,
        LocalDateTime.parse("2025-01-02T12:00:00"),
      )
    } returns AggregateEntity(
      assessment = assessment,
      data = AssessmentVersionAggregate(
        answers = mutableMapOf(
          "foo" to listOf("updated_foo_value"),
          "baz" to listOf("baz_value"),
        ),
        deletedAnswers = mutableMapOf(
          "bar" to listOf("bar_value"),
        ),
      ),
    )
    // previous version
    every {
      aggregateService.fetchAggregateForExactPointInTime(
        assessment,
        AssessmentVersionAggregate.aggregateType,
        LocalDateTime.parse("2025-01-01T12:00:00"),
      )
    } returns null
    every {
      aggregateService.createAggregateForPointInTime(
        assessment,
        AssessmentVersionAggregate.aggregateType,
        LocalDateTime.parse("2025-01-01T12:00:00"),
      )
    } returns AggregateEntity(
      assessment = assessment,
      data = AssessmentVersionAggregate(
        answers = mutableMapOf(
          "foo" to listOf("foo_value"),
          "bar" to listOf("bar_value"),
        ),
      ),
    )

    val event = slot<EventEntity>()
    every { eventBus.add(capture(event)) } just Runs

    handler.execute(command)
    verify(exactly = 1) { eventBus.add(any<EventEntity>()) }

    assertThat(event.captured.assessment.uuid).isEqualTo(command.assessmentUuid)
    assertThat(event.captured.user).isEqualTo(command.user)
    val eventData = assertIs<AnswersRolledBackEvent>(event.captured.data)
    assertThat(eventData.added).isEqualTo(
      mapOf(
        "foo" to listOf("foo_value"),
        "bar" to listOf("bar_value"),
      ),
    )
    assertThat(eventData.removed).isEqualTo(listOf("baz"))
    assertThat(eventData.rolledBackTo).isEqualTo(command.pointInTime)
  }

  @Test
  fun `it creates the current version if it does not exist`() {
    val command = RollbackAnswersCommand(
      user = User("FOO_USER", "Foo User"),
      assessmentUuid = UUID.randomUUID(),
      pointInTime = LocalDateTime.parse("2025-01-01T12:00:00"),
    )

    val assessment = AssessmentEntity(uuid = command.assessmentUuid)

    every { assessmentService.findByUuid(command.assessmentUuid) } returns assessment
    // current version
    every {
      aggregateService.fetchAggregateForExactPointInTime(
        assessment,
        AssessmentVersionAggregate.aggregateType,
        LocalDateTime.parse("2025-01-02T12:00:00"),
      )
    } returns null
    every {
      aggregateService.createAggregateForPointInTime(
        assessment,
        AssessmentVersionAggregate.aggregateType,
        LocalDateTime.parse("2025-01-02T12:00:00"),
      )
    } returns AggregateEntity(
      assessment = assessment,
      data = AssessmentVersionAggregate(
        answers = mutableMapOf(
          "foo" to listOf("updated_foo_value"),
          "baz" to listOf("baz_value"),
        ),
        deletedAnswers = mutableMapOf(
          "bar" to listOf("bar_value"),
        ),
      ),
    )
    // previous version
    every {
      aggregateService.fetchAggregateForExactPointInTime(
        assessment,
        AssessmentVersionAggregate.aggregateType,
        LocalDateTime.parse("2025-01-01T12:00:00"),
      )
    } returns AggregateEntity(
      assessment = assessment,
      data = AssessmentVersionAggregate(
        answers = mutableMapOf(
          "foo" to listOf("foo_value"),
          "bar" to listOf("bar_value"),
        ),
      ),
    )

    val event = slot<EventEntity>()
    every { eventBus.add(capture(event)) } just Runs

    handler.execute(command)
    verify(exactly = 1) { eventBus.add(any<EventEntity>()) }

    assertThat(event.captured.assessment.uuid).isEqualTo(command.assessmentUuid)
    assertThat(event.captured.user).isEqualTo(command.user)
    val eventData = assertIs<AnswersRolledBackEvent>(event.captured.data)
    assertThat(eventData.added).isEqualTo(
      mapOf(
        "foo" to listOf("foo_value"),
        "bar" to listOf("bar_value"),
      ),
    )
    assertThat(eventData.removed).isEqualTo(listOf("baz"))
    assertThat(eventData.rolledBackTo).isEqualTo(command.pointInTime)
  }
}
