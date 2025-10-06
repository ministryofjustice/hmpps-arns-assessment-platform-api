package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateFormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.FormVersionUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventBus

@Component
class UpdateFormVersionCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<UpdateFormVersion> {
  override val type = UpdateFormVersion::class
  override fun handle(command: UpdateFormVersion) {
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessmentService.findByUuid(assessmentUuid),
          data = FormVersionUpdated(version),
        )
      },
    )
  }
}
