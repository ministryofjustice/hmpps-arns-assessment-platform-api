package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.DataDeletionOperation
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.DataDeletionRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.DataDeletionDataResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.DataDeletionResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.RedactedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception.EventHandlingException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.EventDTO
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.EventNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.TimelineNotFoundException
import java.util.UUID

@Service
class DataDeletionService(
  val assessmentService: AssessmentService,
  val eventService: EventService,
  val stateService: StateService,
  val timelineService: TimelineService,
  val auditService: AuditService,
  val clock: Clock,
) {
  fun getData(assessmentUuid: UUID) = DataDeletionDataResponse(
    events = eventService.findAllIncludingDeleted(assessmentUuid).map { EventDTO.from(it) },
    timeline = timelineService.findAllIncludingDeleted(assessmentUuid).map { TimelineItem.from(it) },
  )

  @Transactional
  fun updateData(assessmentUuid: UUID, request: DataDeletionRequest, jwt: Jwt): DataDeletionResponse {
    if (request.dryRun) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
    }

    val assessment = assessmentService.findBy(assessmentUuid)

    val existingTimelines = request.timeline
      .map { it.uuid }.toSet()
      .run(timelineService::findByUuidsIncludingDeleted)
      .associateBy { it.uuid }

    val replacementTimelines = request.timeline.map {
      val existingTimeline = existingTimelines[it.uuid] ?: throw TimelineNotFoundException(it.uuid)
      TimelineEntity(
        uuid = it.uuid,
        position = existingTimeline.position,
        createdAt = existingTimeline.createdAt,
        user = existingTimeline.user,
        assessment = existingTimeline.assessment,
        eventType = it.timeline.event,
        data = it.timeline.data,
        customType = it.timeline.customType,
        customData = it.timeline.customData,
        deleted = existingTimeline.deleted,
      )
    }

    val existingEvents = request.events
      .map { it.uuid }.toSet()
      .run(eventService::findByUuidsIncludingDeleted)
      .associateBy { it.uuid }

    val replacementEvents = request.events.map {
      val existingEvent = existingEvents[it.uuid] ?: throw EventNotFoundException(it.uuid)
      EventEntity(
        uuid = it.uuid,
        position = existingEvent.position,
        createdAt = existingEvent.createdAt,
        user = existingEvent.user,
        assessment = existingEvent.assessment,
        data = when (it.operation) {
          DataDeletionOperation.UPDATE -> it.event
          DataDeletionOperation.DELETE -> RedactedEvent(
            eventType = it.event::class.simpleName ?: "Unknown",
            dateRedacted = clock.now(),
          )
        },
        deleted = existingEvent.deleted,
      )
    }

    timelineService.hardDelete(existingTimelines.values.toList())
    timelineService.saveAll(replacementTimelines)

    eventService.hardDelete(existingEvents.values.toList())
    eventService.saveAll(replacementEvents)

    stateService.delete(assessment.uuid)

    try {
      val rebuiltState = stateService.rebuildFromEvents(assessment, null)
      stateService.persist(mutableMapOf(assessment.uuid to rebuiltState))

      if (!request.dryRun) {
        auditService.audit(
          jwt.subject,
          "RewroteHistory",
          "Updated ${existingEvents.count()} events and ${existingTimelines.count()} timelines." +
            "Event UUIDs: ${existingEvents.keys} ; Timeline UUIDs: ${existingTimelines.keys}",
        )
      }

      return DataDeletionResponse(
        success = true,
        dryRun = request.dryRun,
        state = rebuiltState,
      )
    } catch (ex: EventHandlingException) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()

      return DataDeletionResponse(
        success = false,
        dryRun = request.dryRun,
        exception = ex,
      )
    }
  }
}
