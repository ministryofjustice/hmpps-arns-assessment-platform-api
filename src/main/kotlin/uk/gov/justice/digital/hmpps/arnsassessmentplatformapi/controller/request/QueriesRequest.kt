package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidQueryException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery

data class QueriesRequest(
  val queries: List<RequestableQuery>,
) {
  init {
    if (queries.isEmpty()) throw InvalidQueryException("No queries received")
  }
}
