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
  private val clockProvider = ClockProvider(request)

  @Test
  fun `returns system clock when backdateTo parameter is null`() {
    every { request.getParameter("backdateTo") } returns null

    val clock = clockProvider.clock()

    assertNotNull(clock)
    assertEquals(Clock.systemDefaultZone().zone, clock.zone)
  }

  @Test
  fun `returns offset clock when backdateTo is in the past`() {
    val pastTime = LocalDateTime.now().minusDays(1)
    every { request.getParameter("backdateTo") } returns pastTime.toString()

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

    val exception = assertThrows(ClockException::class.java) {
      clockProvider.clock()
    }

    assertTrue(exception.message.contains("Invalid backdateTo parameter"))
  }
}
