package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.util.UUID

data class ReorderCollectionItemCommand(
  val collectionItemUuid: UUID,
  val index: Int,
  override val user: UserDetails,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand
