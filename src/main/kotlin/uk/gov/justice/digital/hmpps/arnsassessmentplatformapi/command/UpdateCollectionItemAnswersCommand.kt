package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class UpdateCollectionItemAnswersCommand(
  val collectionItemUuid: UUID,
  @Schema(ref = "#/components/schemas/Answers")
  val added: Map<String, Any>,
  val removed: List<String>,
  override val user: User,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand
