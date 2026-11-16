package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook

data class ClearAssessmentCommand(
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  val properties: List<String> = emptyList(),
  val collections: List<String> = emptyList(),
  override val timeline: Timeline? = null,
  override val hooks: List<Hook>? = null,
  override val autosaved: Boolean = false,
) : RequestableCommand
