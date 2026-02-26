package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock

import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class Clock(
  private val clockProvider: ClockProvider,
) {
  fun now(): LocalDateTime = LocalDateTime.now(clockProvider.clock())
}
