package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence.CreatedBy
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence.CreatedByResolver
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence.OasysVersionRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Service
class OasysVersionService(
  private val repository: OasysVersionRepository,
) {
  fun createVersionFor(event: EventEntity<*>) {
    val latest = repository.findTopByAssessmentUuidOrderByVersionDesc(event.assessment.uuid)

    val now = Clock.now()
    val createdBy = CreatedByResolver.from(event)
    val eventStatus = event.statusOrNull()

    val shouldUpdateLatest = latest != null &&
      createdBy == CreatedBy.DAILY_EDIT &&
      (latest.status == "UNSIGNED" || latest.status == null) &&
      latest.updatedAt.toLocalDate().isEqual(now.toLocalDate())

    val entity = if (shouldUpdateLatest) {
      latest.apply {
        status = eventStatus ?: status ?: "UNSIGNED"
        updatedAt = now
        lastEvent = event
      }
    } else {
      val nextVersion = latest?.version?.plus(1) ?: 0
      val status = eventStatus ?: latest?.status ?: "UNSIGNED"

      OasysVersionEntity(
        createdBy = createdBy,
        version = nextVersion,
        status = status,
        lastEvent = event,
        assessment = event.assessment,
      )
    }

    repository.save(entity)
  }

  fun EventEntity<*>.statusOrNull(): String? = ((data as? AssessmentPropertiesUpdatedEvent)?.added?.get("STATUS") as? SingleValue)?.value
}
