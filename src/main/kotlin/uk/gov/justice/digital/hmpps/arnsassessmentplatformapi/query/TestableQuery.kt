package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import java.time.LocalDateTime

data class TestableQuery(
  override val timestamp: LocalDateTime? = null,
  val param: String = "",
) : Query
