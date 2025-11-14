package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface EventRepository : JpaRepository<EventEntity<*>, Long> {
  fun findAllByAssessmentUuid(uuid: UUID): List<EventEntity<*>>

  fun findAllByAssessmentUuidAndCreatedAtIsLessThanEqual(uuid: UUID, dateTime: LocalDateTime): List<EventEntity<*>>
}
