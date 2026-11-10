package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent

typealias OasysToSanMappingVersion = Pair<String, Set<SanSectionMapping>>

abstract class SanSectionMapping {
  abstract fun map(oasysEquivalent: OasysEquivalent, assessmentAggregate: AssessmentAggregate)
}
