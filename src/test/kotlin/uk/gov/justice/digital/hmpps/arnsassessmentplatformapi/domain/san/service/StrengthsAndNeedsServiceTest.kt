package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateTimelineItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.RetryableCommandDispatcher
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus.QueryBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID

class StrengthsAndNeedsServiceTest {
  private val commandDispatcher: RetryableCommandDispatcher = mockk(relaxed = true)
  private val queryBus: QueryBus = mockk()
  private val service = StrengthsAndNeedsService(commandDispatcher, queryBus)

  private val assessmentUuid = UUID.randomUUID()
  private val userDetails = UserDetails("user-1", "Test User")
  private val now = LocalDateTime.now()

  private fun buildAssessment(
    type: String = "STRENGTHS_AND_NEEDS",
    answers: Map<String, Value> = emptyMap(),
  ) = AssessmentVersionQueryResult(
    assessmentUuid = assessmentUuid,
    aggregateUuid = UUID.randomUUID(),
    assessmentType = type,
    formVersion = "v1.0",
    createdAt = now,
    updatedAt = now,
    answers = answers,
    properties = emptyMap(),
    collections = emptyList(),
    collaborators = emptySet(),
    identifiers = emptyMap(),
    assignedUser = null,
  )

  private fun dispatchedCommands(): List<RequestableCommand> {
    val commandsSlot = slot<List<RequestableCommand>>()
    verify { commandDispatcher.dispatch(capture(commandsSlot)) }
    return commandsSlot.captured
  }

  @Nested
  inner class StartNewPathway {
    @Test
    fun `should remove every answer on the assessment in a single command`() {
      val assessment = buildAssessment(
        answers = mapOf(
          "accommodation_type" to SingleValue("SETTLED"),
          "drug_use" to SingleValue("NO"),
        ),
      )
      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.startNewPathway(assessmentUuid, userDetails, "COMMUNITY", "CUSTODY")

      val answerCommands = dispatchedCommands().filterIsInstance<UpdateAssessmentAnswersCommand>()
      assertThat(answerCommands).hasSize(1)
      assertThat(answerCommands.first().removed).containsExactlyInAnyOrder("accommodation_type", "drug_use")
      assertThat(answerCommands.first().added).isEmpty()
    }

    @Test
    fun `should add a timeline entry recording the pathway change`() {
      val assessment = buildAssessment(
        answers = mapOf(
          "accommodation_type" to SingleValue("SETTLED"),
          "drug_use" to SingleValue("NO"),
        ),
      )
      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.startNewPathway(assessmentUuid, userDetails, "COMMUNITY", "CUSTODY")

      val timelineCommands = dispatchedCommands().filterIsInstance<CreateTimelineItemCommand>()
      assertThat(timelineCommands).hasSize(1)
      assertThat(timelineCommands.first().timeline.type).isEqualTo("PATHWAY_CHANGE")
      assertThat(timelineCommands.first().timeline.data["Pathway change"]).isEqualTo("COMMUNITY to CUSTODY")
    }

    @Test
    fun `should not clear answers if there are no answers on the assessment`() {
      val assessment = buildAssessment(answers = emptyMap())
      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.startNewPathway(assessmentUuid, userDetails, "COMMUNITY", "CUSTODY")

      val answerCommands = dispatchedCommands().filterIsInstance<UpdateAssessmentAnswersCommand>()
      assertThat(answerCommands).hasSize(1)
      assertThat(answerCommands.first().removed).isEmpty()
      assertThat(answerCommands.first().added).isEmpty()
    }
  }
}
