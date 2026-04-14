package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

data class GetAssessmentsModifiedSinceQueryResult(
  val assessments: List<AssessmentVersionQueryResult>,
  override val pageInfo: PageInfo,
) : QueryResult,
  PageableQueryResult
