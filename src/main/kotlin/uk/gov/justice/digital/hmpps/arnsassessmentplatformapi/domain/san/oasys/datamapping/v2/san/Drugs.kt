package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.UnexpectedOasysValueException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class Drugs : SanSectionMapping() {
  override fun map(oasys: OasysEquivalent, aap: AssessmentAggregate) {
    when (oasys["o8-1"]) {
      "YES" -> Value.YES
      "NO" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o8-1")
    }?.let {
      aap.answers[Field.DRUG_USE.lower] = SingleValue(it.name)
    }

    when (oasys["o8-8"]) {
      "0" -> Value.FULL_MOTIVATION
      "1" -> Value.PARTIAL_MOTIVATION
      "2" -> Value.NO_MOTIVATION
      "M", null -> null
      else -> throw UnexpectedOasysValueException("o8-8")
    }?.let {
      aap.answers[Field.DRUGS_PRACTITIONER_ANALYSIS_MOTIVATED_TO_STOP.lower] = SingleValue(it.name)
    }
  }
}
