package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.FormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import java.util.UUID

data class CreateAssessmentCommand(
  override val user: UserDetails,
  val formVersion: FormVersion,
  val assessmentType: String,
  val identifiers: Map<IdentifierType, String>? = null,
  val properties: Map<String, Value>? = null,
  val flags: List<String> = emptyList(),
  override val timeline: Timeline? = null,
) : RequestableCommand {
  @JsonIgnore
  override val assessmentUuid: Reference = UUID.randomUUID().toReference()
}
