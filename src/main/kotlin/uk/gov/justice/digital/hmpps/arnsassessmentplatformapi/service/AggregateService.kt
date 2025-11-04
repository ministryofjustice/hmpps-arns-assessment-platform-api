package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class AggregateService(
  private val aggregateRepository: AggregateRepository,
  private val eventService: EventService,
  private val eventBus: EventBus,
  private val clock: Clock,
) {
  private fun now(): LocalDateTime = LocalDateTime.now(clock)

  @Transactional
  fun createAggregate(assessment: AssessmentEntity): AggregateEntity = createAggregateForPointInTime(assessment, now())
    .run(aggregateRepository::save)

  fun fetchLatestAggregateBeforePointInTime(
    assessmentUuid: UUID,
    pointInTime: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentBeforeDate(assessmentUuid, pointInTime)

  fun fetchLatestAggregate(assessmentUuid: UUID): AggregateEntity? = fetchLatestAggregateBeforePointInTime(assessmentUuid, now())

  fun fetchOrCreateAggregate(
    assessment: AssessmentEntity,
    pointInTime: LocalDateTime?,
  ): AggregateEntity = if (pointInTime != null) {
    fetchAggregateForExactPointInTime(assessment, pointInTime)
      ?: createAggregateForPointInTime(assessment, pointInTime).run(aggregateRepository::save)
  } else {
    fetchLatestAggregate(assessment.uuid)
      ?: createAggregateForPointInTime(assessment, now()).run(aggregateRepository::save)
  }

  fun fetchAggregateForExactPointInTime(
    assessment: AssessmentEntity,
    date: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentOnExactDate(assessment.uuid, date)

  fun createAggregateForPointInTime(
    assessment: AssessmentEntity,
    pointInTime: LocalDateTime,
  ): AggregateEntity {
    val events = eventService
      .findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, pointInTime)
      .sortedBy { it.createdAt }

    val base = events.maxByOrNull { it.createdAt }?.let { latestEvent ->
      fetchLatestAggregateBeforePointInTime(assessment.uuid, latestEvent.createdAt)
        ?.clone()
    } ?: AggregateEntity(
      assessment = assessment,
      data = AssessmentAggregate(),
      eventsFrom = assessment.createdAt,
      eventsTo = assessment.createdAt,
      updatedAt = now(),
    )

    return events.fold(AssessmentState(base)) { acc, event -> eventBus.handle(event, acc) }.current()
  }

  fun fetchLatestState(assessment: AssessmentEntity) = let {
    fetchLatestAggregate(assessment.uuid)
      ?: AggregateEntity(
        assessment = assessment,
        data = AssessmentAggregate(),
        eventsFrom = assessment.createdAt,
        eventsTo = assessment.createdAt,
        updatedAt = now(),
      )
  }.run(::AssessmentState)

  fun persistState(assessmentState: AssessmentState) {
    aggregateRepository.saveAll(assessmentState.aggregates)
  }
}
