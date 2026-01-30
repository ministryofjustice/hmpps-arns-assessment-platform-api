package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.exception.DuplicateExternalIdentifierException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssignedToUserEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException

@Component
class CreateAssessmentCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
  private val userDetailsService: UserDetailsService,
) : CommandHandler<CreateAssessmentCommand> {
  override val type = CreateAssessmentCommand::class
  override fun handle(command: CreateAssessmentCommand): CreateAssessmentCommandResult {
    val assessment = AssessmentEntity(
      uuid = command.assessmentUuid,
      type = command.assessmentType,
    ).apply {
      command.identifiers?.forEach {
        identifiers.add(
          AssessmentIdentifierEntity(
            assessment = this,
            identifierType = it.key,
            identifier = it.value,
          ),
        )
      }
    }

    assessment.identifiers.forEach {
      try {
        assessmentService.findBy(it.toIdentifier())
        throw DuplicateExternalIdentifierException(it.toIdentifier())
      } catch (_: AssessmentNotFoundException) {}
    }

    assessmentService.save(assessment)

    val user = userDetailsService.findOrCreate(command.user)

    val events = listOf(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessment,
          createdAt = assessment.createdAt,
          data = AssessmentCreatedEvent(
            formVersion = formVersion,
            properties = properties ?: emptyMap(),
            timeline = timeline,
          ),
        )
      },
      EventEntity(
        user = user,
        assessment = assessment,
        createdAt = assessment.createdAt,
        data = AssignedToUserEvent(
          userUuid = user.uuid,
          timeline = null,
        ),
      ),
    )

    events.forEach { event ->
      eventBus.handle(event).run(stateService::persist)
    }

    eventService.saveAll(events)

    return CreateAssessmentCommandResult(assessment.uuid)
  }
}
