package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.criteria.TimelineCriteria
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult

@Component
class TimelineQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<TimelineQuery> {
  override val type = TimelineQuery::class

  override fun handle(query: TimelineQuery) = services.timeline.findAll(
    TimelineCriteria(
      assessmentUuid = query.assessmentIdentifier?.let { services.assessment.findBy(it).uuid },
      userUuid = query.subject?.let { services.userDetails.find(it).uuid },
      from = query.from,
      to = query.to,
    ),
    PageRequest.of(
      query.pageNumber,
      query.pageSize,
      Sort.by(Sort.Direction.DESC, "created_at"),
    ),
  ).run(TimelineQueryResult::from)
}
