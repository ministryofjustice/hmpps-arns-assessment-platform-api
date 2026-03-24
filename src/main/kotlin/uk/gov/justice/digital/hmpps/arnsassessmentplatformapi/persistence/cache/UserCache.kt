package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.cache

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity

@Component
@RequestScope
class UserCache {
  private val userDetails = mutableMapOf<UserDetails, UserDetailsEntity>()
  fun get(user: UserDetails) = userDetails[user]
  fun put(user: UserDetailsEntity) = userDetails.put(UserDetails.from(user), user).let { user }
}
