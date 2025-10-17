package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentTimelineAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentTimelineQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class AssessmentTimelineHandler(
  private val assessmentService: AssessmentService,
  private val aggregateService: AggregateService,
) : QueryHandler<AssessmentTimelineQuery> {
  override val type = AssessmentTimelineQuery::class
  override fun handle(query: AssessmentTimelineQuery): AssessmentTimelineQueryResult {
    val aggregate = assessmentService.findByUuid(query.assessmentUuid)
      .let { assessment -> aggregateService.fetchOrCreateAggregate(assessment, AssessmentTimelineAggregate.aggregateType, query.timestamp) }
      .data as AssessmentTimelineAggregate
    return AssessmentTimelineQueryResult(aggregate.getTimeline())
  }
}
