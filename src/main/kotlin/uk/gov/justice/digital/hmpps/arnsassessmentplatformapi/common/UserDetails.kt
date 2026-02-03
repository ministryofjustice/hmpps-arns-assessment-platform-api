package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource

data class UserDetails(
  val id: String,
  val name: String,
  val authSource: AuthSource = AuthSource.NOT_SPECIFIED,
)
