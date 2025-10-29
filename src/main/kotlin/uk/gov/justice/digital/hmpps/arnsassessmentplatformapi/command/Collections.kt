package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class CreateCollection(
  val name: String,
  override val user: User,
  override val assessmentUuid: UUID,
) : RequestableCommand

data class CreateChildCollection(
  val name: String,
  val parentCollectionUuid: UUID,
  override val user: User,
  override val assessmentUuid: UUID,
) : RequestableCommand

data class AddCollectionItem(
  val collectionUuid: UUID,
  val answers: Map<String, List<String>>,
  val index: Int?,
  override val user: User,
  override val assessmentUuid: UUID,
) : RequestableCommand

data class UpdateCollectionItem(
  val collectionUuid: UUID,
  val index: Int,
  val added: Map<String, List<String>>,
  val removed: List<String>,
  override val user: User,
  override val assessmentUuid: UUID,
) : RequestableCommand

data class RemoveCollectionItem(
  val collectionUuid: UUID,
  val index: Int,
  override val user: User,
  override val assessmentUuid: UUID,
) : RequestableCommand

data class ReorderCollectionItem(
  val collectionUuid: UUID,
  val index: Int,
  val previousIndex: Int,
  override val user: User,
  override val assessmentUuid: UUID,
) : RequestableCommand
