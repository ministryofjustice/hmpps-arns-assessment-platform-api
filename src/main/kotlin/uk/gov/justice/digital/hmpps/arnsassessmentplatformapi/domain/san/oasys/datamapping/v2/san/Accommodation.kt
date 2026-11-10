package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class Accommodation : SanSectionMapping() {
  override fun map(oasysEquivalent: OasysEquivalent, assessmentAggregate: AssessmentAggregate) {
    // TODO: apply mappings to the assessment aggregate
    assessmentAggregate.answers["field"] = when (oasysEquivalent["o2-3"]) {
      "0" -> { "YES" }
      1, 2 -> { "NO" }
      null -> { "NO" }
      else -> throw RuntimeException("Unrecognised section structure")
    }.run(::SingleValue)
  }
}
