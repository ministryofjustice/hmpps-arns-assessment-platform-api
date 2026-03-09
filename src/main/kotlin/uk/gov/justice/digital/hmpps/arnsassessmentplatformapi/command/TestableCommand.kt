package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import java.time.LocalDateTime

data class TestableCommand(
  override val timeline: Timeline? = null,
  val param: String = "",
  override var receivedOn: LocalDateTime = LocalDateTime.now(),
) : Command
