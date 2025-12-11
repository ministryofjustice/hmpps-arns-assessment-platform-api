package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.FormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class CreateAssessmentCommand(
  override val user: User,
  val formVersion: FormVersion,
  val properties: Map<String, Value>? = null,
  override val timeline: Timeline? = null,
) : RequestableCommand {
  @JsonIgnore
  override val assessmentUuid: UUID = UUID.randomUUID()
}
