package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.UnexpectedOasysValueException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class Accommodation : SanSectionMapping() {
  override fun map(oasys: OasysEquivalent, aap: AssessmentAggregate) {
    // TODO: apply mappings to the assessment aggregate
    when (oasys["o1-test"]) {
      0 -> Value.SETTLED
      1, 2 -> Value.TEMPORARY
      null -> null
      else -> throw UnexpectedOasysValueException("o1-test")
    }?.let {
      aap.answers[Field.CURRENT_ACCOMMODATION.lower] = SingleValue(it.name)
    }
  }
}
