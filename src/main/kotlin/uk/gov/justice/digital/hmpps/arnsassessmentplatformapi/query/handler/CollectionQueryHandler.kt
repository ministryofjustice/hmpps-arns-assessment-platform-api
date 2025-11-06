package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.CollectionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.CollectionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class CollectionQueryHandler(
  private val assessmentService: AssessmentService,
  private val stateService: StateService,
) : QueryHandler<CollectionQuery> {
  override val type = CollectionQuery::class
  override fun handle(query: CollectionQuery): CollectionQueryResult {
    val assessment = assessmentService.findByUuid(query.assessmentUuid)

    val state = stateService.ForType(AssessmentAggregate::class)
      .fetchState(assessment, query.timestamp) as AssessmentState

    val collection = state.get().data.getCollection(query.collectionUuid)

    return CollectionQueryResult(
      collection = truncateCollection(collection, query.depth),
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
