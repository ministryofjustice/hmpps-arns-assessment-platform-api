package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemovedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReorderedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.util.UUID
import kotlin.reflect.KClass

class AssessmentVersionAggregate(
  private val answers: MutableMap<String, List<String>> = mutableMapOf(),
  private val deletedAnswers: MutableMap<String, List<String>> = mutableMapOf(),
  private val collections: MutableList<Collection> = mutableListOf(),
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

  private fun handle(event: AssessmentAnswersUpdatedEvent) = applyAnswers(event.added, event.removed)

  private fun handle(event: AssessmentAnswersRolledBackEvent) = applyAnswers(event.added, event.removed)

  private fun handle(event: FormVersionUpdatedEvent) = run { formVersion = event.version }

  private fun handle(event: CollectionCreatedEvent) {
    Collection(
      uuid = event.collectionUuid,
      name = event.name,
      items = mutableListOf(),
    ).let { collection ->
      when (event.parentCollectionItemUuid) {
        null -> collections
        else -> getCollectionItem(event.parentCollectionItemUuid).collections
      }.add(collection)
    }
  }

  private fun handle(event: CollectionItemAddedEvent) {
    CollectionItem(
      uuid = event.collectionItemUuid,
      answers = event.answers.toMutableMap(),
      properties = event,,
      collections = mutableListOf(),
    ).let { collectionItem ->
      with(getCollection(event.collectionUuid)) {
        when (event.index) {
          null -> items.add(collectionItem)
          else -> items.add(event.index, collectionItem)
        }
      }
    }
  }

  private fun handle(event: CollectionItemAnswersUpdatedEvent) {
    getCollectionItem(event.collectionItemUuid).run {
      event.added.forEach { answers.put(it.key, it.value) }
      event.removed.forEach { answers.remove(it) }
    }
  }

  private fun handle(event: CollectionItemRemovedEvent) {
    if (!collections.any { collection -> collection.removeItem(event.collectionItemUuid) }) {
      throw Error("Collection item ID ${event.collectionItemUuid} does not exist")
    }
  }

  private fun handle(event: CollectionItemReorderedEvent) {
    if (!collections.any { collection -> collection.reorderItem(event.collectionItemUuid, event.index) }) {
      throw Error("Collection item ID ${event.collectionItemUuid} does not exist")
    }
  }

  fun getCollections() = this.collections.toList()

  fun getCollection(id: UUID) = collections.firstOrNull { it.uuid == id }
    ?: collections.firstNotNullOfOrNull { collection -> collection.items.firstNotNullOfOrNull { it.findCollection(id) } } ?: throw Error("Collection ID $id does not exist")

  fun getCollectionItem(id: UUID) = collections.firstNotNullOfOrNull { it.findItem(id) } ?: throw Error("Collection item ID $id does not exist")

  fun getAnswers() = this.answers.toMap()
  fun getFormVersion() = this.formVersion
  fun getCollaborators() = collaborators

  override var numberOfEventsApplied: Long = 0

  override fun apply(event: EventEntity): Boolean {
    when (event.data) {
      is AssessmentAnswersUpdatedEvent -> handle(event.data)
      is AssessmentAnswersRolledBackEvent -> handle(event.data)
      is FormVersionUpdatedEvent -> handle(event.data)
      is CollectionCreatedEvent -> handle(event.data)
      is CollectionItemAddedEvent -> handle(event.data)
      is CollectionItemAnswersUpdatedEvent -> handle(event.data)
      is CollectionItemRemovedEvent -> handle(event.data)
      is CollectionItemReorderedEvent -> handle(event.data)
      else -> return false
    }

    collaborators.add(event.user)
    numberOfEventsApplied += 1

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
    override val createsOn = setOf(AssessmentCreatedEvent::class, AssessmentPropertiesUpdatedEvent::class)
    override val updatesOn = setOf(
      AssessmentAnswersUpdatedEvent::class,
      AssessmentAnswersRolledBackEvent::class,
      FormVersionUpdatedEvent::class,
      CollectionCreatedEvent::class,
      CollectionItemAddedEvent::class,
      CollectionItemAnswersUpdatedEvent::class,
      CollectionItemRemovedEvent::class,
      CollectionItemReorderedEvent::class,
    )
  }
}
