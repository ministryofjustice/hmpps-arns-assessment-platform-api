package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.hook

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FormConfig
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.HookType

@HookType("UpdateOasysDataMapping")
data class UpdateOasysDataMapping(
  val formConfig: FormConfig,
) : Hook
