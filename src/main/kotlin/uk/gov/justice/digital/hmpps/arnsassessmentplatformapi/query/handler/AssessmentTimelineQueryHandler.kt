package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentTimelineQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class AssessmentTimelineQueryHandler(
  private val assessmentService: AssessmentService,
  private val stateService: StateService,
) : QueryHandler<AssessmentTimelineQuery> {
  override val type = AssessmentTimelineQuery::class
  override fun handle(query: AssessmentTimelineQuery): AssessmentTimelineQueryResult {
    val assessment = assessmentService.findByUuid(query.assessmentUuid)

    val state = stateService.stateForType(AssessmentAggregate::class)
      .fetchOrCreateState(assessment, query.timestamp) as AssessmentState

    val timeline = state.getLatest().data.timeline
    val filtered = query.timelineTypes?.let { types -> timeline.filter { it.type in types } } ?: timeline

    return AssessmentTimelineQueryResult(filtered.toMutableList())
  }
}
