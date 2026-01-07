package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model

import com.github.difflib.DiffUtils
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.exception.IncompatibleValueException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.MultiValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value

data class ValueHistory(
  val initialValue: Value,
  var latestValue: Value = initialValue,
) {
  val patches: MutableList<ValuePatch> = mutableListOf()

  fun addPatch(revised: Value) {
    if (latestValue == revised) return

    val original = latestValue
    val patch = when {
      original is SingleValue && revised is SingleValue ->
        DiffUtils.diff(original.value.lineSequence().toList(), revised.value.lineSequence().toList())
      original is MultiValue && revised is MultiValue ->
        DiffUtils.diff(original.values, revised.values)
      else -> throw IncompatibleValueException(original, revised)
    }

    patches.add(ValuePatch(patch, original::class))
    latestValue = revised
  }
}
