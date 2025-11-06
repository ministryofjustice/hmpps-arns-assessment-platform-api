package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Answers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Collaborators
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Collections
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.FormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Properties

data class AssessmentVersionQueryResult(
  val formVersion: FormVersion,
  val answers: Answers,
  val properties: Properties,
  val collections: Collections,
  val collaborators: Collaborators,
) : QueryResult
