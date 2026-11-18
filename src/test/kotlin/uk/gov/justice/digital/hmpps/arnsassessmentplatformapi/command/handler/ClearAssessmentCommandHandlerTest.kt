package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ClearAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentClearedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.util.UUID

class ClearAssessmentCommandHandlerTest : AbstractCommandHandlerTest<ClearAssessmentCommand>() {
  @BeforeEach
  fun stubAssessmentState() {
    every {
      persistenceContext.getAggregateState(assessment, AssessmentAggregate::class, null)
    } returns AssessmentState(
      AggregateEntity(
        assessment = assessment,
        updatedAt = now,
        eventsFrom = now,
        eventsTo = now,
        data = AssessmentAggregate().apply {
          properties["accommodation_section_complete"] = SingleValue("YES")
          properties["finance_section_complete"] = SingleValue("YES")
          properties["section_complete_extra"] = SingleValue("x")
          properties["oasys_equivalent"] = SingleValue("{}")
          properties["is_user_submitted_accommodation"] = SingleValue("YES")
          collections.add(
            Collection(
              uuid = UUID.randomUUID(),
              createdAt = now,
              updatedAt = now,
              name = "OFFENCE_ANALYSIS_VICTIM",
              items = mutableListOf(),
            ),
          )
        },
      ),
    )
  }

  override val handler = ClearAssessmentCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<ClearAssessmentCommand>(
      name = "It handles the command",
    ).apply {
      command = ClearAssessmentCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        properties = listOf("*_section_complete"),
        timeline = timeline,
      )
      expectedEvent = AssessmentClearedEvent(
        properties = listOf("accommodation_section_complete", "finance_section_complete"),
        collections = emptyList(),
      )
      expectedResult = CommandSuccessCommandResult()
    },
    Scenario.Executes<ClearAssessmentCommand>(
      name = "matches a prefix wildcard",
    ).apply {
      command = ClearAssessmentCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        properties = listOf("is_user_submitted_*"),
        timeline = timeline,
      )
      expectedEvent = AssessmentClearedEvent(
        properties = listOf("is_user_submitted_accommodation"),
        collections = emptyList(),
      )
      expectedResult = CommandSuccessCommandResult()
    },
    Scenario.Executes<ClearAssessmentCommand>(
      name = "matches an exact property name",
    ).apply {
      command = ClearAssessmentCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        properties = listOf("finance_section_complete"),
        timeline = timeline,
      )
      expectedEvent = AssessmentClearedEvent(
        properties = listOf("finance_section_complete"),
        collections = emptyList(),
      )
      expectedResult = CommandSuccessCommandResult()
    },
    Scenario.Executes<ClearAssessmentCommand>(
      name = "returns no properties when nothing matches",
    ).apply {
      command = ClearAssessmentCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        properties = listOf("*_nope"),
        timeline = timeline,
      )
      expectedEvent = AssessmentClearedEvent(
        properties = emptyList(),
        collections = emptyList(),
      )
      expectedResult = CommandSuccessCommandResult()
    },
    Scenario.Executes<ClearAssessmentCommand>(
      name = "treats dots in patterns literally",
    ).apply {
      command = ClearAssessmentCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        properties = listOf("accommodation.section_complete"),
        timeline = timeline,
      )
      expectedEvent = AssessmentClearedEvent(
        properties = emptyList(),
        collections = emptyList(),
      )
      expectedResult = CommandSuccessCommandResult()
    },
    Scenario.Executes<ClearAssessmentCommand>(
      name = "keeps only collections that exist on the assessment",
    ).apply {
      command = ClearAssessmentCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        properties = emptyList(),
        collections = listOf("OFFENCE_ANALYSIS_VICTIM", "NOT_A_COLLECTION"),
        timeline = timeline,
      )
      expectedEvent = AssessmentClearedEvent(
        properties = emptyList(),
        collections = listOf("OFFENCE_ANALYSIS_VICTIM"),
      )
      expectedResult = CommandSuccessCommandResult()
    },
  )
}
