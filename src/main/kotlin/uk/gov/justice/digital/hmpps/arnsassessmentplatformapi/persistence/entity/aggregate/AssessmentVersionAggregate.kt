package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.FormVersionUpdated

const val type = "ASSESSMENT_VERSION"

@JsonTypeName(type)
class AssessmentVersionAggregate : Aggregate {
  private lateinit var formVersion: String
  private val answers: MutableMap<String, List<String>> = mutableMapOf()
  private val collaborators: MutableSet<User> = mutableSetOf()

  fun handle(event: AnswersUpdated) {
    event.added.entries.map { answers.put(it.key, it.value) }
  }

  fun handle(event: FormVersionUpdated) {
    formVersion = event.version
  }

  fun getAnswers() = this.answers.toMap()

  override fun apply(events: List<EventEntity>): AssessmentVersionAggregate {
    events.sortedBy { it.createdAt }
      .forEach { event ->
        collaborators.add(event.user)
        when (event.data) {
          is AnswersUpdated -> handle(event.data)
          is FormVersionUpdated -> handle(event.data)
          else -> {}
        }
      }
    return this
  }

  companion object : AggregateType {
    override val aggregateType = type
    override val updatesOn = setOf(AnswersUpdated::class)
  }
}
