package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Query
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.QueryResult

data class QueryResponse(
  val request: Query,
  val result: QueryResult,
)
