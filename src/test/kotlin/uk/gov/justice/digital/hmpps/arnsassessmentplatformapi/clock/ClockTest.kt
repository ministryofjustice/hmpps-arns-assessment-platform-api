package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ClockTest {

  private val clockProvider: ClockProvider = mockk()
  private val clock = Clock(clockProvider)

  @Test
  fun `now and request date time use microsecond precision`() {
    val fixedInstant = Instant.parse("2024-01-01T10:00:00.719716868Z")
    val zone = ZoneId.systemDefault()
    val fixedClock = Clock.fixed(fixedInstant, zone)
    val requestDateTime = LocalDateTime.ofInstant(fixedInstant, zone)

    every { clockProvider.clock() } returns fixedClock
    every { clockProvider.requestDateTime } returns requestDateTime

    val expected = LocalDateTime.ofInstant(fixedInstant, zone).toDatabasePrecision()

    assertEquals(expected, clock.now())
    assertEquals(expected, clock.requestDateTime())
    verify(exactly = 1) { clockProvider.clock() }
  }
}
