package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class CreateAssessmentHandler(
  private val assessmentRepository: AssessmentRepository,
  private val eventBus: EventBus,
) : CommandHandler<CreateAssessment> {
  override val type = CreateAssessment::class
  override fun handle(command: CreateAssessment): CreateAssessmentResult {
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
    return CreateAssessmentResult(assessment.uuid)
  }
}
