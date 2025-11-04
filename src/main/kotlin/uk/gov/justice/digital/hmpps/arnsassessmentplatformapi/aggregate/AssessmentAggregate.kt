package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User

typealias Timeline = MutableList<String>
typealias Collaborators = MutableSet<User>

data class AssessmentAggregate(
  val answers: MutableMap<String, List<String>> = mutableMapOf(),
  val deletedAnswers: MutableMap<String, List<String>> = mutableMapOf(),
  val collections: MutableList<Collection> = mutableListOf(),
  val collaborators: Collaborators = mutableSetOf(),
  val timeline: Timeline = mutableListOf(),
  var formVersion: String? = null,
) {
  fun clone() = AssessmentAggregate(
    formVersion = formVersion,
    answers = answers.toMutableMap(),
    deletedAnswers = deletedAnswers.toMutableMap(),
    collaborators = collaborators.toMutableSet(),
  )
}
