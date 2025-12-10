package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateView
import java.time.LocalDateTime
import java.util.UUID

interface AggregateEntityView<A : AggregateView> {
  val id: Long?
  val uuid: UUID
  val updatedAt: LocalDateTime
  val eventsFrom: LocalDateTime
  val eventsTo: LocalDateTime
  val numberOfEventsApplied: Long
  val assessment: AssessmentEntity
  val data: A
}
