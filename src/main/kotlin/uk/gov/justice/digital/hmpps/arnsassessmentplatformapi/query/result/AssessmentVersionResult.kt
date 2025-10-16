package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User

@JsonTypeName("AssessmentVersionResult")
data class AssessmentVersionResult(
  val formVersion: String?,
  val answers: Map<String, List<String>>,
  val collaborators: Set<User>,
) : QueryResult
