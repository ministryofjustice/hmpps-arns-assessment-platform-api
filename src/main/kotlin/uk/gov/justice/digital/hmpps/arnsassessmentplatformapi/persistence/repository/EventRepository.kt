package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface EventRepository : JpaRepository<EventEntity<*>, Long> {
  fun findAllByAssessmentUuidOrderById(uuid: UUID): List<EventEntity<*>>
  fun findAllByAssessmentUuidAndCreatedAtIsLessThanEqual(uuid: UUID, dateTime: LocalDateTime): List<EventEntity<*>>
  fun findAllByAssessmentUuidAndCreatedAtGreaterThanAndCreatedAtLessThanEqual(assessmentUuid: UUID, from: LocalDateTime, to: LocalDateTime): List<EventEntity<*>>

  @Query(
    value = """
        SELECT e.*
        FROM event e
        WHERE e.assessment_uuid = :assessmentUuid
        ORDER BY e.position DESC
        LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findTopByAssessmentUuidOrderByPositionDesc(assessmentUuid: UUID): EventEntity<*>?
  fun findAllByAssessmentUuidAndCreatedAtGreaterThanEqual(assessmentUuid: UUID, from: LocalDateTime): List<EventEntity<*>>

  // Cursor pattern used over normal offset pagination because paginating
  // over large result sets take long enough for the underlying data to
  // change, and offset pagination can drop rows when that happens.
  @Query(
    """
    SELECT DISTINCT a FROM EventEntity e
    JOIN e.assessment a
    WHERE a.type = :assessmentType
      AND e.createdAt > :since
      AND e.deleted IS FALSE
      AND (:after IS NULL OR a.uuid > :after)
    ORDER BY a.uuid
    """,
  )
  fun findAssessmentsModifiedSinceAfter(
    assessmentType: String,
    since: LocalDateTime,
    after: UUID?,
    limit: Limit,
  ): List<AssessmentEntity>
}
