package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Component
@RequestScope
class ClockProvider(
  private val request: HttpServletRequest,
) {
  lateinit var clock: Clock
  init {
      clock = createClock()
  }
  fun clock(): Clock = clock
  fun createClock(): Clock {
    val baseClock = Clock.systemDefaultZone()

    val backdateTo = request.getParameter("backdateTo")
      ?.let(LocalDateTime::parse)
      ?.atZone(baseClock.zone)
      ?.toInstant()

    val offset = backdateTo?.let {
      Duration.between(baseClock.instant(), it)
    }

    if (offset?.isPositive == true) {
      throw ClockException("Invalid backdateTo parameter '$backdateTo' - the clock cannot be moved forward")
    }

    return offset?.let { Clock.offset(baseClock, it) } ?: baseClock
  }
}
