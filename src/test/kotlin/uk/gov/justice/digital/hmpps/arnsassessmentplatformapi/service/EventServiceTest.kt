package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.time.LocalDateTime

class EventServiceTest {
  val eventRepository: EventRepository = mockk()
  val service = EventService(
    eventRepository = eventRepository,
  )
  val assessment = AssessmentEntity(type = "TEST")
  val user = UserDetailsEntity(userId = "FOO_USER", displayName = "Foo User", authSource = AuthSource.HMPPS_AUTH)

  val events = listOf(
    EventEntity(
      user = user,
      assessment = assessment,
      data = AssessmentCreatedEvent(formVersion = "1", properties = emptyMap()),
    ),
    EventEntity(
      user = user,
      assessment = assessment,
      data = AssessmentAnswersUpdatedEvent(
        added = mapOf("foo" to SingleValue("foo_value")),
        removed = emptyList(),
      ),
    ),
  )

  @Nested
  inner class FindAllByAssessmentUuidAndCreatedAtBefore {
    @Test
    fun `it returns all events for an assessment before a provided timestamp`() {
      val pointInTime = LocalDateTime.parse("2025-01-01T12:00:00")
      every { eventRepository.findAllByAssessmentUuidAndCreatedAtIsLessThanEqualAndParentIsNull(assessment.uuid, pointInTime) } returns events

      val result = service.findAllForPointInTime(assessment.uuid, pointInTime)
      assertThat(result).isEqualTo(events)
    }

    @Test
    fun `it returns empty when no events found`() {
      val pointInTime = LocalDateTime.parse("2025-01-01T12:00:00")
      every { eventRepository.findAllByAssessmentUuidAndCreatedAtIsLessThanEqualAndParentIsNull(assessment.uuid, pointInTime) } returns emptyList()

      val result = service.findAllForPointInTime(assessment.uuid, pointInTime)
      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class SaveAll {
    @Test
    fun `it saves events`() {
      every { eventRepository.save(any<EventEntity<Event>>()) } answers { firstArg() }

      service.save(events.first())
      verify(exactly = 1) { eventRepository.save(events.first()) }
    }
  }
}
