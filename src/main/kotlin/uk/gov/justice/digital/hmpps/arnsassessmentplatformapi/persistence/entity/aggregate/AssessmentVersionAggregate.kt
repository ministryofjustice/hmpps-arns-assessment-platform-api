package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersRolledBack
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.FormVersionUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.OasysEventAdded

private const val TYPE = "ASSESSMENT_VERSION"

@JsonTypeName(TYPE)
class AssessmentVersionAggregate(
  private val answers: MutableMap<String, List<String>> = mutableMapOf(),
  private val deletedAnswers: MutableMap<String, List<String>> = mutableMapOf(),
  private val collaborators: MutableSet<User> = mutableSetOf(),
  private var formVersion: String? = null,
) : Aggregate {
  private fun applyAnswers(added: Map<String, List<String>>, removed: List<String>) {
    added.entries.map {
      answers.put(it.key, it.value)
      deletedAnswers.remove(it.key)
    }
    removed.map { fieldCode ->
      answers[fieldCode]?.let { value ->
        answers.remove(fieldCode)
        deletedAnswers.put(
          fieldCode,
          value,
        )
      }
    }
  }

  private fun handle(event: AnswersUpdated) {
    applyAnswers(event.added, event.removed)
    numberOfEventsApplied += 1
  }

  private fun handle(event: AnswersRolledBack) {
    applyAnswers(event.added, event.removed)
    numberOfEventsApplied += 1
  }

  private fun handle(event: FormVersionUpdated) {
    formVersion = event.version
    numberOfEventsApplied += 1
  }

  fun getAnswers() = this.answers.toMap()
  fun getFormVersion() = this.formVersion
  fun getCollaborators() = collaborators

  override var numberOfEventsApplied: Long = 0

  override fun apply(event: EventEntity): Boolean {
    collaborators.add(event.user)
    when (event.data) {
      is AnswersUpdated -> handle(event.data)
      is AnswersRolledBack -> handle(event.data)
      is FormVersionUpdated -> handle(event.data)
      else -> return false
    }

    return true
  }

  override fun shouldCreate(event: Event) = createsOn.contains(event::class) || numberOfEventsApplied % 50L == 0L
  override fun shouldUpdate(event: Event) = updatesOn.contains(event::class)

  // TODO: refactor? We clone maps/sets using the toMutableX() method to avoid a pass by ref
  override fun clone() = AssessmentVersionAggregate(
    formVersion = formVersion,
    answers = answers.toMutableMap(),
    deletedAnswers = deletedAnswers.toMutableMap(),
    collaborators = collaborators.toMutableSet(),
  )

  companion object : AggregateType {
    override val getInstance = { AssessmentVersionAggregate() }
    override val aggregateType = TYPE
    override val createsOn = setOf(AssessmentCreated::class, OasysEventAdded::class)
    override val updatesOn = setOf(AnswersUpdated::class, AnswersRolledBack::class, FormVersionUpdated::class)
  }
}
