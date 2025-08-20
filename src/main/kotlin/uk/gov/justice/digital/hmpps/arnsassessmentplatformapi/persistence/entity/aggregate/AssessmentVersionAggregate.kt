package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersRolledBack
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
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

  fun applyAnswers(added: Map<String, List<String>>, removed: List<String>) {
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

  fun handle(event: AnswersUpdated) = applyAnswers(event.added, event.removed)

  fun handle(event: AnswersRolledBack) = applyAnswers(event.added, event.removed)

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
          is AnswersRolledBack -> handle(event.data)
          is FormVersionUpdated -> handle(event.data)
          else -> {}
        }
      }
    return this
  }

  override fun clone() = AssessmentVersionAggregate()
    .also {
      it.formVersion = formVersion
      it.answers.plus(answers)
      it.collaborators.addAll(collaborators)
    }

  companion object : AggregateType {
    override val getInstance = { AssessmentVersionAggregate() }
    override val aggregateType = TYPE
    override val createsOn = setOf(AssessmentCreated::class, OasysEventAdded::class)
    override val updatesOn = setOf(AnswersUpdated::class, AnswersRolledBack::class, FormVersionUpdated::class)
  }
}
