package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import java.time.LocalDateTime

class TestableQuery(
  override val timestamp: LocalDateTime? = null,
) : Query
