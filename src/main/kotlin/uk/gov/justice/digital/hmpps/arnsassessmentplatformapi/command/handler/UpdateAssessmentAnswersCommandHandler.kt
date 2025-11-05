package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService

@Component
class UpdateAssessmentAnswersCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
) : CommandHandler<UpdateAssessmentAnswersCommand> {
  override val type = UpdateAssessmentAnswersCommand::class
  override fun handle(command: UpdateAssessmentAnswersCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findByUuid(assessmentUuid),
        data = AssessmentAnswersUpdatedEvent(
          added = added,
          removed = removed,
        ),
      )
    }.run(eventService::save)

    assessmentService.fetchLatestState(event.assessment)
      .let { eventBus.handle(event, it) }
      .let { assessmentService.persist(it) }

    return CommandSuccessCommandResult()
  }
}
