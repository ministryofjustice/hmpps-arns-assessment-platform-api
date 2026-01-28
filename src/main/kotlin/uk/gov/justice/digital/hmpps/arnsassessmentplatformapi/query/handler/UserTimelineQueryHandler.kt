package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Events
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Timeframe
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UserTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.PageInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult

@Component
class UserTimelineQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<UserTimelineQuery> {
  override val type = UserTimelineQuery::class

  override fun handle(query: UserTimelineQuery) = when (query.window) {
    is Timeframe -> services.timeline.findAllBetweenByUserUuid(
      services.userDetails.find(query.subject).uuid,
      query.window.from,
      query.window.to,
    ).let { timeline -> TimelineQueryResult(timeline) }

    is Events -> services.timeline.findAllPageableByUserUuid(
      services.userDetails.find(query.subject).uuid,
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
