package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.UnexpectedOasysValueException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class Alcohol : SanSectionMapping() {
  override fun map(oasys: OasysEquivalent, aap: AssessmentAggregate) {
    when (oasys["o9-2"]) {
      "0" -> Value.NO_EVIDENCE
      "1" -> Value.YES_WITH_SOME_EVIDENCE
      "2" -> Value.YES_WITH_EVIDENCE
      "M", null -> null
      else -> throw UnexpectedOasysValueException("o9-2")
    }?.let {
      aap.answers[Field.COMMUNITY_ALCOHOL_EVIDENCE_OF_EXCESS_DRINKING.lower] = SingleValue(it.name)
    }
  }
}
