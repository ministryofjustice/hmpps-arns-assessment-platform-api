package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class UpdateAssessmentAnswersCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
) : CommandHandler<UpdateAssessmentAnswersCommand> {
  override val type = UpdateAssessmentAnswersCommand::class

  override fun handle(command: UpdateAssessmentAnswersCommand): CommandSuccessCommandResult {
    validateAnswers(command.added)

    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findByUuid(assessmentUuid),
        data = AssessmentAnswersUpdatedEvent(
          added = added,
          removed = removed,
          timeline = timeline,
        ),
      )
    }

    eventBus.handle(event).run(stateService::persist)
    eventService.save(event)

    return CommandSuccessCommandResult()
  }

  companion object {
    fun validateAnswers(added: Map<String, Any>) {
      added.forEach { (key, value) ->
        when (value) {
          is String -> Unit
          is List<*> -> {
            if (!value.all { it is String }) {
              throw IllegalArgumentException("Answer '$key' must be a String or List<String>")
            }
          }
          else -> throw IllegalArgumentException("Answer '$key' must be a String or List<String>")
        }
      }
    }
  }
}
