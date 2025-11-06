package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class CreateAssessmentCommand(
  override val user: User,
  val properties: Map<String, List<String>>,
) : RequestableCommand {
  @JsonIgnore
  override val assessmentUuid: UUID = UUID.randomUUID()
}
