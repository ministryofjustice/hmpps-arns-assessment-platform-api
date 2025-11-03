package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.CollectionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.CollectionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class CollectionQueryHandler(
  private val assessmentService: AssessmentService,
  private val aggregateService: AggregateService,
) : QueryHandler<CollectionQuery> {
  override val type = CollectionQuery::class
  override fun handle(query: CollectionQuery): CollectionQueryResult {
    val aggregate = assessmentService.findByUuid(query.assessmentUuid)
      .let { assessment -> aggregateService.fetchOrCreateAggregate(assessment, AssessmentVersionAggregate::class, query.timestamp) }
      .data as AssessmentVersionAggregate

    return CollectionQueryResult(
      collection = truncateCollection(aggregate.getCollection(query.collectionUuid), query.depth),
    )
  }

  companion object {
    fun truncateCollection(collection: Collection, depth: Int, currentDepth: Int = 0): Collection = collection.copy(
      items = collection.items.map { item ->
        item.copy(
          collections = if (currentDepth < depth) {
            item.collections.map { truncateCollection(it, depth, currentDepth + 1) }.toMutableList()
          } else {
            mutableListOf()
          },
        )
      }.toMutableList(),
    )
  }
}
