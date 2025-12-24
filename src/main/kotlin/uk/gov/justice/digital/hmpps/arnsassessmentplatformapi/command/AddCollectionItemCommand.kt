package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import java.util.UUID

data class AddCollectionItemCommand(
  val collectionUuid: UUID,
  val answers: Map<String, Value>,
  val properties: Map<String, Value>,
  val index: Int?,
  override val user: User,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand {
  @JsonIgnore
  val collectionItemUuid: UUID = UUID.randomUUID()
}
