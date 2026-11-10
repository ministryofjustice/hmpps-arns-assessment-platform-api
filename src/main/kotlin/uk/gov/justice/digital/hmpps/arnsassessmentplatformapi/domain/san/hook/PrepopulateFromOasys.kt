package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.hook

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.HookType

@HookType("PrepopulateFromOasys")
data class PrepopulateFromOasys(
  val data: OasysEquivalent,
) : Hook
