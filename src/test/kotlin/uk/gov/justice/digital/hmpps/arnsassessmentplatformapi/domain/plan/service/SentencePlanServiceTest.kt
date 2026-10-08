package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.plan.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateTimelineItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.RetryableCommandDispatcher
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.plan.exception.AssessmentNotPlanException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus.QueryBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID

class SentencePlanServiceTest {
  private val retryableCommandDispatcher: RetryableCommandDispatcher = mockk(relaxed = true)
  private val queryBus: QueryBus = mockk()
  private val service = SentencePlanService(retryableCommandDispatcher, queryBus)

  private val assessmentUuid = UUID.randomUUID()
  private val userDetails = UserDetails("user-1", "Test User")
  private val now = LocalDateTime.now()

  private fun goalItem(status: String, hasNotesCollection: Boolean = true) = CollectionItem(
    uuid = UUID.randomUUID(),
    createdAt = now,
    updatedAt = now,
    answers = mutableMapOf("target_date" to SingleValue("2026-06-01")),
    properties = mutableMapOf("status" to SingleValue(status)),
    collections = if (hasNotesCollection) {
      mutableListOf(
        Collection(
          uuid = UUID.randomUUID(),
          createdAt = now,
          updatedAt = now,
          name = "NOTES",
          items = mutableListOf(),
        ),
      )
    } else {
      mutableListOf()
    },
  )

  private fun agreementItem() = CollectionItem(
    uuid = UUID.randomUUID(),
    createdAt = now,
    updatedAt = now,
    answers = mutableMapOf(),
    properties = mutableMapOf("status" to SingleValue("AGREED")),
    collections = mutableListOf(),
  )

  private fun buildAssessment(
    type: String = "SENTENCE_PLAN",
    goals: List<CollectionItem> = emptyList(),
    agreements: List<CollectionItem> = emptyList(),
    properties: Map<String, SingleValue> = emptyMap(),
  ): AssessmentVersionQueryResult {
    val collections = mutableListOf(
      Collection(
        uuid = UUID.randomUUID(),
        createdAt = now,
        updatedAt = now,
        name = "GOALS",
        items = goals.toMutableList(),
      ),
    )

    if (agreements.isNotEmpty()) {
      collections.add(
        Collection(
          uuid = UUID.randomUUID(),
          createdAt = now,
          updatedAt = now,
          name = "PLAN_AGREEMENTS",
          items = agreements.toMutableList(),
        ),
      )
    }

    return AssessmentVersionQueryResult(
      assessmentUuid = assessmentUuid,
      aggregateUuid = UUID.randomUUID(),
      assessmentType = type,
      formVersion = "v1.0",
      createdAt = now,
      updatedAt = now,
      answers = emptyMap(),
      properties = properties,
      collections = collections,
      collaborators = emptySet(),
      identifiers = emptyMap(),
      assignedUser = null,
    )
  }

