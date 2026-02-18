package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.sentencePlan

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.GroupCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
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

    val goalsToRemove = goalsCollection.items
      .filter {
        val status = it.properties["status"] as SingleValue
        status.value == "ACTIVE" || status.value == "FUTURE"
      }
      .map {
        UpdateCollectionItemPropertiesCommand(
          collectionItemUuid = it.uuid,
          added = mapOf(
            "status" to SingleValue("REMOVED"),
            "status_date" to SingleValue(LocalDateTime.now().toString()),
          ),
          removed = emptyList(),
          user = userDetails,
          assessmentUuid = assessmentUuid,
        )
      }

    GroupCommand(
      user = userDetails,
      assessmentUuid = assessmentUuid,
      commands = listOf(goalsToRemove).flatten(),
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