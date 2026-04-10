package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface EventRepository : JpaRepository<EventEntity<*>, Long> {
  fun findAllByAssessmentUuidOrderById(uuid: UUID): List<EventEntity<*>>
  fun findAllByAssessmentUuidAndCreatedAtIsLessThanEqual(uuid: UUID, dateTime: LocalDateTime): List<EventEntity<*>>
  fun findAllByAssessmentUuidAndCreatedAtGreaterThanAndCreatedAtLessThanEqual(assessmentUuid: UUID, from: LocalDateTime, to: LocalDateTime): List<EventEntity<*>>
  fun findTopByAssessmentUuidOrderByPositionDesc(assessmentUuid: UUID): EventEntity<*>?
  fun findAllByAssessmentUuidAndCreatedAtGreaterThanEqual(assessmentUuid: UUID, from: LocalDateTime): List<EventEntity<*>>
}
