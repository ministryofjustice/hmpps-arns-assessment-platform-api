package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.sentencePlan

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.GroupCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandDispatcher
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus.QueryBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.sentencePlan.exceptions.AssessmentNotPlanException
import java.time.LocalDateTime
import java.util.UUID

@Service
class SentencePlanService(
  private val commandDispatcher: CommandDispatcher,
  private val queryBus: QueryBus,
) {
  fun newPeriodOfSupervision(assessmentUuid: UUID, userDetails: UserDetails) {
    val assessment = queryBus.dispatch(
      AssessmentVersionQuery(
        user = userDetails,
        assessmentIdentifier = UuidIdentifier(assessmentUuid),
      ),
    ) as AssessmentVersionQueryResult

    if (assessment.assessmentType != "SENTENCE_PLAN") throw AssessmentNotPlanException(assessmentUuid)

    val goalsCollection = assessment.collections.firstOrNull { it.name == "GOALS" }
      ?: throw IllegalStateException("Sentence plan must have goals collection")

    val now = LocalDateTime.now().toString()

    val goalsToRemove = goalsCollection.items
      .filter {
        val status = it.properties["status"] as SingleValue
        status.value == "ACTIVE" || status.value == "FUTURE"
      }

    val goalCommands = goalsToRemove.flatMap { goal ->
      val notesCollection = goal.collections.firstOrNull { it.name == "NOTES" }

      val createNotesCollection = if (notesCollection == null) {
        CreateCollectionCommand(
          name = "NOTES",
          parentCollectionItemUuid = goal.uuid,
          user = userDetails,
          assessmentUuid = assessmentUuid,
        )
      } else null

      val notesCollectionUuid = notesCollection?.uuid ?: createNotesCollection!!.collectionUuid

      listOfNotNull(
        UpdateCollectionItemPropertiesCommand(
          collectionItemUuid = goal.uuid,
          added = mapOf(
            "status" to SingleValue("REMOVED"),
            "status_date" to SingleValue(now),
          ),
          removed = emptyList(),
          user = userDetails,
          assessmentUuid = assessmentUuid,
        ),
        UpdateCollectionItemAnswersCommand(
          collectionItemUuid = goal.uuid,
          added = emptyMap(),
          removed = listOf("target_date"),
          user = userDetails,
          assessmentUuid = assessmentUuid,
        ),
        createNotesCollection,
        AddCollectionItemCommand(
          collectionUuid = notesCollectionUuid,
          answers = mapOf(
            "note" to SingleValue("Automatically removed as the previous supervision period has ended."),
            "created_by" to SingleValue("System"),
          ),
          properties = mapOf(
            "type" to SingleValue("REMOVED"),
            "created_at" to SingleValue(now),
          ),
          index = null,
          user = userDetails,
          assessmentUuid = assessmentUuid,
        ),
      )
    }


    val agreementCommands = assessment.collections
      .firstOrNull { it.name == "PLAN_AGREEMENTS" }
      ?.items?.map {
        RemoveCollectionItemCommand(
          collectionItemUuid = it.uuid,
          user = userDetails,
          assessmentUuid = assessmentUuid,
        )
      } ?: emptyList()

    GroupCommand(
      user = userDetails,
      assessmentUuid = assessmentUuid,
      commands = listOf(goalCommands, agreementCommands).flatten(),
      timeline = Timeline(
        type = "NEW_PERIOD_OF_SUPERVISION",
        data = mapOf(
          "Goals removed" to goalsToRemove.size,
        ),
      ),
    ).let {
      commandDispatcher.dispatch(listOf(it))
    }
  }
}