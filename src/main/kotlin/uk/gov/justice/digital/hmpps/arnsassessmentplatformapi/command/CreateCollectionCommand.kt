package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.util.UUID

data class CreateCollectionCommand(
  val name: String,
  val parentCollectionItemUuid: Reference?,
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  override val timeline: Timeline? = null,
) : RequestableCommand {
  @JsonIgnore
  val collectionUuid: UUID = UUID.randomUUID()
}
