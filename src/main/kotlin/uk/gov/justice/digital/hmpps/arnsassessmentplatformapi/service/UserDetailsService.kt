package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.UserDetailsRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.util.UUID

@Service
class UserDetailsService(
  private val userDetailsRepository: UserDetailsRepository,
) {
  fun findByUserUuid(userUuid: UUID) = userDetailsRepository.findByUuid(userUuid)
  fun findUsersByUuids(userUuids: Collection<UUID>) = userDetailsRepository.findAllByUuidIsIn(userUuids.toSet())

  fun findOrCreate(commandUser: UserDetails) = userDetailsRepository.findByUserIdAndAuthSource(commandUser.id, commandUser.authSource)
    ?: UserDetailsEntity(
      userId = commandUser.id,
      displayName = commandUser.name,
      authSource = commandUser.authSource,
    ).run(userDetailsRepository::save)
}
