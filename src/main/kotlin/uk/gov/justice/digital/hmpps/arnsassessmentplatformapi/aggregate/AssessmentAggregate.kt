package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import io.micrometer.core.instrument.MockClock.clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.mutableListOf

typealias Timeline = MutableList<TimelineItem>
typealias Collaborators = MutableSet<User>
typealias Answers = MutableMap<String, List<String>>
typealias Properties = MutableMap<String, List<String>>
typealias Collections = MutableList<Collection>
typealias FormVersion = String?

data class AssessmentAggregate(
  var createdAt: LocalDateTime,
  var updatedAt: LocalDateTime,
  val properties: Properties = mutableMapOf(),
  val deletedProperties: Properties = mutableMapOf(),
  val answers: Answers = mutableMapOf(),
  val deletedAnswers: Answers = mutableMapOf(),
  val collections: Collections = mutableListOf(),
  val collaborators: Collaborators = mutableSetOf(),
  val timeline: Timeline = mutableListOf(),
  var formVersion: FormVersion = null,
) : Aggregate<AssessmentAggregate> {
  override fun clone() = AssessmentAggregate(
    createdAt = createdAt,
    updatedAt = updatedAt,
    properties = properties.toMutableMap(),
    answers = answers.toMutableMap(),
    deletedAnswers = deletedAnswers.toMutableMap(),
    collections = collections.toMutableList(),
    collaborators = collaborators.toMutableSet(),
    timeline = timeline.toMutableList(),
    formVersion = formVersion,
  )

  fun getCollection(id: UUID) = collections.firstOrNull { it.uuid == id }
    ?: collections.firstNotNullOfOrNull { collection -> collection.items.firstNotNullOfOrNull { it.findCollection(id) } } ?: throw Error("Collection ID $id does not exist")

  fun getCollectionItem(id: UUID) = collections.firstNotNullOfOrNull { it.findItem(id) } ?: throw Error("Collection item ID $id does not exist")
}
