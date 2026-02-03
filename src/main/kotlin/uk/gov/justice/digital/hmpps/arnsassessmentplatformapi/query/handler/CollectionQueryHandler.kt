package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.CollectionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.exception.CollectionDepthOutOfBoundsException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.CollectionQueryResult

@Component
class CollectionQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<CollectionQuery> {
  override val type = CollectionQuery::class
  override fun handle(query: CollectionQuery): CollectionQueryResult {
    val assessment = services.assessment.findBy(query.assessmentIdentifier)

    val state = services.state.stateForType(AssessmentAggregate::class)
      .fetchOrCreateState(assessment, query.timestamp) as AssessmentState

    val aggregate = state.getForRead()
    val collection = aggregate.data.getCollection(query.collectionUuid)
      ?: throw CollectionNotFoundException(query.collectionUuid, aggregate.uuid)

    val truncatedCollection = when {
      query.depth == -1 -> collection
      query.depth >= 0 -> truncateCollection(collection, query.depth)
      else -> throw CollectionDepthOutOfBoundsException(query.depth, query.collectionUuid)
    }

    return CollectionQueryResult(
      collection = truncatedCollection,
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
