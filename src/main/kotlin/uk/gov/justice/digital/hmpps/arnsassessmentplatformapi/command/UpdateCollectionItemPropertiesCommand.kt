package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value

data class UpdateCollectionItemPropertiesCommand(
  val collectionItemUuid: Reference,
  val added: Map<String, Value>,
  val removed: List<String>,
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  override val timeline: Timeline? = null,
) : RequestableCommand
