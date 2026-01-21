package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.UserDetailsRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity

@Service
class UserDetailsService(
  private val userDetailsRepository: UserDetailsRepository,
) {
  fun findOrCreate(commandUser: UserDetails) = userDetailsRepository.findByUserIdAndAuthSource(commandUser.userId, commandUser.authSource)
    ?: UserDetailsEntity(
      userId = commandUser.userId,
      displayName = commandUser.displayName,
      authSource = commandUser.authSource,
    ).run(userDetailsRepository::save)
}
