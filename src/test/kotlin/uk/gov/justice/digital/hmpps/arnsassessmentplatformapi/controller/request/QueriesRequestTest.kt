package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidQueryException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Timeframe
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class QueriesRequestTest {
  @Test
  fun `it creates`() {
    val query = AssessmentTimelineQuery(
      user = UserDetails("test-user", "Test User"),
      identifier = UuidIdentifier(UUID.randomUUID()),
      window = Timeframe(
        from = LocalDateTime.parse("2021-07-01T00:00:00"),
        to = Clock.now(),
      ),
      timestamp = Clock.now(),
    )

    val request = QueriesRequest(
      queries = listOf(query),
    )

    Assertions.assertThat(request.queries).contains(query)
  }

  @Test
  fun `it throws when passed no queries`() {
    val exception = assertThrows<InvalidQueryException> {
      QueriesRequest(
        queries = emptyList(),
      )
    }
    assertEquals("No queries received", exception.developerMessage)
    assertEquals("Unable to process queries", exception.message)
  }
}
