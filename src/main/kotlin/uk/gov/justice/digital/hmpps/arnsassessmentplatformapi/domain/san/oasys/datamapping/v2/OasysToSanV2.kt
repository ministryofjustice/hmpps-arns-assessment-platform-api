package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.OasysToSanMappingVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san.Accommodation

val OasysToSanV2: OasysToSanMappingVersion = "v2.0" to setOf(
  Accommodation(),
)
