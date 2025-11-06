package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollBackAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import java.time.Clock
import java.time.LocalDateTime

@Component
class RollbackAnswersCommandHandler(
  private val assessmentService: AssessmentService,
  private val stateService: StateService,
  private val eventBus: EventBus,
  private val clock: Clock,
  private val eventService: EventService,
) : CommandHandler<RollBackAssessmentAnswersCommand> {
  private fun now() = LocalDateTime.now(clock)
  override val type = RollBackAssessmentAnswersCommand::class
  override fun handle(command: RollBackAssessmentAnswersCommand): CommandSuccessCommandResult {
    with(command) {
      EventEntity(
        user = command.user,
        assessment = assessmentService.findByUuid(assessmentUuid),
        data = AssessmentAnswersRolledBackEvent(
          rolledBackTo = command.pointInTime,
        ),
      )
    }
      .run(eventService::save)
      .run(eventBus::handle)
      .run(stateService::persist)

    return CommandSuccessCommandResult()
  }
}
