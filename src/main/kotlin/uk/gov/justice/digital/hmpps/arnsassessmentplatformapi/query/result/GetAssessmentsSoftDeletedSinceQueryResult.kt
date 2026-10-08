package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import java.util.UUID

class GetAssessmentsSoftDeletedSinceQueryResult(
  val assessments: List<UUID>,
) : QueryResult
