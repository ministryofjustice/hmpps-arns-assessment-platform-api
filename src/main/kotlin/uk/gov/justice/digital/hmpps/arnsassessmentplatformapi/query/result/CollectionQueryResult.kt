package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Collection

data class CollectionQueryResult(
  val collection: Collection,
) : QueryResult
