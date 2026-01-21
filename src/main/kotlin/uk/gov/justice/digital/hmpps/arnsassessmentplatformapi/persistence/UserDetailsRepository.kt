package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.util.UUID

@Repository
interface UserDetailsRepository : JpaRepository<UserDetailsEntity, Long> {
  fun findByUuid(uuid: UUID): UserDetailsEntity?
  fun findByUserId(userId: String): List<UserDetailsEntity>
  fun findByUserIdAndAuthSource(userId: String, authSource: AuthSource): UserDetailsEntity?
}
