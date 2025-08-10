package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("UPDATE_FORM_VERSION")
class UpdateFormVersion(
  val version: String,
) : Command
