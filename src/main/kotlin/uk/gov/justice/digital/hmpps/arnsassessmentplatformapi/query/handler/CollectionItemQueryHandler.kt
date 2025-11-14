package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.CollectionItemQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.CollectionItemQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class CollectionItemQueryHandler(
  private val assessmentService: AssessmentService,
  private val stateService: StateService,
) : QueryHandler<CollectionItemQuery> {
  override val type = CollectionItemQuery::class
  override fun handle(query: CollectionItemQuery): CollectionItemQueryResult {
    val assessment = assessmentService.findByUuid(query.assessmentUuid)

    val state = stateService.stateForType(AssessmentAggregate::class)
      .fetchOrCreateState(assessment, query.timestamp) as AssessmentState

    val collectionItem = state.get().data.getCollectionItem(query.collectionItemUuid)

    return CollectionItemQueryResult(
      collectionItem = truncateCollectionItem(collectionItem, query.depth),
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
