package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssignedToUserEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventProto
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair

class CreateAssessmentCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<CreateAssessmentCommand> {
  override val type = CreateAssessmentCommand::class
  override fun handle(command: CreateAssessmentCommand): CreateAssessmentCommandResult {
    val assessment = AssessmentEntity(
      uuid = command.assessmentUuid.value,
      type = command.assessmentType,
      createdAt = services.clock.requestDateTime(),
    ).apply {
      command.identifiers?.forEach {
        identifiers.add(
          AssessmentIdentifierEntity(
            assessment = this,
            externalIdentifier = IdentifierPair(it.key, it.value),
            createdAt = services.clock.requestDateTime(),
          ),
        )
      }
    }

    services.assessment.save(assessment)

    val user = services.userDetails.findOrCreate(command.user)

    val createEvent = with(command) {
      EventProto(
        user = user,
        assessment = assessment,
        createdAt = services.clock.requestDateTime(),
        data = AssessmentCreatedEvent(
          formVersion = formVersion,
          properties = properties ?: emptyMap(),
          flags = flags,
        ),
      )
    }

    val assignEvent = EventProto(
      user = user,
      assessment = assessment,
      createdAt = assessment.createdAt,
      data = AssignedToUserEvent(
        userUuid = user.uuid,
      ),
    )

    services.eventBus.handle(createEvent).createTimeline(command.timeline)
    services.eventBus.handle(assignEvent).createTimeline(command.timeline)

    return CreateAssessmentCommandResult(assessment.uuid)
  }
}
