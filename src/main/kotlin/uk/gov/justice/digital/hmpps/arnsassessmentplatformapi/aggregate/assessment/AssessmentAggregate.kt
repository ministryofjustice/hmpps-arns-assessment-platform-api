package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import java.util.UUID
import kotlin.collections.mutableListOf

typealias Timeline = MutableList<TimelineItem>
typealias Collaborators = MutableSet<UUID>
typealias Answers = MutableMap<String, Value>
typealias Properties = MutableMap<String, Value>
typealias Collections = MutableList<Collection>
typealias FormVersion = String

class AssessmentAggregate :
  Aggregate<AssessmentAggregate>,
  AssessmentAggregateView {
  override lateinit var formVersion: FormVersion
  override var assignedUser: UUID? = null

  override val properties: Properties = mutableMapOf()
  override val answers: Answers = mutableMapOf()
  override val collections: Collections = mutableListOf()
  override val collaborators: Collaborators = mutableSetOf()
  override val timeline: Timeline = mutableListOf()

  override fun clone() = AssessmentAggregate().also { clone ->
    clone.properties.putAll(properties)
    clone.answers.putAll(answers)
    clone.collections.addAll(collections)
    clone.collaborators.addAll(collaborators)
    clone.timeline.addAll(timeline)
    clone.formVersion = formVersion
  }

  override fun getCollection(collectionUuid: UUID) = collections.firstOrNull { it.uuid == collectionUuid }
    ?: collections.firstNotNullOfOrNull { collection -> collection.items.firstNotNullOfOrNull { it.findCollection(collectionUuid) } }

  override fun getCollectionItem(collectionItemUuid: UUID) = collections.firstNotNullOfOrNull { it.findItem(collectionItemUuid) }
}
