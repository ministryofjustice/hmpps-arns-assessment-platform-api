package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.AddOasysEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.OasysEventAdded
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventBus

@Component
class AddOasysEventCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<AddOasysEvent> {
  override val type = AddOasysEvent::class
  override fun handle(command: AddOasysEvent) {
    val assessment = assessmentService.findByUuid(command.assessmentUuid)
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessment,
          data = OasysEventAdded(tag),
        )
      },
    )
  }
}
