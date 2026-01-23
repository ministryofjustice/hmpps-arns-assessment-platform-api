package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

interface UserQuery : RequestableQuery {
  val subject: UserIdentifier
}
