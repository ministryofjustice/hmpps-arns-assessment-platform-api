package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentTimelineQueryResult

@Component
class AssessmentTimelineQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<AssessmentTimelineQuery> {
  override val type = AssessmentTimelineQuery::class
  override fun handle(query: AssessmentTimelineQuery): AssessmentTimelineQueryResult {
    val assessment = services.assessmentService.findBy(query.assessmentIdentifier)

    val state = services.stateService.stateForType(AssessmentAggregate::class)
      .fetchOrCreateState(assessment, query.timestamp) as AssessmentState

    val timeline = state.getForRead().data.timeline
    val filtered = query.timelineTypes?.let { types -> timeline.filter { it.type in types } } ?: timeline

    return AssessmentTimelineQueryResult(filtered.toMutableList())
  }
}
