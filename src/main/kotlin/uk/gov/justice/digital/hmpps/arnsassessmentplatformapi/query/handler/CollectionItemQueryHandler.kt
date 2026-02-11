package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.CollectionItemQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.exception.CollectionDepthOutOfBoundsException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.CollectionItemQueryResult
import java.time.LocalDateTime

@Component
class CollectionItemQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<CollectionItemQuery> {
  override val type = CollectionItemQuery::class
  override fun handle(query: CollectionItemQuery): CollectionItemQueryResult {
    val assessment = services.assessment.findBy(query.assessmentIdentifier, LocalDateTime.now())

    val state = services.state.stateForType(AssessmentAggregate::class)
      .fetchOrCreateState(assessment, query.timestamp) as AssessmentState

    val aggregate = state.getForRead()
    val collectionItem = aggregate.data.getCollectionItem(query.collectionItemUuid)
      ?: throw CollectionItemNotFoundException(query.collectionItemUuid, aggregate.uuid)

    val truncatedCollectionItem = when {
      query.depth == -1 -> collectionItem
      query.depth >= 0 -> truncateCollectionItem(collectionItem, query.depth)
      else -> throw CollectionDepthOutOfBoundsException(query.depth, query.collectionItemUuid)
    }

    return CollectionItemQueryResult(
      collectionItem = truncatedCollectionItem,
    )
  }

  companion object {
    fun truncateCollectionItem(collectionItem: CollectionItem, depth: Int, currentDepth: Int = 0): CollectionItem = collectionItem.copy(
      collections = collectionItem.collections.map { collection ->
        collection.copy(
          items = if (currentDepth < depth) {
            collection.items.map { truncateCollectionItem(it, depth, currentDepth + 1) }.toMutableList()
          } else {
            mutableListOf()
          },
        )
      }.toMutableList(),
    )
  }
}
