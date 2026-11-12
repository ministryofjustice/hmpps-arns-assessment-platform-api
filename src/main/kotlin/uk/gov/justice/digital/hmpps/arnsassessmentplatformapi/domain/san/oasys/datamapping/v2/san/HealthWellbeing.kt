package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.UnexpectedOasysValueException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class HealthWellbeing : SanSectionMapping() {
  override fun map(oasys: OasysEquivalent, aap: AssessmentAggregate) {
    when (oasys["o4-8"]) {
      "0" -> Value.NO
      "1" -> Value.YES_SOME_DIFFICULTIES
      "2" -> Value.YES_SIGNIFICANT
      "M", null -> null
      else -> throw UnexpectedOasysValueException("o4-8")
    }?.let {
      aap.answers[Field.HEALTH_WELLBEING_LEARNING_DIFFICULTIES.lower] = SingleValue(it.name)
    }

    when (oasys["o10-1"]) {
      "0" -> Value.YES
      "1" -> Value.YES_SOME_DIFFICULTIES
      "2" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o10-1")
    }?.let {
      aap.answers[Field.HEALTH_WELLBEING_COPING_DAY_TO_DAY_LIFE.lower] = SingleValue(it.name)
    }

    when (oasys["o10-4"]) {
      "0" -> Value.POSITIVE
      "1" -> Value.SOME_NEGATIVE_ASPECTS
      "2" -> Value.NEGATIVE
      null -> null
      else -> throw UnexpectedOasysValueException("o10-4")
    }?.let {
      aap.answers[Field.HEALTH_WELLBEING_ATTITUDE_TOWARDS_SELF.lower] = SingleValue(it.name)
    }
  }
}
