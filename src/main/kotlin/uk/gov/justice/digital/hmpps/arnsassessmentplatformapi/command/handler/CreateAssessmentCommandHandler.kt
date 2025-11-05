package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService

@Component
class CreateAssessmentCommandHandler(
  private val assessmentRepository: AssessmentRepository,
  private val eventBus: EventBus,
  private val assessmentService: AssessmentService,
  private val eventService: EventService,
) : CommandHandler<CreateAssessmentCommand> {
  override val type = CreateAssessmentCommand::class
  override fun handle(command: CreateAssessmentCommand): CreateAssessmentCommandResult {
    val assessment = assessmentRepository.save(AssessmentEntity(uuid = command.assessmentUuid))
    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessment,
        data = AssessmentCreatedEvent(properties),
      )
    }.run(eventService::save)

    assessmentService.blankState(event.assessment)
      .let { eventBus.handle(event, it) }
      .let { assessmentService.persist(it) }

    return CreateAssessmentCommandResult(assessment.uuid)
  }
}
