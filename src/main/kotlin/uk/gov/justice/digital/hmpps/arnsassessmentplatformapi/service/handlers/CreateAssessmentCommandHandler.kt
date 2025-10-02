package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventBus

@Component
class CreateAssessmentCommandHandler(
  private val assessmentRepository: AssessmentRepository,
  private val eventBus: EventBus,
) : CommandHandler<CreateAssessment> {
  override val type = CreateAssessment::class
  override fun handle(command: CreateAssessment) {
    val assessment = assessmentRepository.save(AssessmentEntity(uuid = command.assessmentUuid))
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessment,
          data = AssessmentCreated(),
        )
      },
    )
  }
}
