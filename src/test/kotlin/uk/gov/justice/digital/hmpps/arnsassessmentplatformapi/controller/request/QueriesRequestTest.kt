package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidQueryException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class QueriesRequestTest {
  @Test
  fun `it creates`() {
    val query = AssessmentTimelineQuery(
      user = User("FOO_USER", "Foo User"),
      assessmentUuid = UUID.randomUUID(),
      timestamp = LocalDateTime.now(),
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
