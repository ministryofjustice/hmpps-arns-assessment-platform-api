package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("FORM_VERSION_UPDATED")
data class FormVersionUpdated(
  val version: String,
) : Event
