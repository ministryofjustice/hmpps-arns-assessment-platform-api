package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.PageWindow
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimeframeWindow
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.PageInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult

@Component
class AssessmentTimelineQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<AssessmentTimelineQuery> {
  override val type = AssessmentTimelineQuery::class

  override fun handle(query: AssessmentTimelineQuery) = when (query.window) {
    is TimeframeWindow -> services.timeline.findAllBetweenByAssessmentUuid(
      services.assessment.findBy(query.identifier).uuid,
      query.window.from,
      query.window.to,
    ).let { timeline -> TimelineQueryResult(timeline) }

    is PageWindow -> services.timeline.findAllPageableByAssessmentUuid(
      services.assessment.findBy(query.identifier).uuid,
      query.window.count,
      query.window.page,
    ).let { page ->
      TimelineQueryResult(
        page.content,
        PageInfo(
          pageNumber = page.number,
          totalPages = page.totalPages,
        ),
      )
    }
  }
}
