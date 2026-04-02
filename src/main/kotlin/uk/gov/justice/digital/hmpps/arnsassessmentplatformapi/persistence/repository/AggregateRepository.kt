package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface AggregateRepository : JpaRepository<AggregateEntity<*>, Long> {
  fun findTopByAssessmentUuidAndDataTypeOrderByPositionDesc(assessmentUuid: UUID, dataType: String): AggregateEntity<*>?

  fun findTopByAssessmentUuidAndDataTypeAndEventsToLessThanEqualOrderByPositionDesc(
    assessmentUuid: UUID,
    aggregateType: String,
    beforeDate: LocalDateTime,
  ): AggregateEntity<*>?
}
