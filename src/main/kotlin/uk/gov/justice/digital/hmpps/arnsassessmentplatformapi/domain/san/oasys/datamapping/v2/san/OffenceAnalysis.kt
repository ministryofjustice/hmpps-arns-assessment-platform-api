package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.UnexpectedOasysValueException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class OffenceAnalysis : SanSectionMapping() {
  override fun map(oasys: OasysEquivalent, aap: AssessmentAggregate) {
    (oasys["o2-1"] as String?)?.let {
      aap.answers[Field.OFFENCE_ANALYSIS_DESCRIPTION_OF_OFFENCE.lower] = SingleValue(it)
    }

    (oasys["o2-8"] as String?)?.let {
      aap.answers[Field.OFFENCE_ANALYSIS_REASON.lower] = SingleValue(it)
    }

    (oasys["o2-12"] as String?)?.let {
      aap.answers[Field.OFFENCE_ANALYSIS_PATTERNS_OF_OFFENDING.lower] = SingleValue(it)
    }

    when (oasys["o2-6"]) {
      "YES" -> Value.YES
      "NO" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o2-6")
    }?.let {
      aap.answers[Field.OFFENCE_ANALYSIS_IMPACT_ON_VICTIMS.lower] = SingleValue(it.name)
    }

    when (oasys["o2-11"]) {
      "YES" -> Value.YES
      "NO" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o2-11")
    }?.let {
      aap.answers[Field.OFFENCE_ANALYSIS_ACCEPT_RESPONSIBILITY.lower] = SingleValue(it.name)
    }

    when (oasys["o2-99"]) {
      "YES" -> Value.YES
      "NO" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o2-99")
    }?.let {
      aap.answers[Field.OFFENCE_ANALYSIS_RISK.lower] = SingleValue(it.name)
    }

    when (oasys["o2-7"]) {
      "YES" -> when (oasys["o2-7-1"]) {
        "0" -> Value.NONE
        "1" -> Value.ONE
        "2" -> Value.TWO
        "3" -> Value.THREE
        "4" -> Value.FOUR
        "5" -> Value.FIVE
        "6-10" -> Value.SIX_TO_10
        "11-15" -> Value.ELEVEN_TO_15
        "15+" -> Value.MORE_THAN_15
        null -> null
        else -> throw UnexpectedOasysValueException("o2-7-1")
      }
      "NO", null -> null
      else -> throw UnexpectedOasysValueException("o2-7")
    }?.let {
      aap.answers[Field.OFFENCE_ANALYSIS_HOW_MANY_INVOLVED.lower] = SingleValue(it.name)
    }
  }
}
