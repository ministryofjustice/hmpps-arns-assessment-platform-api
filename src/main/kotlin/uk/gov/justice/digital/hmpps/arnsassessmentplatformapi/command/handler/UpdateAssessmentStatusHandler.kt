package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentStatusUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class UpdateAssessmentStatusHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<UpdateAssessmentStatus> {
  override val type = UpdateAssessmentStatus::class
  override fun handle(command: UpdateAssessmentStatus): CommandSuccessResult {
    val assessment = assessmentService.findByUuid(command.assessmentUuid)
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessment,
          data = AssessmentStatusUpdated(status),
        )
      },
    )
    return CommandSuccessResult()
  }
}
