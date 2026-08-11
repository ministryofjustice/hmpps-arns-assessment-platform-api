package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import java.util.UUID

data class AddCollectionItemCommand(
  val collectionUuid: Reference,
  val answers: Map<String, Value>,
  val properties: Map<String, Value>,
  val index: Int?,
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  override val timeline: Timeline? = null,
  override val hooks: List<Hook>? = null,
) : RequestableCommand {
  @JsonIgnore
  val collectionItemUuid: UUID = UUID.randomUUID()
}
