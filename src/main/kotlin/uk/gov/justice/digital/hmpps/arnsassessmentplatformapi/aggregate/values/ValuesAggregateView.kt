package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model.ValueHistory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model.ValueId

typealias ValuesView = Map<ValueId, ValueHistory>

interface ValuesAggregateView : AggregateView {
  val answers: ValuesView
  val properties: ValuesView
}
