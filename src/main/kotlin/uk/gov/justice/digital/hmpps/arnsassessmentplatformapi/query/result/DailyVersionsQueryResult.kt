package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.DailyVersionDetails

data class DailyVersionsQueryResult(
  val versions: List<DailyVersionDetails>,
) : QueryResult
