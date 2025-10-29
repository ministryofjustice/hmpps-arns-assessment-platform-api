package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.ChildCollectionCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAdded
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemoved
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReordered
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.util.UUID
import kotlin.reflect.KClass

data class CollectionItem(
  val answers: MutableMap<String, List<String>>,
)

data class Collection(
  val name: String,
  val uuid: UUID,
  val parentCollection: UUID? = null,
  val items: MutableList<CollectionItem> = mutableListOf(),
)

class CollectionAggregate(
  val collections: MutableMap<UUID, Collection>,
) : Aggregate {
  override var numberOfEventsApplied: Long = 0

  override fun apply(event: EventEntity): Boolean {
    when (event.data) {
      is CollectionCreated -> handle(event.data)
      is ChildCollectionCreated -> handle(event.data)
      is CollectionItemAdded -> handle(event.data)
      is CollectionItemUpdated -> handle(event.data)
      is CollectionItemRemoved -> handle(event.data)
      is CollectionItemReordered -> handle(event.data)
      else -> return false
    }

    numberOfEventsApplied += 1
    return true
  }

  private fun handle(event: CollectionCreated) {
    Collection(
      name = event.name,
      uuid = event.collectionUuid,
    ).let { collection -> collections.put(collection.uuid, collection) }
  }

  private fun handle(event: ChildCollectionCreated) {
    if (collections[event.parentCollectionUuid] == null) throw Error("Parent collection does not exist for ${event.parentCollectionUuid}")

    Collection(
      name = event.name,
      uuid = event.collectionUuid,
      parentCollection = event.parentCollectionUuid,
    ).let { collection -> collections.put(collection.uuid, collection) }
  }

  private fun handle(event: CollectionItemAdded) {
    collections[event.collectionUuid]?.let { collection ->
      val item = CollectionItem(event.answers.toMutableMap())
      event.index?.let { index ->
        collection.items.add(index, item)
      } ?: collection.items.add(item)
    } ?: throw Error("collection does not exist for ${event.collectionUuid}")
  }

  private fun handle(event: CollectionItemUpdated) {
    collections[event.collectionUuid]?.let { collection ->
      collection.items[event.index].run {
        event.added.forEach { answers.put(it.key, it.value) }
        event.removed.forEach { answers.remove(it) }
      }
    }
  }

  private fun handle(event: CollectionItemRemoved) {
    collections[event.collectionUuid]?.items?.removeAt(event.index)
      ?: throw Error("collection does not exist for ${event.collectionUuid}")
  }

  private fun handle(event: CollectionItemReordered) {
    collections[event.collectionUuid]?.let { collection ->
      val item = collection.items.removeAt(event.previousIndex)
      collection.items.add(event.index, item)
    }
  }

  override fun shouldCreate(event: KClass<out Event>) = createsOn.contains(event) || numberOfEventsApplied % 50L == 0L
  override fun shouldUpdate(event: KClass<out Event>) = updatesOn.contains(event)

  override fun clone() = CollectionAggregate(collections = mutableMapOf())

  companion object : AggregateType {
    override val createsOn = setOf(AssessmentCreatedEvent::class)
    override val updatesOn = setOf(
      CollectionCreated::class,
      ChildCollectionCreated::class,
      CollectionItemAdded::class,
      CollectionItemUpdated::class,
      CollectionItemRemoved::class,
      CollectionItemReordered::class,
    )
  }
}
