package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.DataDeletionOperation
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.DataDeletionRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.EventUpdate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.TimelineUpdate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.RedactedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.EventNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.TimelineNotFoundException
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.LocalDateTime
import java.util.UUID

class DataDeletionServiceTest {
  private val assessmentService: AssessmentService = mockk()
  private val eventService: EventService = mockk()
  private val stateService: StateService = mockk()
  private val timelineService: TimelineService = mockk()
  private val auditService: AuditService = mockk()
  private val clock: Clock = mockk()
  private val authenticationHolder: HmppsAuthenticationHolder = mockk()

  private val service = DataDeletionService(
    assessmentService = assessmentService,
    eventService = eventService,
    stateService = stateService,
    timelineService = timelineService,
    auditService = auditService,
    clock = clock,
    authenticationHolder = authenticationHolder,
  )

  private val now: LocalDateTime = LocalDateTime.parse("2026-07-14T10:15:00")
  private val assessment = AssessmentEntity(
    uuid = UUID.fromString("87c7432d-bb00-49fa-a6a6-d91d83a6ca3b"),
    createdAt = now.minusDays(1),
    type = "TEST",
  )
  private val user = UserDetailsEntity(
    uuid = UUID.fromString("52f17021-8731-4e28-821b-5d886d1128fa"),
    userId = "USER1",
    displayName = "Test User",
    authSource = AuthSource.HMPPS_AUTH,
  )

  @Nested
  inner class GetData {
    @Test
    fun `returns all events and timeline items including soft-deleted data`() {
      val event = existingEvent(
        position = 1,
        data = AssessmentCreatedEvent(formVersion = "1", properties = emptyMap()),
      )
      val timeline = existingTimeline(
        position = 2,
        eventType = "AssessmentCreatedEvent",
        data = mapOf("formVersion" to "1"),
      )

      every { eventService.findAllIncludingDeleted(assessment.uuid) } returns listOf(event)
      every { timelineService.findAllIncludingDeleted(assessment.uuid) } returns listOf(timeline)

      val result = service.getData(assessment.uuid)

      assertThat(result.events).hasSize(1)
      assertThat(result.events.first().uuid).isEqualTo(event.uuid)
      assertThat(result.events.first().position).isEqualTo(1)
      assertThat(result.events.first().data).isEqualTo(event.data)

      assertThat(result.timeline).hasSize(1)
      assertThat(result.timeline.first().uuid).isEqualTo(timeline.uuid)
      assertThat(result.timeline.first().position).isEqualTo(2)
      assertThat(result.timeline.first().user).isEqualTo(User(user.uuid, user.displayName))
      assertThat(result.timeline.first().data).isEqualTo(timeline.data)
    }
  }

