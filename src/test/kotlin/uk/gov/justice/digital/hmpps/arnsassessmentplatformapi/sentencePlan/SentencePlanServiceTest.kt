package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.sentencePlan

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.GroupCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandDispatcher
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus.QueryBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.sentencePlan.exceptions.AssessmentNotPlanException
import java.time.LocalDateTime
import java.util.UUID

class SentencePlanServiceTest {
  private val commandDispatcher: CommandDispatcher = mockk(relaxed = true)
  private val queryBus: QueryBus = mockk()
  private val service = SentencePlanService(commandDispatcher, queryBus)

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

      val commandsSlot = slot<List<GroupCommand>>()
      verify { commandDispatcher.dispatch(capture(commandsSlot)) }

      val groupCommand = commandsSlot.captured.first()
      val subCommands = groupCommand.commands

      val updatePropsCommands = subCommands.filterIsInstance<UpdateCollectionItemPropertiesCommand>()
      val updateAnswersCommands = subCommands.filterIsInstance<UpdateCollectionItemAnswersCommand>()
      val addItemCommands = subCommands.filterIsInstance<AddCollectionItemCommand>()
      val removeItemCommands = subCommands.filterIsInstance<RemoveCollectionItemCommand>()

      assertThat(updatePropsCommands).hasSize(2)
      assertThat(updateAnswersCommands).hasSize(2)
      assertThat(addItemCommands).hasSize(2)
      assertThat(removeItemCommands).hasSize(1)

      assertThat(updatePropsCommands).allMatch {
        it.added["status"] == SingleValue("REMOVED")
      }
      assertThat(updateAnswersCommands).allMatch {
        it.removed == listOf("target_date")
      }
      assertThat(removeItemCommands.first().collectionItemUuid).isEqualTo(agreement.uuid)

      assertThat(groupCommand.timeline?.type).isEqualTo("NEW_PERIOD_OF_SUPERVISION")
      assertThat(groupCommand.timeline?.data?.get("Goals removed")).isEqualTo(2)
    }

    @Test
    fun `should not include removed or achieved goals`() {
      val removedGoal = goalItem("REMOVED")
      val achievedGoal = goalItem("ACHIEVED")

      val assessment = buildAssessment(goals = listOf(removedGoal, achievedGoal))

      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.newPeriodOfSupervision(assessmentUuid, userDetails)

      val commandsSlot = slot<List<GroupCommand>>()
      verify { commandDispatcher.dispatch(capture(commandsSlot)) }

      val groupCommand = commandsSlot.captured.first()
      val subCommands = groupCommand.commands

      assertThat(subCommands.filterIsInstance<UpdateCollectionItemPropertiesCommand>()).isEmpty()
      assertThat(subCommands.filterIsInstance<UpdateCollectionItemAnswersCommand>()).isEmpty()
      assertThat(subCommands.filterIsInstance<AddCollectionItemCommand>()).isEmpty()
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

      val commandsSlot = slot<List<GroupCommand>>()
      verify { commandDispatcher.dispatch(capture(commandsSlot)) }

      val groupCommand = commandsSlot.captured.first()

      val createCollectionCommands = groupCommand.commands.filterIsInstance<CreateCollectionCommand>()
      val addItemCommands = groupCommand.commands.filterIsInstance<AddCollectionItemCommand>()

      assertThat(createCollectionCommands).hasSize(1)
      assertThat(createCollectionCommands.first().name).isEqualTo("NOTES")
      assertThat(createCollectionCommands.first().parentCollectionItemUuid).isEqualTo(goalWithoutNotes.uuid)
      assertThat(addItemCommands).hasSize(1)
      assertThat(groupCommand.commands).hasSize(4)

    }

    @Test
    fun `should include forename in removal note when SUBJECT_FORENAME property exists`() {
      val activeGoal = goalItem("ACTIVE")

      val assessment = buildAssessment(
        goals = listOf(activeGoal),
        properties = mapOf("SUBJECT_FORENAME" to SingleValue("John")),
      )

      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.newPeriodOfSupervision(assessmentUuid, userDetails)

      val commandsSlot = slot<List<GroupCommand>>()
      verify { commandDispatcher.dispatch(capture(commandsSlot)) }

      val addItemCommands = commandsSlot.captured.first().commands.filterIsInstance<AddCollectionItemCommand>()
      val noteValue = addItemCommands.first().answers["note"] as SingleValue

      assertThat(noteValue.value).isEqualTo("Automatically removed as John's previous supervision period has ended.")
    }

    @Test
    fun `should use generic removal note when SUBJECT_FORENAME property is missing`() {
      val activeGoal = goalItem("ACTIVE")

      val assessment = buildAssessment(goals = listOf(activeGoal))

      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.newPeriodOfSupervision(assessmentUuid, userDetails)

      val commandsSlot = slot<List<GroupCommand>>()
      verify { commandDispatcher.dispatch(capture(commandsSlot)) }

      val addItemCommands = commandsSlot.captured.first().commands.filterIsInstance<AddCollectionItemCommand>()
      val noteValue = addItemCommands.first().answers["note"] as SingleValue

      assertThat(noteValue.value).isEqualTo("Automatically removed as the previous supervision period has ended.")
    }
  }


}