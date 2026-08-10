package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails

data class UpdateFlagsCommand(
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  val flags: List<String>,
  override val timeline: Timeline? = null,
) : RequestableCommand
