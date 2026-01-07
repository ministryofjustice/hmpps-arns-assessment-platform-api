package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model.ValueHistory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model.ValueId
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value

typealias Values = MutableMap<ValueId, ValueHistory>

class ValuesAggregate :
  Aggregate<ValuesAggregate>,
  ValuesAggregateView {
  override val answers: Values = mutableMapOf()
  override val properties: Values = mutableMapOf()

  override fun clone() = ValuesAggregate().also { clone ->
    clone.answers.putAll(answers)
  }

  fun addAnswer(id: ValueId, value: Value) = answers.putIfAbsent(id, ValueHistory(value))?.addPatch(value)

  fun addProperty(id: ValueId, value: Value) = properties.putIfAbsent(id, ValueHistory(value))?.addPatch(value)
}
