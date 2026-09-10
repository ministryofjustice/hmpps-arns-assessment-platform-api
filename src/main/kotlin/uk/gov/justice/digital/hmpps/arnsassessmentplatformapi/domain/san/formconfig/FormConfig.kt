package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class FormConfig(
  val version: String,
  val fields: Map<String, Field> = emptyMap(),
)
