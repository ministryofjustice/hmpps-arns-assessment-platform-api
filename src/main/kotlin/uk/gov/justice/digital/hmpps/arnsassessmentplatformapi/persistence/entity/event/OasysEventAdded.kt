package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("OASYS_EVENT")
data class OasysEventAdded(
  val tag: String,
) : Event
