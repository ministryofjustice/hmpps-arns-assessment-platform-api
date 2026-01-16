package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OasysVersionRepository : JpaRepository<OasysVersionEntity, Long> {
  @Query(
    """
        SELECT COALESCE(MAX(v.version), 0)
        FROM OasysVersionEntity v
        WHERE v.assessment.uuid = :assessmentUuid
    """,
  )
  fun findMaxVersionByAssessmentUuid(
    @Param("assessmentUuid") assessmentUuid: UUID,
  ): Long

  fun findTopByAssessmentUuidOrderByVersionDesc(
    assessmentUuid: UUID,
  ): OasysVersionEntity?
}
