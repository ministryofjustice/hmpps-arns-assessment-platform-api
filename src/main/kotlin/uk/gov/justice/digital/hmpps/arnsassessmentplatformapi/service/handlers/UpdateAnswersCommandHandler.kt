package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventBus

@Component
class UpdateAnswersCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<UpdateAnswers> {
  override val type = UpdateAnswers::class
  override fun handle(command: UpdateAnswers) {
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessmentService.findByUuid(assessmentUuid),
          data = AnswersUpdated(
            added = added,
            removed = removed,
          ),
        )
      },
    )
  }
}
