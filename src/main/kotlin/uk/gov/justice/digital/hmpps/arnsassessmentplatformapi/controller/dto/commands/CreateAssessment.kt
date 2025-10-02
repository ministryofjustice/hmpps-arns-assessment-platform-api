package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

@JsonTypeName("CREATE_ASSESSMENT")
class CreateAssessment(
  val user: User,
  val assessmentUuid: UUID = UUID.randomUUID(),
) : Command
