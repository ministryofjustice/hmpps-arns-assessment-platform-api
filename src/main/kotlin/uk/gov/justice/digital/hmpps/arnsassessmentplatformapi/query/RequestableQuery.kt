package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails

sealed interface RequestableQuery : Query {
  val user: UserDetails
  val assessmentIdentifier: AssessmentIdentifier
}
