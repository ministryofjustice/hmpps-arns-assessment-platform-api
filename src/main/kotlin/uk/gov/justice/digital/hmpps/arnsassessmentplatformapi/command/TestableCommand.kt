package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

class TestableCommand(
  override val timeline: CommandTimeline? = null,
) : Command
