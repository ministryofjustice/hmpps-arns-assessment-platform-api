package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.CollectionItemView

data class CollectionItemQueryResult(
  val collectionItem: CollectionItemView,
) : QueryResult
