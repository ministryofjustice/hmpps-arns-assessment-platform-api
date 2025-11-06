package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.Answers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.Collaborators
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.Collections
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.FormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.Properties

data class AssessmentVersionQueryResult(
  val formVersion: FormVersion,
  val answers: Answers,
  val properties: Properties,
  val collections: Collections,
  val collaborators: Collaborators,
) : QueryResult
