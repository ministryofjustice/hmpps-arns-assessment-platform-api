package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.UnexpectedOasysValueException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class EmploymentEducation : SanSectionMapping() {
  override fun map(oasys: OasysEquivalent, aap: AssessmentAggregate) {
    when (oasys["o4-3"]) {
      "0" -> Value.STABLE
      "1" -> Value.PERIODS_OF_INSTABILITY
      "2" -> Value.UNSTABLE
      "M", null -> null
      else -> throw UnexpectedOasysValueException("o4-3")
    }?.let {
      aap.answers[Field.COMMUNITY_EMPLOYMENT_HISTORY.lower] = SingleValue(it.name)
    }

    when (oasys["o4-9"]) {
      "0" -> Value.YES
      "2" -> Value.NO
      "M", null -> null
      else -> throw UnexpectedOasysValueException("o4-9")
    }?.let {
      aap.answers[Field.EDUCATION_ACADEMIC_PROFESSIONAL_VOCATIONAL_QUALIFICATIONS.lower] = SingleValue(it.name)
    }
  }
}
