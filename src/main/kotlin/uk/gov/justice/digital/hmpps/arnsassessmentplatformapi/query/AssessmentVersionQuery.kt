package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.time.LocalDateTime
import java.util.UUID

data class AssessmentVersionQuery(
  override val user: User,
  override val assessmentUuid: UUID,
  val timestamp: LocalDateTime? = null,
) : RequestableQuery
