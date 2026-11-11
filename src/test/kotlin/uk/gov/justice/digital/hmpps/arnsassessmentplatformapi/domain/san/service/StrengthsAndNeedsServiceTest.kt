package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.CollectionsView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateTimelineItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.RetryableCommandDispatcher
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.Pathway
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.exception.AssessmentNotStrengthsAndNeedsException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.exception.PathwayNotChangedException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.MultiValue
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
    collections: CollectionsView = emptyList(),
  ) = AssessmentVersionQueryResult(
    assessmentUuid = assessmentUuid,
    aggregateUuid = UUID.randomUUID(),
    assessmentType = type,
    formVersion = "v1.0",
    createdAt = now,
    updatedAt = now,
    answers = answers,
    properties = emptyMap(),
    collections = collections,
    collaborators = emptySet(),
    identifiers = emptyMap(),
    assignedUser = null,
  )

  private fun victimItem() = CollectionItem(
    uuid = UUID.randomUUID(),
    createdAt = now,
    updatedAt = now,
    answers = mutableMapOf(
      "offence_analysis_victim_relationship" to SingleValue("STRANGER"),
      "offence_analysis_victim_age" to SingleValue("AGE_50_TO_64_YEARS"),
      "offence_analysis_victim_sex" to SingleValue("MALE"),
      "offence_analysis_victim_race" to SingleValue("WHITE_ENGLISH_WELSH_SCOTTISH_NORTHERN_IRISH_OR_BRITISH"),
    ),
    properties = mutableMapOf(),
    collections = mutableListOf(),
  )

  private fun victimsCollection(vararg items: CollectionItem) = Collection(
    uuid = UUID.randomUUID(),
    createdAt = now,
    updatedAt = now,
    name = "OFFENCE_ANALYSIS_VICTIM",
    items = items.toMutableList(),
  )

  private fun dispatchedCommands(): List<RequestableCommand> {
    val commandsSlot = slot<List<RequestableCommand>>()
    verify { commandDispatcher.dispatch(capture(commandsSlot)) }
    return commandsSlot.captured
  }

  @Nested
  inner class StartNewPathwayTest {

    @Test
    fun `should remove every answer on the assessment in a single command`() {
      val assessment = buildAssessment(
        answers = mapOf(
          "accommodation_type" to SingleValue("SETTLED"),
          "drug_use" to SingleValue("NO"),
          "finance_income" to MultiValue(listOf("EMPLOYMENT", "FAMILY_OR_FRIENDS")),
        ),
        collections = listOf(
          victimsCollection(victimItem()),
        ),
      )
      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.startNewPathway(assessmentUuid, userDetails, Pathway.COMMUNITY, Pathway.CUSTODY)

      val answerCommands = dispatchedCommands().filterIsInstance<UpdateAssessmentAnswersCommand>()
      assertThat(answerCommands).hasSize(1)
      assertThat(answerCommands.first().removed).containsExactlyInAnyOrder("accommodation_type", "drug_use", "finance_income")
      assertThat(answerCommands.first().added).isEmpty()
    }

    @Test
    fun `should remove every collection item on the assessment`() {
      val victim1 = victimItem()
      val victim2 = victimItem()
      val assessment = buildAssessment(
        collections = listOf(
          victimsCollection(victim1, victim2),
        ),
      )
      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.startNewPathway(assessmentUuid, userDetails, Pathway.COMMUNITY, Pathway.CUSTODY)

      val removedUuids = dispatchedCommands()
        .filterIsInstance<RemoveCollectionItemCommand>()
        .map { it.collectionItemUuid.value }
      assertThat(removedUuids).containsExactlyInAnyOrder(victim1.uuid, victim2.uuid)
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

      service.startNewPathway(assessmentUuid, userDetails, Pathway.COMMUNITY, Pathway.CUSTODY)

      val timelineCommands = dispatchedCommands().filterIsInstance<CreateTimelineItemCommand>()
      assertThat(timelineCommands).hasSize(1)
      assertThat(timelineCommands.first().timeline.type).isEqualTo("PATHWAY_CHANGE")
      assertThat(timelineCommands.first().timeline.data["Pathway change"]).isEqualTo("COMMUNITY to CUSTODY")
    }

    @Test
    fun `should not clear answers if there are no answers on the assessment`() {
      val assessment = buildAssessment(answers = emptyMap())
      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      service.startNewPathway(assessmentUuid, userDetails, Pathway.COMMUNITY, Pathway.CUSTODY)

      val commands = dispatchedCommands()

      assertThat(commands.filterIsInstance<UpdateAssessmentAnswersCommand>()).isEmpty()
      assertThat(commands.filterIsInstance<RemoveCollectionItemCommand>()).isEmpty()
      assertThat(commands.filterIsInstance<CreateTimelineItemCommand>()).hasSize(1)
    }

    @Test
    fun `should throw an exception if the assessment is not a strengths and needs assessment`() {
      val assessment = buildAssessment(type = "SENTENCE_PLAN")
      every { queryBus.dispatch(any<AssessmentVersionQuery>()) } returns assessment

      assertThrows<AssessmentNotStrengthsAndNeedsException> {
        service.startNewPathway(assessmentUuid, userDetails, Pathway.COMMUNITY, Pathway.CUSTODY)
      }
      verify(exactly = 0) { commandDispatcher.dispatch(any()) }
    }

    @Test
    fun `should throw and not clear answers when the pathway has not changed`() {
      assertThrows<PathwayNotChangedException> {
        service.startNewPathway(assessmentUuid, userDetails, Pathway.COMMUNITY, Pathway.COMMUNITY)
      }
      verify(exactly = 0) { commandDispatcher.dispatch(any()) }
      verify(exactly = 0) { queryBus.dispatch(any<AssessmentVersionQuery>()) }
    }
  }
}
