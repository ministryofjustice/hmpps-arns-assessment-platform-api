package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

@JsonTypeName("CREATE_ASSESSMENT")
data class CreateAssessmentCommand(
  override val user: User,
) : RequestableCommand {
  @JsonIgnore
  override val assessmentUuid: UUID = UUID.randomUUID()
}
