package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * PostgreSQL persists timestamps at microsecond precision, rounding fractional
 * microseconds to the nearest microsecond.
 */
fun LocalDateTime.toDatabasePrecision(): LocalDateTime =
  plusNanos(500).truncatedTo(ChronoUnit.MICROS)