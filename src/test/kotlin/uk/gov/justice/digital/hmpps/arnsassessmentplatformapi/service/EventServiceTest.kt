package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.CollectionEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime

class EventServiceTest {
  val eventRepository: EventRepository = mockk()
  val service = EventService(
    eventRepository = eventRepository,
  )
  val assessment = CollectionEntity()
  val user = User("FOO_USER", "Foo User")

  val events = listOf(
    EventEntity(
      user = user,
      collection = assessment,
      data = AssessmentCreatedEvent(),
    ),
    EventEntity(
      user = user,
      collection = assessment,
      data = AnswersUpdatedEvent(
        added = mapOf("foo" to listOf("foo_value")),
        removed = emptyList(),
      ),
    ),
  )

  @Nested
  inner class FindAllByCollectionUuid {
    @Test
    fun `it returns all events for an assessment`() {
      every { eventRepository.findAllByCollectionUuid(assessment.uuid) } returns events

      val result = service.findAllByCollectionUuid(assessment.uuid)
      assertThat(result).isEqualTo(events)
    }

    @Test
    fun `it returns empty when no events found`() {
      every { eventRepository.findAllByCollectionUuid(assessment.uuid) } returns emptyList()

      val result = service.findAllByCollectionUuid(assessment.uuid)
      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class FindAllByCollectionUuidAndCreatedAtBefore {
    @Test
    fun `it returns all events for an assessment before a provided timestamp`() {
      val pointInTime = LocalDateTime.parse("2025-01-01T12:00:00")
      every { eventRepository.findAllByCollectionUuidAndCreatedAtBefore(assessment.uuid, pointInTime) } returns events

      val result = service.findAllByCollectionUuidAndCreatedAtBefore(assessment.uuid, pointInTime)
      assertThat(result).isEqualTo(events)
    }

    @Test
    fun `it returns empty when no events found`() {
      val pointInTime = LocalDateTime.parse("2025-01-01T12:00:00")
      every { eventRepository.findAllByCollectionUuidAndCreatedAtBefore(assessment.uuid, pointInTime) } returns emptyList()

      val result = service.findAllByCollectionUuidAndCreatedAtBefore(assessment.uuid, pointInTime)
      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class SaveAll {
    @Test
    fun `it saves events`() {
      every { eventRepository.saveAll(any<List<EventEntity>>()) } answers { firstArg() }

      service.saveAll(events)
      verify(exactly = 1) { eventRepository.saveAll(events) }
    }
  }
}
