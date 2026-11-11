package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateTimelineItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.RetryableCommandDispatcher
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.Pathway
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.exception.AssessmentNotStrengthsAndNeedsException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.exception.PathwayNotChangedException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus.QueryBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID

@Service
class StrengthsAndNeedsService(
  private val commandDispatcher: RetryableCommandDispatcher,
  private val queryBus: QueryBus,
) {
  fun startNewPathway(assessmentUuid: UUID, userDetails: UserDetails, previousPathway: Pathway, newPathway: Pathway) {
    if (previousPathway == newPathway) throw PathwayNotChangedException(assessmentUuid, pathway = previousPathway)

    val assessment = queryBus.dispatch(
      AssessmentVersionQuery(
        user = userDetails,
        assessmentIdentifier = UuidIdentifier(assessmentUuid),
      ),
    ) as AssessmentVersionQueryResult

    if (assessment.assessmentType != "STRENGTHS_AND_NEEDS") throw AssessmentNotStrengthsAndNeedsException(assessmentUuid)

    val removed = assessment.answers.keys.toList()

    val answerCommands = if (removed.isEmpty()) {
      emptyList()
    } else {
      listOf(
        UpdateAssessmentAnswersCommand(
          user = userDetails,
          assessmentUuid = assessmentUuid.toReference(),
          added = emptyMap(),
          removed = removed,
        ),
      )
    }

    val timelineCommands = listOf(
      CreateTimelineItemCommand(
        timestamp = LocalDateTime.now(),
        user = userDetails,
        assessmentUuid = assessmentUuid.toReference(),
        timeline = Timeline(
          type = "PATHWAY_CHANGE",
          data = mapOf(
            "Pathway change" to "$previousPathway to $newPathway",
          ),
        ),
      ),
    )

    commandDispatcher.dispatch(listOf(answerCommands, timelineCommands).flatten())
  }
}
