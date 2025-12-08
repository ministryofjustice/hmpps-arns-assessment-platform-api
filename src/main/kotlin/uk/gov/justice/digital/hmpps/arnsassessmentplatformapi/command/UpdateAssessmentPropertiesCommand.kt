package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class UpdateAssessmentPropertiesCommand(
  override val user: User,
  override val assessmentUuid: UUID,
  val added: Map<String, Value>,
  val removed: List<String>,
  override val timeline: Timeline? = null,
) : RequestableCommand
