package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import java.time.LocalDateTime

class RollbackAssessment(
  val pointInTime: LocalDateTime,
) : Command
