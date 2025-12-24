package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionView

data class CollectionQueryResult(
  val collection: CollectionView,
) : QueryResult
