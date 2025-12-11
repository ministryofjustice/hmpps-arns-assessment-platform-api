package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User

sealed interface RequestableQuery : Query {
  val user: User
  val assessmentIdentifier: AssessmentIdentifier
}
