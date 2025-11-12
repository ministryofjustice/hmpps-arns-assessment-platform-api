package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class CreateCollectionCommand(
  val name: String,
  val parentCollectionItemUuid: UUID?,
  override val user: User,
  override val assessmentUuid: UUID,
) : RequestableCommand {
  @JsonIgnore
  val collectionUuid: UUID = UUID.randomUUID()
}
