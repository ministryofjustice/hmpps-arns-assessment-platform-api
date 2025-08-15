package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.FormVersionUpdated

@JsonTypeName("UPDATE_FORM_VERSION")
class UpdateFormVersion(
  val version: String,
) : Command {
  override fun toEvent() = FormVersionUpdated(version = version)
}
