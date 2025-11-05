package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface AggregateRepository : JpaRepository<AggregateEntity<*>, Long> {
  @Query(
    value = """
        SELECT * FROM aggregate 
        WHERE assessment_uuid = :assessmentUuid 
          AND events_to <= :beforeDate 
        ORDER BY events_to DESC 
        LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findByAssessmentBeforeDate(
    @Param("assessmentUuid") assessmentUuid: UUID,
    @Param("beforeDate") beforeDate: LocalDateTime,
  ): AggregateEntity<*>?

  @Query(
    value = """
        SELECT * FROM aggregate 
        WHERE assessment_uuid = :assessmentUuid 
          AND events_to = :beforeDate 
        ORDER BY events_to DESC 
        LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findByAssessmentOnExactDate(
    @Param("assessmentUuid") assessmentUuid: UUID,
    @Param("beforeDate") beforeDate: LocalDateTime = LocalDateTime.now(),
  ): AggregateEntity<*>?
}
