package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

class CreatedByResolverTest {
  val user = User("TEST_USER", "Test User")
  val assessment = AssessmentEntity(
    type = "Test",
  )

  @Nested
  inner class From {
    @Test
    fun `it returns CREATED when the event is AssessmentCreatedEvent`() {
      val result = CreatedByResolver.from(
        event = EventEntity(
          user = user,
          assessment = assessment,
          data = AssessmentCreatedEvent(
            formVersion = "v1.0",
            properties = emptyMap(),
            timeline = null,
          ),
        ),
      )

      assertEquals(CreatedBy.CREATED, result)
    }
  }

  @Test
  fun `it returns CLONED when the event is AssessmentPropertiesUpdatedEvent and the property 'status' is 'CLONED'`() {
    val result = CreatedByResolver.from(
      event = EventEntity(
        user = user,
        assessment = assessment,
        data = AssessmentPropertiesUpdatedEvent(
          added = mapOf("STATUS" to SingleValue("CLONED")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
    )

    assertEquals(CreatedBy.CLONED, result)
  }

  @Test
  fun `it returns CLONED when the event is AssessmentPropertiesUpdatedEvent and the property 'status' is not 'CLONED'`() {
    val result = CreatedByResolver.from(
      event = EventEntity(
        user = user,
        assessment = assessment,
        data = AssessmentPropertiesUpdatedEvent(
          added = mapOf("STATUS" to SingleValue("SOME_OTHER_STATUS")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
    )

    assertEquals(CreatedBy.DAILY_EDIT, result)
  }

  @Test
  fun `it returns DAILY_EDIT when the event is PropertiesUpdatedEvent and there is no status property`() {
    val result = CreatedByResolver.from(
      event = EventEntity(
        user = user,
        assessment = assessment,
        data = AssessmentPropertiesUpdatedEvent(
          added = emptyMap(),
          removed = emptyList(),
          timeline = null,
        ),
      ),
    )

    assertEquals(CreatedBy.DAILY_EDIT, result)
  }

  @Test
  fun `it returns DAILY_EDIT for other events`() {
    val result = CreatedByResolver.from(
      event = EventEntity(
        user = user,
        assessment = assessment,
        data = AssessmentAnswersUpdatedEvent(
          added = emptyMap(),
          removed = emptyList(),
          timeline = null,
        ),
      ),
    )

    assertEquals(CreatedBy.DAILY_EDIT, result)
  }
}
