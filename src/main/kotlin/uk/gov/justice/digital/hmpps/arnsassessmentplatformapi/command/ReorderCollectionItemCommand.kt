package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails

data class ReorderCollectionItemCommand(
  val collectionItemUuid: Reference,
  val index: Int,
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  override val timeline: Timeline? = null,
) : RequestableCommand
