package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e.dto

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails

data class AssessmentRequestDto(
  val commands: List<Command>)
data class Command(
  val type: String,
  val assessmentType: String,
  val formVersion: String,
  val properties: Map<String, String>,
  val user: UserDetails,
  val CRN: String,
  val flags: List<String> = emptyList())
