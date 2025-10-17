package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class CreateAssessmentCommandHandler(
  private val assessmentRepository: AssessmentRepository,
  private val eventBus: EventBus,
) : CommandHandler<CreateAssessmentCommand> {
  override val type = CreateAssessmentCommand::class
  override fun handle(command: CreateAssessmentCommand): CreateAssessmentCommandResult {
    val assessment = assessmentRepository.save(AssessmentEntity(uuid = command.assessmentUuid))
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessment,
          data = AssessmentCreatedEvent(),
        )
      },
    )
    return CreateAssessmentCommandResult(assessment.uuid)
  }
}
