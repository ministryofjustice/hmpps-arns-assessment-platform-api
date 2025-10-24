package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentTimelineAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentTimelineQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.CollectionService

@Component
class AssessmentTimelineQueryHandler(
  private val collectionService: CollectionService,
  private val aggregateService: AggregateService,
) : QueryHandler<AssessmentTimelineQuery> {
  override val type = AssessmentTimelineQuery::class
  override fun handle(query: AssessmentTimelineQuery): AssessmentTimelineQueryResult {
    val aggregate = collectionService.findByUuid(query.collectionUuid)
      .let { assessment -> aggregateService.fetchOrCreateAggregate(assessment, AssessmentTimelineAggregate::class, query.timestamp) }
      .data as AssessmentTimelineAggregate
    return AssessmentTimelineQueryResult(aggregate.getTimeline())
  }
}
