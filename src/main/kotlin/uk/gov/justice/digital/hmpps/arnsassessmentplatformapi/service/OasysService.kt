package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.AddOasysEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.OasysEventAdded

@Service
class OasysService(
  val assessmentService: AssessmentService,
  val eventRepository: EventRepository,
  private val aggregateService: AggregateService,
) : CommandExecutor {

  override fun executeCommands(request: CommandRequest) {
    val assessment = assessmentService.fetchAssessment(request.assessmentUuid)

    val events: List<EventEntity> = request.commands.mapNotNull { command ->
      when (command) {
        is AddOasysEvent -> {
          EventEntity.from(
            assessment,
            request.user,
            OasysEventAdded(
              tag = command.tag,
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
