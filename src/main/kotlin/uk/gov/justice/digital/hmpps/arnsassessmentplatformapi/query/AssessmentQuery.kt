package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

interface AssessmentQuery : RequestableQuery {
  val assessmentIdentifier: AssessmentIdentifier
}
