package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

interface PageableQuery : Query {
  val pageNumber: Int
  val pageSize: Int
}
