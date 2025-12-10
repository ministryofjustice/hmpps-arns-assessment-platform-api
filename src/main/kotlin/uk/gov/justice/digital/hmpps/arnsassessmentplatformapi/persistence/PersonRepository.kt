package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.PersonEntity
import java.util.UUID

@Repository
interface PersonRepository : JpaRepository<PersonEntity, Long> {
  fun findByUuid(uuid: UUID): PersonEntity?
}
