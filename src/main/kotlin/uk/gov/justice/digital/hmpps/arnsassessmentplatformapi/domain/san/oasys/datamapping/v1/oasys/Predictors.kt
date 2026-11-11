package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.AnswersProvider
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.FieldsToMap
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.OasysSectionMapping

class Predictors(ap: AnswersProvider) : OasysSectionMapping(ap) {
  override fun getFieldsToMap(): FieldsToMap = emptyMap()
}
