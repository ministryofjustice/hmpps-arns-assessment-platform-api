package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Option(
  val value: String? = null,
)
