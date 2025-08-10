package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateFormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.FormVersionUpdated
import java.util.UUID

@Service
class AssessmentService(
  val assessmentRepository: AssessmentRepository,
  val eventRepository: EventRepository,
  private val aggregateService: AggregateService,
) : CommandExecutor {
  fun fetchAssessment(assessmentUuid: UUID): AssessmentEntity = assessmentRepository.findByUuid(assessmentUuid)
    ?: throw Error("AssessmentNotFound")

  override fun executeCommands(request: CommandRequest) {
    val assessment = fetchAssessment(request.assessmentUuid)

    val events: List<EventEntity> = request.commands.mapNotNull { command ->
      when (command) {
        is UpdateAnswers -> {
          EventEntity.from(
            assessment,
            request.user,
            AnswersUpdated(
              added = command.added,
              removed = command.removed,
            ),
          )
        }

        is UpdateFormVersion -> {
          EventEntity.from(
            assessment,
            request.user,
            FormVersionUpdated(
              version = command.version,
            ),
          )
        }

        else -> null
      }
    }

    if (events.isNotEmpty()) {
      eventRepository.saveAll(events)
      events.forEach {
        aggregateService.findAggregatesUpdatingOnEvent(it.data)
          .forEach { aggregateType -> aggregateService.updateAggregate(assessment, aggregateType, events) }
      }
    }
  }
}