  @Nested
  inner class NewPeriodOfSupervision {
    @Test
    fun `should remove active and future goals and clear agreements`() {
      val activeGoal = goalItem("ACTIVE")
      val futureGoal = goalItem("FUTURE")
      val removedGoal = goalItem("REMOVED")
      val agreement = agreementItem()

      val assessment = buildAssessment(
        goals = listOf(activeGoal, futureGoal, removedGoal),
        agreements = listOf(agreement),
      )

      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.newPeriodOfSupervision(assessmentUuid, userDetails)

      val commandsSlot = slot<List<RequestableCommand>>()
      verify { retryableCommandDispatcher.dispatch(capture(commandsSlot)) }

      val subCommands = commandsSlot.captured

      val updatePropsCommands = subCommands.filterIsInstance<UpdateCollectionItemPropertiesCommand>()
      val updateAnswersCommands = subCommands.filterIsInstance<UpdateCollectionItemAnswersCommand>()
      val addItemCommands = subCommands.filterIsInstance<AddCollectionItemCommand>()
      val removeItemCommands = subCommands.filterIsInstance<RemoveCollectionItemCommand>()
      val timelineCommands = subCommands.filterIsInstance<CreateTimelineItemCommand>()

      Assertions.assertThat(updatePropsCommands).hasSize(2)
      Assertions.assertThat(updateAnswersCommands).hasSize(2)
      Assertions.assertThat(addItemCommands).hasSize(2)
      Assertions.assertThat(removeItemCommands).hasSize(1)
      Assertions.assertThat(timelineCommands).hasSize(1)

      Assertions.assertThat(updatePropsCommands).allMatch {
        it.added["status"] == SingleValue("REMOVED")
      }
      Assertions.assertThat(updateAnswersCommands).allMatch {
        it.removed == listOf("target_date")
      }
      Assertions.assertThat(removeItemCommands.first().collectionItemUuid.value).isEqualTo(agreement.uuid)

      Assertions.assertThat(timelineCommands.first().timeline.type).isEqualTo("NEW_PERIOD_OF_SUPERVISION")
      Assertions.assertThat(timelineCommands.first().timeline.data["Goals removed"]).isEqualTo(2)
    }

    @Test
    fun `should not include removed or achieved goals`() {
      val removedGoal = goalItem("REMOVED")
      val achievedGoal = goalItem("ACHIEVED")

      val assessment = buildAssessment(goals = listOf(removedGoal, achievedGoal))

      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.newPeriodOfSupervision(assessmentUuid, userDetails)

      val commandsSlot = slot<List<RequestableCommand>>()
      verify { retryableCommandDispatcher.dispatch(capture(commandsSlot)) }

      val subCommands = commandsSlot.captured

      Assertions.assertThat(subCommands.filterIsInstance<UpdateCollectionItemPropertiesCommand>()).isEmpty()
      Assertions.assertThat(subCommands.filterIsInstance<UpdateCollectionItemAnswersCommand>()).isEmpty()
      Assertions.assertThat(subCommands.filterIsInstance<AddCollectionItemCommand>()).isEmpty()
    }

    @Test
    fun `should throw when assessment is not a sentence plan`() {
      val assessment = buildAssessment(type = "OTHER_ASSESSMENT")

      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      assertThrows<AssessmentNotPlanException> {
        service.newPeriodOfSupervision(assessmentUuid, userDetails)
      }
    }

    @Test
    fun `should create NOTES collection and add note when goal has no NOTES collection`() {
      val goalWithoutNotes = goalItem("ACTIVE", hasNotesCollection = false)

      val assessment = buildAssessment(goals = listOf(goalWithoutNotes))

      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.newPeriodOfSupervision(assessmentUuid, userDetails)

      val commandsSlot = slot<List<RequestableCommand>>()
      verify { retryableCommandDispatcher.dispatch(capture(commandsSlot)) }

      val commands = commandsSlot.captured

      val createCollectionCommands = commands.filterIsInstance<CreateCollectionCommand>()
      val addItemCommands = commands.filterIsInstance<AddCollectionItemCommand>()

      Assertions.assertThat(createCollectionCommands).hasSize(1)
      Assertions.assertThat(createCollectionCommands.first().name).isEqualTo("NOTES")
      Assertions.assertThat(createCollectionCommands.first().parentCollectionItemUuid?.value).isEqualTo(goalWithoutNotes.uuid)
      Assertions.assertThat(addItemCommands).hasSize(1)
      Assertions.assertThat(commands).hasSize(5)
    }

    @Test
    fun `should use generic removal note`() {
      val activeGoal = goalItem("ACTIVE")

      val assessment = buildAssessment(goals = listOf(activeGoal))

      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.newPeriodOfSupervision(assessmentUuid, userDetails)

      val commandsSlot = slot<List<RequestableCommand>>()
      verify { retryableCommandDispatcher.dispatch(capture(commandsSlot)) }

      val addItemCommands = commandsSlot.captured.filterIsInstance<AddCollectionItemCommand>()
      val noteValue = addItemCommands.first().answers["note"] as SingleValue

      Assertions.assertThat(noteValue.value).isEqualTo("Automatically removed as the previous supervision period has ended.")
    }
  }
}
