package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.projection.DailyVersionProjection
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface TimelineRepository :
  JpaRepository<TimelineEntity, Long>,
  JpaSpecificationExecutor<TimelineEntity> {
  fun findByUuid(uuid: UUID): TimelineEntity?

  fun findByAssessmentUuid(assessmentUuid: UUID): List<TimelineEntity>

  fun findTopByAssessmentUuidOrderByPositionDesc(assessmentUuid: UUID): TimelineEntity?

  @Query(
    value = """
            SELECT
                min_created_at AS createdAt,
                max_created_at AS updatedAt,
                uuid AS lastTimelineUuid
            FROM (
                SELECT
                    DATE(t.created_at) AS day,
                    t.created_at,
                    t.uuid,
                    MIN(t.created_at) OVER (PARTITION BY DATE(t.created_at)) AS min_created_at,
                    MAX(t.created_at) OVER (PARTITION BY DATE(t.created_at)) AS max_created_at,
                    ROW_NUMBER() OVER (
                        PARTITION BY DATE(t.created_at)
                        ORDER BY t.created_at DESC, t.position DESC
                    ) AS rn
                FROM timeline t
                WHERE t.assessment_uuid = :assessmentUuid
                  AND t.deleted IS FALSE
            ) ranked
            WHERE rn = 1
            ORDER BY day
        """,
    nativeQuery = true,
  )
  fun findDailyVersionsByAssessment(
    @Param("assessmentUuid") assessmentUuid: UUID,
  ): List<DailyVersionProjection>

  fun findByAssessmentUuidAndCreatedAtGreaterThanEqual(assessmentUuid: UUID, from: LocalDateTime): List<TimelineEntity>

  @Query(
    """
    SELECT t FROM TimelineEntity t
    WHERE t.assessment = :assessmentUuid
    ORDER BY t.position
    """,
  )
  fun findAllIncludingDeleted(
    assessmentUuid: UUID,
  ): List<TimelineEntity>

  @Query(
    """
    SELECT t FROM TimelineEntity t
    WHERE t.uuid IN :timelineUuids
    ORDER BY t.position
    """,
  )
  fun findByUuidsIncludingDeleted(
    timelineUuids: Set<UUID>,
  ): List<TimelineEntity>
}
