package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanToOasysMappingVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.oasys.Accommodation

val SanToOasysV2: SanToOasysMappingVersion = "v2.0" to setOf(
  ::Accommodation,
)
