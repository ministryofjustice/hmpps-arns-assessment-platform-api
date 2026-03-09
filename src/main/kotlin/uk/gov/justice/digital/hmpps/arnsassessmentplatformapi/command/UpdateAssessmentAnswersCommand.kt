package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import java.time.LocalDateTime

data class UpdateAssessmentAnswersCommand(
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  val added: Map<String, Value>,
  val removed: List<String>,
  override val timeline: Timeline? = null,
) : RequestableCommand {
  override lateinit var receivedOn: LocalDateTime
}
