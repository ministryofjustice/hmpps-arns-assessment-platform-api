package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.TimelineRepository
import java.time.LocalDateTime
import java.util.UUID

class TimelineServiceTest {
  val timelineRepository: TimelineRepository = mockk()
  val service = TimelineService(
    timelineRepository = timelineRepository,
  )

  val now: LocalDateTime = LocalDateTime.now()
  val assessment = AssessmentEntity(type = "TEST", createdAt = now)
  val user = UserDetailsEntity(userId = "FOO_USER", displayName = "Foo User", authSource = AuthSource.HMPPS_AUTH)

  val timelineEntries = listOf(
    TimelineEntity(
      createdAt = now,
      user = user,
      assessment = assessment,
      eventType = "AssessmentAnswersUpdatedEvent",
      data = mapOf("added" to listOf("foo")),
    ),
    TimelineEntity(
      createdAt = now.plusMinutes(5),
      user = user,
      assessment = assessment,
      eventType = "AssessmentPropertiesUpdatedEvent",
      data = mapOf("updated" to listOf("bar")),
    ),
  )

  @Nested
  inner class FindAllIncludingDeleted {
    @Test
    fun `returns all timeline entries for an assessment including deleted entries`() {
      every { timelineRepository.findAllIncludingDeleted(assessment.uuid) } returns timelineEntries

      val result = service.findAllIncludingDeleted(assessment.uuid)

      assertThat(result).isEqualTo(timelineEntries)
      verify(exactly = 1) { timelineRepository.findAllIncludingDeleted(assessment.uuid) }
    }
  }

  @Nested
  inner class FindByUuidsIncludingDeleted {
    @Test
    fun `returns all timeline entries matching the requested UUIDs including deleted entries`() {
      val timelineUuids = setOf(UUID.randomUUID(), UUID.randomUUID())
      every { timelineRepository.findByUuidsIncludingDeleted(timelineUuids) } returns timelineEntries

      val result = service.findByUuidsIncludingDeleted(timelineUuids)

      assertThat(result).isEqualTo(timelineEntries)
      verify(exactly = 1) { timelineRepository.findByUuidsIncludingDeleted(timelineUuids) }
    }
  }

  @Nested
  inner class SoftDelete {
    @Test
    fun `should mark matching timeline entries as deleted and save them`() {
      val from = now.minusHours(1)

      every { timelineRepository.findByAssessmentUuidAndCreatedAtGreaterThanEqual(assessment.uuid, from) } returns timelineEntries
      every { timelineRepository.saveAll(any<List<TimelineEntity>>()) } answers { firstArg() }

      service.softDelete(assessment.uuid, from)

      verify(exactly = 1) { timelineRepository.findByAssessmentUuidAndCreatedAtGreaterThanEqual(assessment.uuid, from) }
      verify(exactly = 1) { timelineRepository.saveAll(timelineEntries) }
      timelineEntries.forEach { assertThat(it.deleted).isTrue() }
    }

    @Test
    fun `should save an empty list when no timeline entries match`() {
      val from = now.minusHours(1)

      every { timelineRepository.findByAssessmentUuidAndCreatedAtGreaterThanEqual(assessment.uuid, from) } returns emptyList()
      every { timelineRepository.saveAll(any<List<TimelineEntity>>()) } answers { firstArg() }

      service.softDelete(assessment.uuid, from)

      verify(exactly = 1) { timelineRepository.saveAll(emptyList()) }
    }
  }

  @Nested
  inner class HardDelete {
    @Test
    fun `deletes the supplied timeline entries`() {
      every { timelineRepository.deleteAll(timelineEntries) } returns Unit

      service.hardDelete(timelineEntries)

      verify(exactly = 1) { timelineRepository.deleteAll(timelineEntries) }
    }
  }
}