  @Nested
  inner class UpdateData {
    @Test
    fun `rewrites requested events and timelines, rebuilds state and audits non dry run changes`() {
      val eventToUpdate = existingEvent(
        position = 3,
        data = AssessmentCreatedEvent(formVersion = "1", properties = emptyMap()),
      )
      val eventToDelete = existingEvent(
        position = 4,
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("old" to SingleValue("value")),
          removed = emptyList(),
        ),
      )
      val timelineToUpdate = existingTimeline(
        position = 5,
        eventType = "AssessmentCreatedEvent",
        data = mapOf("old" to "timeline"),
        customType = "old-custom",
        customData = mapOf("oldCustom" to true),
      )
      val replacementEvent = AssessmentAnswersUpdatedEvent(
        added = mapOf("answer" to SingleValue("updated")),
        removed = listOf("old"),
      )
      val replacementTimeline = TimelineItem(
        uuid = timelineToUpdate.uuid,
        position = 99,
        timestamp = now.plusHours(1),
        user = User(UUID.randomUUID(), "Ignored User"),
        assessment = UUID.randomUUID(),
        event = "AssessmentAnswersUpdatedEvent",
        data = mapOf("new" to "timeline"),
        customType = "new-custom",
        customData = mapOf("newCustom" to true),
      )
      val request = DataDeletionRequest(
        dryRun = false,
        events = listOf(
          EventUpdate(eventToUpdate.uuid, DataDeletionOperation.UPDATE, replacementEvent),
          EventUpdate(eventToDelete.uuid, DataDeletionOperation.DELETE, eventToDelete.data),
        ),
        timeline = listOf(
          TimelineUpdate(timelineToUpdate.uuid, DataDeletionOperation.UPDATE, replacementTimeline),
        ),
      )
      val redactedAt = now.plusMinutes(30)
      val savedEvents = slot<List<EventEntity<*>>>()
      val savedTimelines = slot<List<TimelineEntity>>()

      every { assessmentService.findBy(assessment.uuid) } returns assessment
      every { timelineService.findByUuidsIncludingDeleted(setOf(timelineToUpdate.uuid)) } returns listOf(timelineToUpdate)
      every { eventService.findByUuidsIncludingDeleted(setOf(eventToUpdate.uuid, eventToDelete.uuid)) } returns listOf(eventToUpdate, eventToDelete)
      every { timelineService.hardDelete(listOf(timelineToUpdate)) } just Runs
      every { timelineService.saveAll(capture(savedTimelines)) } answers { firstArg() }
      every { eventService.hardDelete(listOf(eventToUpdate, eventToDelete)) } just Runs
      every { eventService.saveAll(capture(savedEvents)) } answers { firstArg() }
      every { stateService.delete(assessment.uuid) } just Runs
      every { stateService.rebuildFromEvents(assessment, null) } returns mutableMapOf()
      every { stateService.persist(any()) } just Runs
      every { authenticationHolder.principal } returns "DELETER"
      every { clock.now() } returns redactedAt
      every { auditService.audit(any(), any(), any()) } just Runs

      val result = service.updateData(assessment.uuid, request)

      assertThat(result.success).isTrue()
      assertThat(result.dryRun).isFalse()
      assertThat(result.rebuiltState).isEqualTo(emptyMap<Any, Any>())

      assertThat(savedTimelines.captured).hasSize(1)
      savedTimelines.captured.first().also {
        assertThat(it.uuid).isEqualTo(timelineToUpdate.uuid)
        assertThat(it.position).isEqualTo(timelineToUpdate.position)
        assertThat(it.createdAt).isEqualTo(timelineToUpdate.createdAt)
        assertThat(it.user).isEqualTo(timelineToUpdate.user)
        assertThat(it.assessment).isEqualTo(timelineToUpdate.assessment)
        assertThat(it.eventType).isEqualTo(replacementTimeline.event)
        assertThat(it.data).isEqualTo(replacementTimeline.data)
        assertThat(it.customType).isEqualTo(replacementTimeline.customType)
        assertThat(it.customData).isEqualTo(replacementTimeline.customData)
        assertThat(it.deleted).isEqualTo(timelineToUpdate.deleted)
      }

      assertThat(savedEvents.captured).hasSize(2)
      savedEvents.captured.first { it.uuid == eventToUpdate.uuid }.also {
        assertThat(it.position).isEqualTo(eventToUpdate.position)
        assertThat(it.createdAt).isEqualTo(eventToUpdate.createdAt)
        assertThat(it.user).isEqualTo(eventToUpdate.user)
        assertThat(it.assessment).isEqualTo(eventToUpdate.assessment)
        assertThat(it.data).isEqualTo(replacementEvent)
        assertThat(it.deleted).isEqualTo(eventToUpdate.deleted)
      }
      savedEvents.captured.first { it.uuid == eventToDelete.uuid }.also {
        assertThat(it.position).isEqualTo(eventToDelete.position)
        assertThat(it.data).isEqualTo(
          RedactedEvent(
            eventType = "AssessmentAnswersUpdatedEvent",
            dateRedacted = redactedAt,
            redactedBy = "DELETER",
          ),
        )
        assertThat(it.deleted).isEqualTo(eventToDelete.deleted)
      }

      verify(exactly = 1) { stateService.delete(assessment.uuid) }
      verify(exactly = 1) { stateService.rebuildFromEvents(assessment, null) }
      verify(exactly = 1) { stateService.persist(any()) }
      verify(exactly = 1) {
        auditService.audit(
          "DELETER",
          "RewroteHistory",
          match {
            it.contains("Updated 2 events and 1 timelines.") &&
              it.contains(eventToUpdate.uuid.toString()) &&
              it.contains(eventToDelete.uuid.toString()) &&
              it.contains(timelineToUpdate.uuid.toString())
          },
        )
      }
    }

    @Test
    fun `throws when a requested timeline does not exist`() {
      val missingTimelineUuid = UUID.fromString("aee7385f-98e0-43af-b092-7a03e3c4e6b7")
      val request = DataDeletionRequest(
        dryRun = false,
        timeline = listOf(
          TimelineUpdate(
            uuid = missingTimelineUuid,
            operation = DataDeletionOperation.UPDATE,
            timeline = TimelineItem(
              uuid = missingTimelineUuid,
              position = 1,
              timestamp = now,
              user = User(user.uuid, user.displayName),
              assessment = assessment.uuid,
              event = "AssessmentCreatedEvent",
            ),
          ),
        ),
      )

      every { assessmentService.findBy(assessment.uuid) } returns assessment
      every { timelineService.findByUuidsIncludingDeleted(setOf(missingTimelineUuid)) } returns emptyList()

      assertThatThrownBy { service.updateData(assessment.uuid, request) }
        .isInstanceOf(TimelineNotFoundException::class.java)
    }

    @Test
    fun `throws when a requested event does not exist`() {
      val missingEventUuid = UUID.fromString("fd584fdf-5ac4-48c3-b14a-6c0566699af0")
      val request = DataDeletionRequest(
        dryRun = false,
        events = listOf(
          EventUpdate(
            uuid = missingEventUuid,
            operation = DataDeletionOperation.UPDATE,
            event = AssessmentCreatedEvent(formVersion = "1", properties = emptyMap()),
          ),
        ),
      )

      every { assessmentService.findBy(assessment.uuid) } returns assessment
      every { timelineService.findByUuidsIncludingDeleted(emptySet()) } returns emptyList()
      every { eventService.findByUuidsIncludingDeleted(setOf(missingEventUuid)) } returns emptyList()

      assertThatThrownBy { service.updateData(assessment.uuid, request) }
        .isInstanceOf(EventNotFoundException::class.java)
    }
  }

  private fun existingEvent(
    position: Int,
    data: Event,
  ) = EventEntity(
    position = position,
    createdAt = now,
    user = user,
    assessment = assessment,
    data = data,
  )

  private fun existingTimeline(
    position: Int,
    eventType: String,
    data: Map<String, Any>,
    customType: String? = null,
    customData: Map<String, Any>? = null,
  ) = TimelineEntity(
    position = position,
    createdAt = now,
    user = user,
    assessment = assessment,
    eventType = eventType,
    data = data,
    customType = customType,
    customData = customData,
  )
}
