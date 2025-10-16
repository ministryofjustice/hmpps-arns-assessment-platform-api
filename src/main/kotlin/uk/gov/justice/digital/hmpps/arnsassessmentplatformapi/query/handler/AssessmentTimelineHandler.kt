package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentTimelineAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentTimelineResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class AssessmentTimelineHandler(
  private val assessmentService: AssessmentService,
  private val aggregateService: AggregateService,
) : QueryHandler<AssessmentTimeline> {
  override val type = AssessmentTimeline::class
  override fun handle(query: AssessmentTimeline): AssessmentTimelineResult {
    val aggregate = assessmentService.findByUuid(query.assessmentUuid)
      .let { assessment -> aggregateService.fetchOrCreateAggregate(assessment, AssessmentTimelineAggregate.aggregateType, query.timestamp) }
      .data as AssessmentTimelineAggregate
    return AssessmentTimelineResult(aggregate.getTimeline())
  }
}
