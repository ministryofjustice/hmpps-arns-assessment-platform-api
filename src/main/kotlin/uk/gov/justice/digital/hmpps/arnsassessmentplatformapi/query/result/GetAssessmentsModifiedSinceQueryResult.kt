package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import java.util.UUID

data class GetAssessmentsModifiedSinceQueryResult(
  val assessments: List<AssessmentVersionQueryResult>,
  val nextCursor: UUID?,
) : QueryResult
