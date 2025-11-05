package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User

data class AssessmentVersionQueryResult(
  val formVersion: String?,
  val answers: Map<String, List<String>>,
  val collections: List<Collection>,
  val collaborators: Set<User>,
) : QueryResult
