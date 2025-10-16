package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class UpdateFormVersionHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<UpdateFormVersion> {
  override val type = UpdateFormVersion::class
  override fun handle(command: UpdateFormVersion): CommandSuccessResult {
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessmentService.findByUuid(assessmentUuid),
          data = FormVersionUpdated(version),
        )
      },
    )
    return CommandSuccessResult()
  }
}
