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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import java.time.LocalDateTime

class EventServiceTest {
  val eventRepository: EventRepository = mockk()
  val service = EventService(
    eventRepository = eventRepository,
  )
  val now = LocalDateTime.now()
  val assessment = AssessmentEntity(type = "TEST", createdAt = now)
  val user = UserDetailsEntity(userId = "FOO_USER", displayName = "Foo User", authSource = AuthSource.HMPPS_AUTH)

  val events = listOf(
    EventEntity(
      user = user,
      assessment = assessment,
      createdAt = now,
      data = AssessmentCreatedEvent(formVersion = "1", properties = emptyMap()),
    ),
    EventEntity(
      user = user,
      assessment = assessment,
      createdAt = now,
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
      every { eventRepository.findAllByAssessmentUuidAndCreatedAtIsLessThanEqual(assessment.uuid, pointInTime) } returns events

      val result = service.findAllForPointInTime(assessment.uuid, pointInTime)
      assertThat(result).isEqualTo(events)
    }

    @Test
    fun `it returns empty when no events found`() {
      val pointInTime = LocalDateTime.parse("2025-01-01T12:00:00")
      every { eventRepository.findAllByAssessmentUuidAndCreatedAtIsLessThanEqual(assessment.uuid, pointInTime) } returns emptyList()

      val result = service.findAllForPointInTime(assessment.uuid, pointInTime)
      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class Save {
    @Test
    fun `it saves events`() {
      every { eventRepository.save(any<EventEntity<Event>>()) } answers { firstArg() }

      service.save(events.first())
      verify(exactly = 1) { eventRepository.save(events.first()) }
    }
  }

  @Nested
  inner class SoftDelete {
    @Test
    fun `should mark matching events as deleted and save them`() {
      val from = now.minusHours(1)

      every { eventRepository.findAllByAssessmentUuidAndCreatedAtGreaterThanEqual(assessment.uuid, from) } returns events
      every { eventRepository.saveAll(any<List<EventEntity<*>>>()) } answers { firstArg() }

      service.softDelete(assessment.uuid, from)

      verify(exactly = 1) { eventRepository.findAllByAssessmentUuidAndCreatedAtGreaterThanEqual(assessment.uuid, from) }
      verify(exactly = 1) { eventRepository.saveAll(events) }
      events.forEach { assertThat(it.deleted).isTrue() }
    }

    @Test
    fun `should save an empty list when no events match`() {
      val from = now.minusHours(1)

      every { eventRepository.findAllByAssessmentUuidAndCreatedAtGreaterThanEqual(assessment.uuid, from) } returns emptyList()
      every { eventRepository.saveAll(any<List<EventEntity<*>>>()) } answers { firstArg() }

      service.softDelete(assessment.uuid, from)

      verify(exactly = 1) { eventRepository.saveAll(emptyList()) }
    }
  }

  @Nested
  inner class FindAssessmentsSoftDeletedSince {
    @Test
    fun `returns assessments soft deleted since the given timestamp`() {
      val since = LocalDateTime.now()
      val type = "SENTENCE_PLAN"
      val assessments = listOf(
        AssessmentEntity(type = type, createdAt = now.plusMinutes(2)),
        AssessmentEntity(type = type, createdAt = now.plusMinutes(1)),
      )

      every { eventRepository.findAssessmentsSoftDeletedSince(type, since) } returns assessments

      val result = service.findAssessmentsSoftDeletedSince(type, since)

      assertThat(result).isEqualTo(assessments)
      verify(exactly = 1) { eventRepository.findAssessmentsSoftDeletedSince(type, since) }
    }
  }
}
