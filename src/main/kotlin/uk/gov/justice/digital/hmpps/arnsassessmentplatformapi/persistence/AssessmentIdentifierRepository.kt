package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import java.time.LocalDateTime

@Repository
interface AssessmentIdentifierRepository : JpaRepository<AssessmentIdentifierEntity, Long> {
  fun findFirstByExternalIdentifierTypeAndExternalIdentifierIdAndAssessmentTypeAndCreatedAtBeforeOrderByCreatedAtDesc(type: IdentifierType, identifier: String, assessmentType: String, pointInTime: LocalDateTime): AssessmentIdentifierEntity?
}
