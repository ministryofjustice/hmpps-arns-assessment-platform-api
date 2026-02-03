package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

data class PageInfo(
  val pageNumber: Int,
  val totalPages: Int,
)

interface PageableQueryResult {
  val pageInfo: PageInfo
}
