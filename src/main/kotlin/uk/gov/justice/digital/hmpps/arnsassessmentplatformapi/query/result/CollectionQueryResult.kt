package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.model.Collection

data class CollectionQueryResult(
  val collection: Collection,
) : QueryResult
