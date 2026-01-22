package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.util.UUID

@Repository
interface UserDetailsRepository : JpaRepository<UserDetailsEntity, Long> {
  fun findByUuid(uuid: UUID): UserDetailsEntity?
  fun findAllByUuidIsIn(uuids: Set<UUID>): Set<UserDetailsEntity>
  fun findByUserIdAndAuthSource(userId: String, authSource: AuthSource): UserDetailsEntity?
}
