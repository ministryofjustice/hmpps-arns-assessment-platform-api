package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

class GetAssessmentsSoftDeletedSinceQueryResult(
  val assessments: List<AssessmentVersionQueryResult>,
) : QueryResult