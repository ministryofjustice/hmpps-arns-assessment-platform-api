package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreated
import java.time.LocalDateTime

class EventServiceTest {
  val eventRepository: EventRepository = mockk()
  val service = EventService(
    eventRepository = eventRepository,
  )
  val assessment = AssessmentEntity()
  val user = User("FOO_USER", "Foo User")

  val events = listOf(
    EventEntity(
      user = user,
      assessment = assessment,
      data = AssessmentCreated(),
    ),
    EventEntity(
      user = user,
      assessment = assessment,
      data = AnswersUpdated(
        added = mapOf("foo" to listOf("foo_value")),
        removed = emptyList(),
      ),
    ),
  )

  @Nested
  inner class FindAllByAssessmentUuid {
    @Test
    fun `it returns all events for an assessment`() {
      every { eventRepository.findAllByAssessmentUuid(assessment.uuid) } returns events

      val result = service.findAllByAssessmentUuid(assessment.uuid)
      assertThat(result).isEqualTo(events)
    }

    @Test
    fun `it returns empty when no events found`() {
      every { eventRepository.findAllByAssessmentUuid(assessment.uuid) } returns emptyList()

      val result = service.findAllByAssessmentUuid(assessment.uuid)
      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class FindAllByAssessmentUuidAndCreatedAtBefore {
    @Test
    fun `it returns all events for an assessment before a provided timestamp`() {
      val pointInTime = LocalDateTime.parse("2025-01-01T12:00:00")
      every { eventRepository.findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, pointInTime) } returns events

      val result = service.findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, pointInTime)
      assertThat(result).isEqualTo(events)
    }

    @Test
    fun `it returns empty when no events found`() {
      val pointInTime = LocalDateTime.parse("2025-01-01T12:00:00")
      every { eventRepository.findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, pointInTime) } returns emptyList()

      val result = service.findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, pointInTime)
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
