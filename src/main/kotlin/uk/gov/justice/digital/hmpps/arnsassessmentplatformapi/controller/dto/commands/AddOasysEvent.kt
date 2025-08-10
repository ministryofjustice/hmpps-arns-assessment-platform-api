package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("OASYS_EVENT")
class AddOasysEvent(
  val tag: String,
) : Command
