package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.CollectionService

@Component
class AssessmentVersionQueryHandler(
  private val collectionService: CollectionService,
  private val aggregateService: AggregateService,
) : QueryHandler<AssessmentVersionQuery> {
  override val type = AssessmentVersionQuery::class
  override fun handle(query: AssessmentVersionQuery): AssessmentVersionQueryResult {
    val aggregate = collectionService.findByUuid(query.collectionUuid)
      .let { assessment -> aggregateService.fetchOrCreateAggregate(assessment, AssessmentVersionAggregate::class, query.timestamp) }
      .data as AssessmentVersionAggregate

    return AssessmentVersionQueryResult(
      formVersion = aggregate.getFormVersion(),
      answers = aggregate.getAnswers(),
      collaborators = aggregate.getCollaborators(),
    )
  }
}
