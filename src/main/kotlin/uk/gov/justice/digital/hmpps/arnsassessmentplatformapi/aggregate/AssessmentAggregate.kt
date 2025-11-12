package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID
import kotlin.collections.mutableListOf

typealias Timeline = MutableList<TimelineItem>
typealias Collaborators = MutableSet<User>
typealias Answers = MutableMap<String, List<String>>
typealias Properties = MutableMap<String, List<String>>
typealias Collections = MutableList<Collection>
typealias FormVersion = String

class AssessmentAggregate() : Aggregate<AssessmentAggregate> {
  lateinit var formVersion: FormVersion

  val properties: Properties = mutableMapOf()
  val deletedProperties: Properties = mutableMapOf()
  val answers: Answers = mutableMapOf()
  val deletedAnswers: Answers = mutableMapOf()
  val collections: Collections = mutableListOf()
  val collaborators: Collaborators = mutableSetOf()
  val timeline: Timeline = mutableListOf()

  override fun clone() = AssessmentAggregate().also { clone ->
    clone.properties.putAll(properties)
    clone.answers.putAll(answers)
    clone.deletedAnswers.putAll(deletedAnswers)
    clone.collections.addAll(collections)
    clone.collaborators.addAll(collaborators)
    clone.timeline.addAll(timeline)
    clone.formVersion = formVersion
  }

  fun getCollection(id: UUID) = collections.firstOrNull { it.uuid == id }
    ?: collections.firstNotNullOfOrNull { collection -> collection.items.firstNotNullOfOrNull { it.findCollection(id) } } ?: throw Error("Collection ID $id does not exist")

  fun getCollectionItem(id: UUID) = collections.firstNotNullOfOrNull { it.findItem(id) } ?: throw Error("Collection item ID $id does not exist")
}
