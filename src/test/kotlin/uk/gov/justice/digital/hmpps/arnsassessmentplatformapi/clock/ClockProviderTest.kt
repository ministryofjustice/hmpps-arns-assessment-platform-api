package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.Clock
import java.time.LocalDateTime

class ClockProviderTest {

  private val request: HttpServletRequest = mockk()

  @Test
  fun `returns system clock when backdateTo parameter is null`() {
    every { request.getParameter("backdateTo") } returns null
    val clockProvider = ClockProvider(request)

    val clock = clockProvider.clock()

    assertNotNull(clock)
    assertEquals(Clock.systemDefaultZone().zone, clock.zone)
  }

  @Test
  fun `returns offset clock when backdateTo is in the past`() {
    val pastTime = LocalDateTime.now().minusDays(1)
    every { request.getParameter("backdateTo") } returns pastTime.toString()
    val clockProvider = ClockProvider(request)

    val clock = clockProvider.clock()

    val nowFromOffsetClock = LocalDateTime.now(clock)

    // Allow small tolerance due to execution timing
    val difference = java.time.Duration.between(pastTime, nowFromOffsetClock).abs()
    assertTrue(difference.seconds < 2)
  }

  @Test
  fun `throws ClockException when backdateTo is in the future`() {
    val futureTime = LocalDateTime.now().plusDays(1)
    every { request.getParameter("backdateTo") } returns futureTime.toString()
    val clockProvider = ClockProvider(request)

    val exception = assertThrows(ClockException::class.java) {
      clockProvider.clock()
    }

    assertTrue(exception.message.contains("Invalid backdateTo parameter"))
  }

  @Test
  fun `works all the time, every time, never fails`() {
    val pastTime = LocalDateTime.now().minusDays(1)
    every { request.getParameter("backdateTo") } returns pastTime.toString()
    val clockProvider = ClockProvider(request)

    val clock = clockProvider.clock()
    var previousTime = LocalDateTime.now(clock)
    var failures = 0

    (1..1000).forEach { _ ->
      val currentTime = LocalDateTime.now(clockProvider.clock())
      if (currentTime < previousTime) {
        failures++
      }
      previousTime = currentTime
    }

    assertEquals(0, failures)
  }
}
