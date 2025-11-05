package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentTimelineQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class AssessmentTimelineQueryHandler(
  private val assessmentService: AssessmentService,
) : QueryHandler<AssessmentTimelineQuery> {
  override val type = AssessmentTimelineQuery::class
  override fun handle(query: AssessmentTimelineQuery): AssessmentTimelineQueryResult {
    val aggregate = assessmentService.findByUuid(query.assessmentUuid)
      .let { assessment -> assessmentService.fetchState(assessment, query.timestamp) }
      .current().data
    return AssessmentTimelineQueryResult(aggregate.timeline)
  }
}
