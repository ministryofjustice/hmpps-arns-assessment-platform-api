package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.exception.DuplicateExternalIdentifierException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssignedToUserEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException

@Component
class CreateAssessmentCommandHandler(
  private val services: CommandHandlerServiceBundle,
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
        services.assessment.findBy(it.toIdentifier())
        throw DuplicateExternalIdentifierException(it.toIdentifier())
      } catch (_: AssessmentNotFoundException) {
      }
    }

    services.assessment.save(assessment)

    val user = services.userDetails.findOrCreate(command.user)

    val createEvent = with(command) {
      EventEntity(
        user = user,
        assessment = assessment,
        createdAt = assessment.createdAt,
        data = AssessmentCreatedEvent(
          formVersion = formVersion,
          properties = properties ?: emptyMap(),
        ),
      )
    }

    val assignEvent = EventEntity(
      user = user,
      assessment = assessment,
      createdAt = assessment.createdAt,
      data = AssignedToUserEvent(
        userUuid = user.uuid,
      ),
    )

    val events = listOf(createEvent, assignEvent)

    events.map { event ->
      services.eventBus.handle(event)
        .also { updatedState -> services.state.persist(updatedState) }
        .run { get(AssessmentAggregate::class) as AssessmentState }
    }

    services.event.saveAll(events)
    services.timeline.saveAll(
      listOf(
        TimelineEntity.from(
          command,
          createEvent,
          mapOf(),
        ),
        TimelineEntity.from(
          command,
          assignEvent,
          mapOf(
            "assignee" to user,
          ),
        ),
      ),
    )

    return CreateAssessmentCommandResult(assessment.uuid)
  }
}
