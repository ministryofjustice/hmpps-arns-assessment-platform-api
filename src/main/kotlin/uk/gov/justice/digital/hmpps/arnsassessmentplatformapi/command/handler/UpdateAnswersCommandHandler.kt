package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class UpdateAnswersCommandHandler(
  private val assessmentService: AssessmentService,
  private val aggregateService: AggregateService,
  private val eventBus: EventBus,
) : CommandHandler<UpdateAnswersCommand> {
  override val type = UpdateAnswersCommand::class
  override fun handle(command: UpdateAnswersCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findByUuid(assessmentUuid),
        data = AssessmentAnswersUpdatedEvent(
          added = added,
          removed = removed,
        ),
      )
    }
    val state = aggregateService.fetchLatestState(event.assessment)
    eventBus.handle(event, state)
    aggregateService.persistState(state)

    return CommandSuccessCommandResult()
  }
}
