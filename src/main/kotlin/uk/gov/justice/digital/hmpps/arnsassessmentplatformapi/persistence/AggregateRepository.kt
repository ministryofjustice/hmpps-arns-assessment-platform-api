package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.util.UUID

@Repository
interface AggregateRepository : JpaRepository<AggregateEntity, Long> {
  @Query(
    value = """
        SELECT * FROM aggregate 
        WHERE assessment_uuid = :assessmentUuid 
          AND data ->> 'type' = :aggregateType 
        ORDER BY updated_at DESC 
        LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findLatestByAssessmentAndType(
    @Param("assessmentUuid") assessmentUuid: UUID,
    @Param("aggregateType") aggregateType: String,
  ): AggregateEntity?
}
