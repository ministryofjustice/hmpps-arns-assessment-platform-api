package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface EventRepository : JpaRepository<EventEntity, Long> {
  fun findAllByCollectionUuid(uuid: UUID): List<EventEntity>

  fun findAllByCollectionUuidAndCreatedAtBefore(uuid: UUID, dateTime: LocalDateTime): List<EventEntity>

  @Query(
    """
        SELECT e FROM EventEntity e 
        JOIN e.collection c 
        WHERE c.leftBound >= :lft AND c.rightBound <= :rgt
        ORDER BY e.createdAt ASC
    """,
  )
  fun findEventsInSubtree(
    @Param("lft") lft: Long,
    @Param("rgt") rgt: Long,
  ): List<EventEntity>

  @Query(
    """
        SELECT e FROM EventEntity e 
        JOIN e.collection c 
        WHERE c.leftBound >= :lft AND c.rightBound <= :rgt
          AND e.createdAt >= :from AND e.createdAt < :to
        ORDER BY e.createdAt ASC
    """,
  )
  fun findEventsInSubtreeBetween(
    @Param("lft") lft: Long,
    @Param("rgt") rgt: Long,
    @Param("from") from: OffsetDateTime,
    @Param("to") to: OffsetDateTime,
  ): List<EventEntity>
}
