package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import java.util.UUID

@Repository
interface AssessmentRepository : JpaRepository<AssessmentEntity, Long> {
  fun findByUuid(uuid: UUID): AssessmentEntity?

  fun findAllByIdentifiersExternalIdentifierIn(identifiers: Set<IdentifierPair>): Set<AssessmentEntity>
}
