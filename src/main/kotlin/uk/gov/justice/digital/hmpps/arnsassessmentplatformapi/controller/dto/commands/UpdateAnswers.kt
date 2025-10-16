package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

@JsonTypeName("UPDATE_ANSWERS")
data class UpdateAnswers(
  override val user: User,
  override val assessmentUuid: UUID,
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : RequestableCommand
