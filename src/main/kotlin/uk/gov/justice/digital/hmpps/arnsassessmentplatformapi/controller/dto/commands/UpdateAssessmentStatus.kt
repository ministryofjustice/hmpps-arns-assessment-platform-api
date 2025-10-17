package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

@JsonTypeName("UPDATE_ASSESSMENT_STATUS")
data class UpdateAssessmentStatus(
  override val user: User,
  override val assessmentUuid: UUID,
  val status: String,
) : RequestableCommand
