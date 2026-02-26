package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails

data class UpdateFormVersionCommand(
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  val version: String,
  override val timeline: Timeline? = null,
) : RequestableCommand
