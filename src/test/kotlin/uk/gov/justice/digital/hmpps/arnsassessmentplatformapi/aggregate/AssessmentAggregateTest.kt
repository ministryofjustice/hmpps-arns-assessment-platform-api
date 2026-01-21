package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collaborator
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class AssessmentAggregateTest {
  private fun creatCollection(
    id: UUID = UUID.randomUUID(),
    name: String = "collection",
    items: MutableList<CollectionItem> = mutableListOf(),
  ): Collection = Collection(
    uuid = id,
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
    name = name,
    items = items,
  )

  private fun createCollectionItem(
    id: UUID = UUID.randomUUID(),
    collections: MutableList<Collection> = mutableListOf(),
  ): CollectionItem = CollectionItem(
    uuid = id,
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
    answers = mutableMapOf(),
    properties = mutableMapOf(),
    collections = collections,
  )

  private fun populatedAggregate(): AssessmentAggregate = AssessmentAggregate().apply {
    formVersion = "v1"

    properties["p1"] = SingleValue("v1")

    answers["a1"] = SingleValue("answer1")

    val collection1 = creatCollection()
    val collection2 = creatCollection()
    collections.addAll(listOf(collection1, collection2))

    collaborators.add(mockk<Collaborator>())
    timeline.add(mockk<TimelineItem>())
  }

  @Nested
  inner class Clone {

    @Test
    fun `creates a new instance with same values`() {
      val aggregate = populatedAggregate()

      val clone = aggregate.clone()

      assertNotSame(aggregate, clone)

      assertEquals(aggregate.formVersion, clone.formVersion)
      assertEquals(aggregate.properties, clone.properties)
      assertEquals(aggregate.answers, clone.answers)
      assertEquals(aggregate.collections, clone.collections)
      assertEquals(aggregate.collaborators, clone.collaborators)
      assertEquals(aggregate.timeline, clone.timeline)
    }

    @Test
    fun `top-level collections are shallow-copied into new containers`() {
      val aggregate = populatedAggregate()

      val clone = aggregate.clone()

      assertNotSame(aggregate.properties, clone.properties)
      assertNotSame(aggregate.answers, clone.answers)
      assertNotSame(aggregate.collections, clone.collections)
      assertNotSame(aggregate.collaborators, clone.collaborators)
      assertNotSame(aggregate.timeline, clone.timeline)

      // modifying clone's top-level containers does not affect original container sizes
      clone.properties["p2"] = SingleValue("v2")
      clone.collections.clear()
      clone.collaborators.clear()
      clone.timeline.clear()

      assertTrue(aggregate.properties.containsKey("p1"))
      assertFalse(aggregate.properties.containsKey("p2"))

      assertTrue(aggregate.collections.isNotEmpty())
      assertTrue(aggregate.collaborators.isNotEmpty())
      assertTrue(aggregate.timeline.isNotEmpty())
    }
  }

  @Nested
  inner class GetCollection {

    @Test
    fun `returns collection when found at top level`() {
      val targetCollection = creatCollection()
      val otherCollection = creatCollection()

      val aggregate = AssessmentAggregate().apply {
        formVersion = "v1"
        collections.addAll(listOf(otherCollection, targetCollection))
      }

      val result = aggregate.getCollection(targetCollection.uuid)

      assertSame(targetCollection, result)
    }

    @Test
    fun `returns collection when found nested in collection items`() {
      val targetCollection = creatCollection()

      // item holding the target collection
      val collectionItem = createCollectionItem(
        collections = mutableListOf(targetCollection),
      )

      // top-level collection that contains the item
      val collection = creatCollection(
        items = mutableListOf(collectionItem),
      )

      val aggregate = AssessmentAggregate().apply {
        formVersion = "v1"
        collections.add(collection)
      }

      val result = aggregate.getCollection(targetCollection.uuid)

      assertSame(targetCollection, result)
    }

    @Test
    fun `searches multiple top-level collections if earlier ones do not contain id`() {
      // first collection has no matching nested collection
      val firstCollection = creatCollection(
        items = mutableListOf(createCollectionItem()),
      )

      // second collection has item with target collection
      val targetCollection = creatCollection()
      val itemWithTarget = createCollectionItem(
        collections = mutableListOf(targetCollection),
      )
      val secondCollection = creatCollection(
        items = mutableListOf(itemWithTarget),
      )

      val aggregate = AssessmentAggregate().apply {
        formVersion = "v1"
        collections.addAll(listOf(firstCollection, secondCollection))
      }

      val result = aggregate.getCollection(targetCollection.uuid)

      assertSame(targetCollection, result)
    }

    @Test
    fun `returns null when collection not found at any level`() {
      val missingCollectionUuid = UUID.randomUUID()
      val existingCollection = creatCollection()

      val aggregate = AssessmentAggregate().apply {
        formVersion = "v1"
        collections.add(existingCollection)
      }

      val result = aggregate.getCollection(missingCollectionUuid)

      assertNull(result)
    }

    @Test
    fun `returns null when collections list is empty`() {
      val aggregate = AssessmentAggregate().apply {
        formVersion = "v1"
      }
      val collectionUuid = UUID.randomUUID()

      val result = aggregate.getCollection(collectionUuid)

      assertNull(result)
    }
  }

  @Nested
  inner class GetCollectionItem {

    @Test
    fun `returns item when found in top-level collection`() {
      val targetCollectionItem = createCollectionItem()
      val collection = creatCollection(
        items = mutableListOf(targetCollectionItem),
      )

      val aggregate = AssessmentAggregate().apply {
        formVersion = "v1"
        collections.add(collection)
      }

      val result = aggregate.getCollectionItem(targetCollectionItem.uuid)

      assertSame(targetCollectionItem, result)
    }

    @Test
    fun `returns nested item when found in nested collections`() {
      val targetCollectionItem = createCollectionItem()

      val childCollection = creatCollection(
        items = mutableListOf(targetCollectionItem),
      )
      val parentCollectionItem = createCollectionItem(
        collections = mutableListOf(childCollection),
      )
      val parentCollection = creatCollection(
        items = mutableListOf(parentCollectionItem),
      )

      val aggregate = AssessmentAggregate().apply {
        formVersion = "v1"
        collections.add(parentCollection)
      }

      val result = aggregate.getCollectionItem(targetCollectionItem.uuid)

      assertSame(targetCollectionItem, result)
    }

    @Test
    fun `returns null when item not found`() {
      val missingItemUuid = UUID.randomUUID()
      val existingItem = createCollectionItem()

      val collection = creatCollection(
        items = mutableListOf(existingItem),
      )

      val aggregate = AssessmentAggregate().apply {
        formVersion = "v1"
        collections.add(collection)
      }

      val result = aggregate.getCollectionItem(missingItemUuid)

      assertNull(result)
    }

    @Test
    fun `returns null when collections list is empty`() {
      val aggregate = AssessmentAggregate().apply {
        formVersion = "v1"
      }
      val itemUuid = UUID.randomUUID()

      val result = aggregate.getCollectionItem(itemUuid)

      assertNull(result)
    }
  }
}
