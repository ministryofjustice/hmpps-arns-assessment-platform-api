package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface AggregateRepository : JpaRepository<AggregateEntity<*>, Long> {
  @Query(
    value = """
        SELECT * FROM aggregate 
        WHERE assessment_uuid = :assessmentUuid 
          AND data ->> 'type' = :aggregateType 
          AND events_to <= :beforeDate 
        ORDER BY events_to DESC 
        LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findByAssessmentAndTypeBeforeDate(
    @Param("assessmentUuid") assessmentUuid: UUID,
    @Param("aggregateType") aggregateType: String,
    @Param("beforeDate") beforeDate: LocalDateTime,
  ): AggregateEntity<*>?

  @Query(
    value = """
        SELECT * FROM aggregate 
        WHERE assessment_uuid = :assessmentUuid 
          AND data ->> 'type' = :aggregateType 
          AND events_to = :beforeDate 
        ORDER BY events_to DESC 
        LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findByAssessmentAndTypeOnExactDate(
    @Param("assessmentUuid") assessmentUuid: UUID,
    @Param("aggregateType") aggregateType: String,
    @Param("beforeDate") beforeDate: LocalDateTime = Clock.now(),
  ): AggregateEntity<*>?
}
