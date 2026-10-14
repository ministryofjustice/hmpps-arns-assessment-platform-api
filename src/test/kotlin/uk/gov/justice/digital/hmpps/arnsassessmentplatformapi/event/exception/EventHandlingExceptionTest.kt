package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.PropertyNotFoundException
import java.util.UUID

class EventHandlingExceptionTest {
  @Test
  fun `uses the developer message of a wrapped platform exception`() {
    val aggregateUuid = UUID.randomUUID()
    val cause = PropertyNotFoundException("foo", aggregateUuid)

    val exception = EventHandlingException(UUID.randomUUID(), "SomeEvent", "SomeHandler", cause)

    assertThat(exception.developerMessage).isEqualTo("The property \"foo\" could not be found on aggregate \"$aggregateUuid\"")
  }

  @Test
  fun `uses the message of any other wrapped exception`() {
    val exception = EventHandlingException(UUID.randomUUID(), "SomeEvent", "SomeHandler", IllegalStateException("boom"))

    assertThat(exception.developerMessage).isEqualTo("boom")
  }
}
