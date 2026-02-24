package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.DailyVersionsQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.DailyVersionsQueryResult

@Component
class DailyVersionsQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<DailyVersionsQuery> {
  override val type = DailyVersionsQuery::class

  override fun handle(query: DailyVersionsQuery) = services.assessment.findBy(query.assessmentIdentifier, services.clock.now()).uuid
    .run(services.timeline::findDailyVersions)
    .let { DailyVersionsQueryResult(it) }
}
