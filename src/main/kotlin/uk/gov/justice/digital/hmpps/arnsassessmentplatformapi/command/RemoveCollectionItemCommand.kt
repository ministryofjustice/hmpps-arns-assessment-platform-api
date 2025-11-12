package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class RemoveCollectionItemCommand(
  val collectionItemUuid: UUID,
  override val user: User,
  override val assessmentUuid: UUID,
  override val timeline: CommandTimeline? = null,
) : RequestableCommand
