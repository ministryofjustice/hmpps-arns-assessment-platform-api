package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.util.UUID

data class RemoveCollectionItemCommand(
  val collectionItemUuid: UUID,
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  override val timeline: Timeline? = null,
) : RequestableCommand
