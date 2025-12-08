package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class AddCollectionItemCommand(
  val collectionUuid: UUID,
  @Schema(ref = "#/components/schemas/Answers")
  val answers: Map<String, Any>,
  val properties: Map<String, List<String>>,
  val index: Int?,
  override val user: User,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand {
  @JsonIgnore
  val collectionItemUuid: UUID = UUID.randomUUID()
}
