package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class AddCollectionItemCommand(
  val collectionUuid: UUID,
  val answers: Map<String, List<String>>,
  val properties: Map<String, List<String>>,
  val index: Int?,
  override val user: User,
  override val assessmentUuid: UUID,
  override val timeline: CommandTimeline? = null,
) : RequestableCommand {
  @JsonIgnore
  val collectionItemUuid: UUID = UUID.randomUUID()
}
