package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Component
class ClockProvider(
  private val request: HttpServletRequest,
) {
  fun clock(): Clock {
    val backdateTo: LocalDateTime? = request.getParameter("backdateTo")?.run(LocalDateTime::parse)
    val offset = backdateTo?.let { Duration.between(LocalDateTime.now(), backdateTo) }

    if (offset?.isPositive ?: false) {
      throw ClockException("Invalid backdateTo parameter '$backdateTo' - the clock cannot be moved forward")
    }

    return offset?.let { Clock.offset(Clock.systemDefaultZone(), it) } ?: Clock.systemDefaultZone()
  }
}
