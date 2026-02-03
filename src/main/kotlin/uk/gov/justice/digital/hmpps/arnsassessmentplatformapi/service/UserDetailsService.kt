package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.UserDetailsRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.UserDetailsNotFoundException
import java.util.UUID

@Service
class UserDetailsService(
  private val userDetailsRepository: UserDetailsRepository,
) {
  fun findByUserUuid(userUuid: UUID) = userDetailsRepository.findByUuid(userUuid)
  fun findUsersByUuids(userUuids: Collection<UUID>) = userDetailsRepository.findAllByUuidIsIn(userUuids.toSet())

  fun find(user: UserDetails) = userDetailsRepository.findByUserIdAndAuthSource(user.id, user.authSource)
    ?: throw UserDetailsNotFoundException()

  fun findOrCreate(user: UserDetails) = userDetailsRepository.findByUserIdAndAuthSource(user.id, user.authSource)
    ?: UserDetailsEntity(
      userId = user.id,
      displayName = user.name,
      authSource = user.authSource,
    ).run(userDetailsRepository::save)
}
