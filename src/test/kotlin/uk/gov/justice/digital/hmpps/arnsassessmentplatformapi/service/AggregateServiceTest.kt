package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.CollectionEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertIs

class AggregateServiceTest {
  val clock: Clock = mockk(relaxed = true)
  val aggregateRepository: AggregateRepository = mockk()
  val eventService: EventService = mockk()
  val collectionService: CollectionService = mockk()
  val service = AggregateService(
    aggregateRepository = aggregateRepository,
    eventService = eventService,
    clock = clock,
    collectionService = collectionService,
  )

  val assessment = CollectionEntity(createdAt = LocalDateTime.parse("2025-01-01T12:00:00"))
  val user = User("FOO_USER", "Foo User")

  @BeforeEach
  fun setUp() {
    every { clock.instant() } returns Instant.parse("2025-01-01T12:00:00Z")
    every { clock.zone } returns ZoneOffset.UTC
    every { aggregateRepository.save(any<AggregateEntity>()) } answers { firstArg<AggregateEntity>() }
  }

  @Nested
  inner class CreateAggregate {
    val firstEventTimestamp = assessment.createdAt
    val lastEventTimestamp: LocalDateTime = LocalDateTime.parse("2025-01-02T12:00:00")
    val events = listOf(
      EventEntity(
        user = user,
        collection = assessment,
        createdAt = firstEventTimestamp,
        data = AssessmentCreatedEvent(),
      ),
      EventEntity(
        user = user,
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = AnswersUpdatedEvent(
          added = mapOf("foo" to listOf("foo_value")),
          removed = emptyList(),
        ),
      ),
      EventEntity(
        user = user,
        collection = assessment,
        createdAt = lastEventTimestamp,
        data = AnswersUpdatedEvent(
          added = mapOf("bar" to listOf("bar_value")),
          removed = emptyList(),
        ),
      ),
    )

    @Test
    fun `it returns an empty aggregate when passed no events`() {
      every {
        eventService.findAllByCollectionUuidAndCreatedAtBefore(
          assessment.uuid,
          LocalDateTime.parse("2025-01-01T12:00:00"),
        )
      } returns emptyList()
      every {
        collectionService.findRoot(assessment)
      } returns assessment
      val result = service.createAggregate(assessment, AssessmentVersionAggregate::class)

      assertThat(result.data.numberOfEventsApplied).isEqualTo(0)
      assertThat(result.eventsFrom).isEqualTo(assessment.createdAt)
      assertThat(result.eventsTo).isEqualTo(assessment.createdAt)
    }

    @Test
    fun `it finds an aggregate`() {
      every {
        eventService.findAllByCollectionUuidAndCreatedAtBefore(
          assessment.uuid,
          LocalDateTime.parse("2025-01-01T12:00:00"),
        )
      } returns events
      every {
        aggregateRepository.findByAssessmentAndTypeBeforeDate(
          assessment.uuid,
          AssessmentVersionAggregate::class.simpleName!!,
          lastEventTimestamp,
        )
      } returns AggregateEntity.init(
        collection = assessment,
        data = AssessmentVersionAggregate(
          answers = mutableMapOf(
            "foo" to listOf("foo_value"),
            "bar" to listOf("bar_value"),
          ),
        ),
        events = events,
      )

      val result = service.createAggregate(assessment, AssessmentVersionAggregate::class)
      assertThat(result.data.numberOfEventsApplied).isEqualTo(2)
      assertThat(result.eventsFrom).isEqualTo(firstEventTimestamp)
      assertThat(result.eventsTo).isEqualTo(lastEventTimestamp)
    }

    @Test
    fun `it creates an aggregate`() {
      every {
        eventService.findAllByCollectionUuidAndCreatedAtBefore(
          assessment.uuid,
          LocalDateTime.parse("2025-01-01T12:00:00"),
        )
      } returns events
      every {
        aggregateRepository.findByAssessmentAndTypeBeforeDate(
          assessment.uuid,
          AssessmentVersionAggregate::class.simpleName!!,
          lastEventTimestamp,
        )
      } returns null
      every {
        collectionService.findRoot(assessment)
      } returns assessment

      val result = service.createAggregate(assessment, AssessmentVersionAggregate::class)
      assertThat(result.data.numberOfEventsApplied).isEqualTo(2)
      assertThat(result.eventsFrom).isEqualTo(firstEventTimestamp)
      assertThat(result.eventsTo).isEqualTo(lastEventTimestamp)
    }
  }

  @Nested
  inner class FetchLatestAggregateForType {
    @Test
    fun `it fetches an the latest aggregate for a given type`() {
      val latestAggregate = AggregateEntity.init(collection = assessment, data = AssessmentVersionAggregate())
      every {
        aggregateRepository.findByAssessmentAndTypeBeforeDate(
          assessment.uuid,
          AssessmentVersionAggregate::class.simpleName!!,
          LocalDateTime.parse("2025-01-01T12:00:00"),
        )
      } returns latestAggregate

      val result = service.fetchLatestAggregate(assessment.uuid, AssessmentVersionAggregate::class)
      assertThat(result).isNotNull
      assertThat(result?.collection).isEqualTo(assessment)
      assertThat(result?.uuid).isEqualTo(latestAggregate.uuid)
      assertThat(result?.data).isEqualTo(latestAggregate.data)
    }

    @Test
    fun `it returns null when no aggregate found`() {
      every {
        aggregateRepository.findByAssessmentAndTypeBeforeDate(
          assessment.uuid,
          AssessmentVersionAggregate::class.simpleName!!,
          LocalDateTime.parse("2025-01-01T12:00:00"),
        )
      } returns null

      val result = service.fetchLatestAggregate(assessment.uuid, AssessmentVersionAggregate::class)
      assertThat(result).isNull()
    }
  }

