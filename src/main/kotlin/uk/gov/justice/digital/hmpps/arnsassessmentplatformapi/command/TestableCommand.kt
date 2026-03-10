package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

data class TestableCommand(
  override val timeline: Timeline? = null,
  val param: String = "",
) : Command
