package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value

data class UpdateAssessmentPropertiesCommand(
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  val added: Map<String, Value>,
  val removed: List<String>,
  override val timeline: Timeline? = null,
) : RequestableCommand