  @Nested
  inner class FetchAggregateForTypeOnDate {
    @Test
    fun `it fetches an the aggregate for a given type and date`() {
      val latestAggregate = AggregateEntity.init(collection = assessment, data = AssessmentVersionAggregate())
      val date = LocalDateTime.parse("2025-01-01T12:00:00")
      every {
        aggregateRepository.findByAssessmentAndTypeOnExactDate(
          assessment.uuid,
          AssessmentVersionAggregate::class.simpleName!!,
          date,
        )
      } returns latestAggregate

      val result = service.fetchAggregateForExactPointInTime(assessment, AssessmentVersionAggregate::class, date)
      assertThat(result).isNotNull
      assertThat(result?.collection).isEqualTo(assessment)
      assertThat(result?.uuid).isEqualTo(latestAggregate.uuid)
      assertThat(result?.data).isEqualTo(latestAggregate.data)
    }

    @Test
    fun `it returns null when no aggregate found`() {
      val date = LocalDateTime.parse("2025-01-01T12:00:00")
      every {
        aggregateRepository.findByAssessmentAndTypeOnExactDate(
          assessment.uuid,
          AssessmentVersionAggregate::class.simpleName!!,
          LocalDateTime.parse("2025-01-01T12:00:00"),
        )
      } returns null

      val result = service.fetchAggregateForExactPointInTime(assessment, AssessmentVersionAggregate::class, date)
      assertThat(result).isNull()
    }
  }

  @Nested
  inner class ProcessEvents {
    val user = User("FOO_USER", "Foo User")
    val events = listOf(
      EventEntity(
        user = user,
        collection = assessment,
        data = AnswersUpdatedEvent(
          added = mapOf("foo" to listOf("foo_value")),
          removed = emptyList(),
        ),
      ),
      EventEntity(
        user = user,
        collection = assessment,
        data = AnswersUpdatedEvent(
          added = mapOf("bar" to listOf("bar_value")),
          removed = listOf("foo"),
        ),
      ),
    )

    @Test
    fun `it processes events and updates an existing aggregate`() {
      val latestAggregate = AggregateEntity.init(collection = assessment, data = AssessmentVersionAggregate())
      every {
        aggregateRepository.findByAssessmentAndTypeBeforeDate(
          assessment.uuid,
          AssessmentVersionAggregate::class.simpleName!!,
          LocalDateTime.parse("2025-01-01T12:00:00"),
        )
      } returns latestAggregate

      val slot = slot<Iterable<AggregateEntity>>()

      every {
        aggregateRepository.saveAll(capture(slot))
      } answers {
        slot.captured.toList()
      }

      val result = service.processEvents(assessment.uuid, AssessmentVersionAggregate::class, events)
      assertThat(result).describedAs("aggregate should be the latest").isEqualTo(slot.captured.last())
      val data = assertIs<AssessmentVersionAggregate>(result.data)
      assertThat(data.numberOfEventsApplied).isEqualTo(2)
      val answers = data.getAnswers()
      assertThat(answers["foo"]).isNull()
      assertThat(answers["bar"]).isEqualTo(listOf("bar_value"))
    }

    @Test
    fun `it processes events and creates a new aggregate`() {
      every {
        aggregateRepository.findByAssessmentAndTypeBeforeDate(
          assessment.uuid,
          AssessmentVersionAggregate::class.simpleName!!,
          LocalDateTime.parse("2025-01-01T12:00:00"),
        )
      } returns null

      every {
        eventService.findAllByCollectionUuid(assessment.uuid)
      } returns listOf(
        EventEntity(
          user = user,
          collection = assessment,
          data = AssessmentCreatedEvent(),
        ),
      )

      val slot = slot<Iterable<AggregateEntity>>()

      every {
        aggregateRepository.saveAll(capture(slot))
      } answers {
        slot.captured.toList()
      }
      every {
        collectionService.findByUuid(assessment.uuid)
      } returns assessment

      val result = service.processEvents(assessment.uuid, AssessmentVersionAggregate::class, events)
      assertThat(result).describedAs("aggregate should be the latest").isEqualTo(slot.captured.last())
      val data = assertIs<AssessmentVersionAggregate>(result.data)
      assertThat(data.numberOfEventsApplied).isEqualTo(2)
      val answers = data.getAnswers()
      assertThat(answers["foo"]).isNull()
      assertThat(answers["bar"]).isEqualTo(listOf("bar_value"))
    }

    //
    // @Test
    // fun `it creates additional aggregates if configured to on an event and returns the latest one`() {
    // }
  }

  @Nested
  inner class CreateAggregateForPointInTime
}
