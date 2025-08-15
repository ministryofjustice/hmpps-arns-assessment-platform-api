package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.OasysEventAdded

@JsonTypeName("OASYS_EVENT")
class AddOasysEvent(
  val tag: String,
) : Command {
  override fun toEvent() = OasysEventAdded(tag = tag)
}
