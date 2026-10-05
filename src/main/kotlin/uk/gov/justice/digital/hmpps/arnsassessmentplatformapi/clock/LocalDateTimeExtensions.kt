package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// PostgreSQL timestamp values are stored with microsecond precision.
fun LocalDateTime.toDatabasePrecision(): LocalDateTime = truncatedTo(ChronoUnit.MICROS)
