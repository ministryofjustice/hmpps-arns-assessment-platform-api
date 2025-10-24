package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

sealed interface RequestableQuery : Query {
  val user: User
  val collectionUuid: UUID
}
