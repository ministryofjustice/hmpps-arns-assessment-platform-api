package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LocalDateTimeExtensionsTest {

  @Test
  fun `truncates timestamps to PostgreSQL microsecond precision`() {
    val timestamp = LocalDateTime.parse("2026-10-05T08:36:22.719716868")

    assertEquals(
      LocalDateTime.parse("2026-10-05T08:36:22.719716"),
      timestamp.toDatabasePrecision(),
    )
  }
}
