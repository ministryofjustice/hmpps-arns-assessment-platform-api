package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import kotlin.reflect.KClass

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

  private fun handle(event: AssessmentAnswersUpdatedEvent) {
    applyAnswers(event.added, event.removed)
    numberOfEventsApplied += 1
  }

  private fun handle(event: AssessmentAnswersRolledBackEvent) {
    applyAnswers(event.added, event.removed)
    numberOfEventsApplied += 1
  }

  private fun handle(event: FormVersionUpdatedEvent) {
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
      is AssessmentAnswersUpdatedEvent -> handle(event.data)
      is AssessmentAnswersRolledBackEvent -> handle(event.data)
      is FormVersionUpdatedEvent -> handle(event.data)
      else -> return false
    }

    return true
  }

  override fun shouldCreate(event: KClass<out Event>) = createsOn.contains(event) || numberOfEventsApplied % 50L == 0L
  override fun shouldUpdate(event: KClass<out Event>) = updatesOn.contains(event)

  // TODO: refactor? We clone maps/sets using the toMutableX() method to avoid a pass by ref
  override fun clone() = AssessmentVersionAggregate(
    formVersion = formVersion,
    answers = answers.toMutableMap(),
    deletedAnswers = deletedAnswers.toMutableMap(),
    collaborators = collaborators.toMutableSet(),
  )

  companion object : AggregateType {
    override val createsOn = setOf(AssessmentCreatedEvent::class, AssessmentStatusUpdatedEvent::class)
    override val updatesOn = setOf(AssessmentAnswersUpdatedEvent::class, AssessmentAnswersRolledBackEvent::class, FormVersionUpdatedEvent::class)
  }
}
