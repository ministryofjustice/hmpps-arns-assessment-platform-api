package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import kotlin.collections.mutableListOf

typealias Timeline = MutableList<TimelineItem>
typealias Collaborators = MutableSet<User>
typealias Answers = MutableMap<String, List<String>>
typealias Properties = MutableMap<String, List<String>>
typealias Collections = MutableList<Collection>
typealias FormVersion = String?

data class AssessmentAggregate(
  val properties: Properties = mutableMapOf(),
  val deletedProperties: Properties = mutableMapOf(),
  val answers: Answers = mutableMapOf(),
  val deletedAnswers: Answers = mutableMapOf(),
  val collections: Collections = mutableListOf(),
  val collaborators: Collaborators = mutableSetOf(),
  val timeline: Timeline = mutableListOf(),
  var formVersion: FormVersion = null,
): Aggregate<AssessmentAggregate> {
  override fun clone() = AssessmentAggregate(
    properties = properties.toMutableMap(),
    answers = answers.toMutableMap(),
    deletedAnswers = deletedAnswers.toMutableMap(),
    collections = collections.toMutableList(),
    collaborators = collaborators.toMutableSet(),
    timeline = timeline.toMutableList(),
    formVersion = formVersion,
  )
}
