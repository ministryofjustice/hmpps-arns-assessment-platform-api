package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated

@JsonTypeName("CREATE_ASSESSMENT")
class CreateAssessment : Command {
  fun toEvent() = AssessmentCreated()
}
