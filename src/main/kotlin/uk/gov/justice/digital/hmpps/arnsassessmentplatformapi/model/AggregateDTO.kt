package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.exception.UndefinedPosition
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

data class AggregateDTO(
  val uuid: UUID,
  val position: Int,
  val updatedAt: LocalDateTime,
  val eventsFrom: LocalDateTime,
  val eventsTo: LocalDateTime,
  val numberOfEventsApplied: Long,
  val assessmentUuid: UUID,
  val data: Aggregate<*>,
) {
  companion object {
    fun from(aggregate: AggregateEntity<*>) = AggregateDTO(
      uuid = aggregate.uuid,
      position = aggregate.position ?: throw UndefinedPosition("Aggregate with UUID: ${aggregate.uuid} does not have a valid position"),
      updatedAt = aggregate.updatedAt,
      eventsFrom = aggregate.eventsFrom,
      eventsTo = aggregate.eventsTo,
      numberOfEventsApplied = aggregate.numberOfEventsApplied,
      assessmentUuid = aggregate.assessment.uuid,
      data = aggregate.data,
    )
  }
}
