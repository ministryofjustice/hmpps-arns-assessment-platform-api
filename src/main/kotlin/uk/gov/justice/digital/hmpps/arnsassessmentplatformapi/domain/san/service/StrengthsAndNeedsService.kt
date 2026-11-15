package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateTimelineItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
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

    val collectionItemCommands = assessment.collections.flatMap { collection ->
      collection.items.map { item ->
        RemoveCollectionItemCommand(
          collectionItemUuid = item.uuid.toReference(),
          user = userDetails,
          assessmentUuid = assessmentUuid.toReference(),
        )
      }
    }

    val removedProperties = assessment.properties.keys.filter(::isClearedOnPathwayChange)

    val propertyCommands = if (removedProperties.isEmpty()) {
      emptyList()
    } else {
      listOf(
        UpdateAssessmentPropertiesCommand(
          user = userDetails,
          assessmentUuid = assessmentUuid.toReference(),
          added = emptyMap(),
          removed = removedProperties,
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

    commandDispatcher.dispatch(listOf(answerCommands, collectionItemCommands, propertyCommands, timelineCommands).flatten())
  }

  /**
   * Properties derived from the answers, so they are stale once the answers are cleared:
   * the OASys mapping, section completion statuses and per-step "user submitted" flags.
   * All other properties (e.g. SAN_ASSESSMENT_TYPE, OASYS_*) are kept.
   */
  private fun isClearedOnPathwayChange(key: String) = key == OASYS_EQUIVALENT ||
    key.endsWith(SECTION_COMPLETE_SUFFIX) ||
    key.startsWith(USER_SUBMITTED_PREFIX)

  private companion object {
    const val OASYS_EQUIVALENT = "oasys_equivalent"
    const val SECTION_COMPLETE_SUFFIX = "_section_complete"
    const val USER_SUBMITTED_PREFIX = "is_user_submitted_"
  }
}
