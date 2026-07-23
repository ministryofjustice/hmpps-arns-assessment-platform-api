package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.AggregateDTO
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.EventDTO
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.EventNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.TimelineNotFoundException
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.util.UUID

@Service
class DataDeletionService(
  val assessmentService: AssessmentService,
  val eventService: EventService,
  val stateService: StateService,
  val timelineService: TimelineService,
  val auditService: AuditService,
  val clock: Clock,
  private val authenticationHolder: HmppsAuthenticationHolder,
) {
  fun getData(assessmentUuid: UUID) = DataDeletionDataResponse(
    events = eventService.findAllIncludingDeleted(assessmentUuid).map(EventDTO::from),
    timeline = timelineService.findAllIncludingDeleted(assessmentUuid).map(TimelineItem::from),
  )

  @Transactional
  fun updateData(assessmentUuid: UUID, request: DataDeletionRequest): DataDeletionResponse {
    if (request.dryRun) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
    }

    val assessment = assessmentService.findBy(assessmentUuid)

    val existingTimelines = request.timeline
      .map { it.uuid }.toSet()
      .run(timelineService::findByUuidsIncludingDeleted)
      .associateBy { it.uuid }

    val replacementTimelines = request.timeline
      .filter { it.operation == DataDeletionOperation.UPDATE }
      .map { replacement ->
        val existing = existingTimelines[replacement.uuid] ?: throw TimelineNotFoundException(replacement.uuid)
        TimelineEntity(
          uuid = replacement.uuid,
          position = existing.position,
          createdAt = existing.createdAt,
          user = existing.user,
          assessment = existing.assessment,
          eventType = replacement.timeline.event,
          data = replacement.timeline.data,
          customType = replacement.timeline.customType,
          customData = replacement.timeline.customData,
          deleted = existing.deleted,
        )
      }

    val existingEvents = request.events
      .map { it.uuid }.toSet()
      .run(eventService::findByUuidsIncludingDeleted)
      .associateBy { it.uuid }

    val replacementEvents = request.events.map { replacement ->
      val existing = existingEvents[replacement.uuid] ?: throw EventNotFoundException(replacement.uuid)
      EventEntity(
        uuid = replacement.uuid,
        position = existing.position,
        createdAt = existing.createdAt,
        user = existing.user,
        assessment = existing.assessment,
        data = when (replacement.operation) {
          DataDeletionOperation.UPDATE -> replacement.event
          DataDeletionOperation.DELETE -> RedactedEvent(
            eventType = replacement.event::class.simpleName ?: "Unknown",
            dateRedacted = clock.now(),
            redactedBy = authenticationHolder.principal,
          )
        },
        deleted = existing.deleted,
      )
    }

    timelineService.hardDelete(existingTimelines.values.toList())
    timelineService.saveAll(replacementTimelines)

    eventService.hardDelete(existingEvents.values.toList())
    eventService.saveAll(replacementEvents)

    return runCatching {
      replacementEvents
        .takeIf { it.isNotEmpty() }
        ?.let { stateService.delete(assessment.uuid) }
        ?.let { stateService.rebuildFromEvents(assessment, null) }
        ?.also { stateService.persist(mutableMapOf(assessment.uuid to it)) }
        .also {
          if (!request.dryRun) {
            auditService.audit(
              authenticationHolder.principal,
              "RewroteHistory",
              "Updated ${existingEvents.count()} events and ${existingTimelines.count()} timelines." +
                "Event UUIDs: ${existingEvents.keys} ; Timeline UUIDs: ${existingTimelines.keys}",
            )
          }
        }.let {
          DataDeletionResponse(
            success = true,
            dryRun = request.dryRun,
            rebuiltState = it?.mapValues { it.value.aggregates.map { aggregate -> AggregateDTO.from(aggregate) } },
          )
        }
    }.recoverCatching { ex ->
      if (ex is EventHandlingException) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()

        DataDeletionResponse(
          success = false,
          dryRun = request.dryRun,
          exception = ex,
        )
      } else {
        throw ex
      }
    }.getOrThrow()
  }
}
