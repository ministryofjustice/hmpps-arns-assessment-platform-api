package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime
import java.util.UUID

data class CollectionItemQuery(
  override val user: UserDetails,
  override val assessmentIdentifier: AssessmentIdentifier,
  override val timestamp: LocalDateTime? = null,
  val collectionItemUuid: UUID,
  val depth: Int = 0,
) : RequestableQuery
