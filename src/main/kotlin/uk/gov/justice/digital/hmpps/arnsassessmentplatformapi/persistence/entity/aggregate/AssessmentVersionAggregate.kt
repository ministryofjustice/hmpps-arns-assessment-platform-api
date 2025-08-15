package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.FormVersionUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.OasysEventAdded

private const val TYPE = "ASSESSMENT_VERSION"

@JsonTypeName(TYPE)
class AssessmentVersionAggregate : Aggregate {
  private lateinit var formVersion: String
  private val answers: MutableMap<String, List<String>> = mutableMapOf()
  private val collaborators: MutableSet<User> = mutableSetOf()

  fun handle(event: AnswersUpdated) {
    event.added.entries.map { answers.put(it.key, it.value) }
    event.removed.map { answers[it] = emptyList() }
  }

  fun handle(event: FormVersionUpdated) {
    formVersion = event.version
  }

  fun getAnswers() = this.answers.toMap()

  override fun applyAll(events: List<EventEntity>): AssessmentVersionAggregate {
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
    override val getInstance = { AssessmentVersionAggregate() }
    override val aggregateType = TYPE
    override val createsOn = setOf(AssessmentCreated::class, OasysEventAdded::class)
    override val updatesOn = setOf(AnswersUpdated::class, FormVersionUpdated::class)
  }
}
