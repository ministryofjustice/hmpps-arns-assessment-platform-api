package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertSame

class CollectionTest {
  private fun createParentCollection(
    name: String = "test-collection",
    items: MutableList<CollectionItem> = mutableListOf(),
  ): Collection = Collection(
    uuid = UUID.randomUUID(),
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
    name = name,
    items = items,
  )

  private fun newItem(
    id: UUID = UUID.randomUUID(),
    collections: MutableList<Collection> = mutableListOf(),
  ): CollectionItem = mockk(relaxed = true) {
    every { uuid } returns id
    every { this@mockk.collections } returns collections
  }

  @Nested
  inner class FindItem {

    @Test
    fun `returns a root item`() {
      val item1 = newItem(UUID.randomUUID())
      val item2 = newItem(UUID.randomUUID())
      val root = createParentCollection(items = mutableListOf(item1, item2))

      val result = root.findItem(item2.uuid)

      assertSame(item2, result)
    }

    @Test
    fun `returns a nested item`() {
      val childCollectionItem = newItem(UUID.randomUUID())
      val childCollection = createParentCollection(
        name = "child",
        items = mutableListOf(childCollectionItem),
      )

      val parentCollectionItem = newItem(
        id = UUID.randomUUID(),
        collections = mutableListOf(childCollection),
      )

      val root = createParentCollection(items = mutableListOf(parentCollectionItem))

      val result = root.findItem(childCollectionItem.uuid)

      assertSame(childCollectionItem, result)
    }

    @Test
    fun `returns null when item not present in any level`() {
      val missingItemUuid = UUID.randomUUID()

      val existingItem = newItem(UUID.randomUUID())
      val root = createParentCollection(items = mutableListOf(existingItem))

      val result = root.findItem(missingItemUuid)

      assertNull(result)
    }

    @Test
    fun `returns null for empty items list`() {
      val root = createParentCollection(items = mutableListOf())

      val result = root.findItem(UUID.randomUUID())

      assertNull(result)
    }

    @Test
    fun `returns first match when duplicate ids exist`() {
      val duplicateItemUuid = UUID.randomUUID()

      val first = newItem(duplicateItemUuid)
      val second = newItem(duplicateItemUuid)
      val root = createParentCollection(items = mutableListOf(first, second))

      val result = root.findItem(duplicateItemUuid)

      assertSame(first, result)
    }
  }

  @Nested
  inner class RemoveItem {

    @Test
    fun `removes and returns true when the item is present at the root level`() {
      val itemToRemove = newItem(UUID.randomUUID())
      val otherItem = newItem(UUID.randomUUID())
      val items = mutableListOf(itemToRemove, otherItem)
      val root = createParentCollection(items = items)

      val result = root.removeItem(itemToRemove.uuid)

      assertTrue(result)
      assertEquals(1, items.size)
      assertFalse(items.any { it.uuid == itemToRemove.uuid })
    }

    @Test
    fun `removes and returns true for nested item`() {
      val childCollectionItem = newItem(UUID.randomUUID())
      val childItems = mutableListOf(childCollectionItem)
      val childCollection = createParentCollection(
        name = "child",
        items = childItems,
      )

      val parentCollectionItem = newItem(
        id = UUID.randomUUID(),
        collections = mutableListOf(childCollection),
      )

      val parentCollectionItems = mutableListOf(parentCollectionItem)
      val parentCollection = createParentCollection(items = parentCollectionItems)

      val result = parentCollection.removeItem(childCollectionItem.uuid)

      assertTrue(result)
      assertTrue(parentCollectionItems.contains(parentCollectionItem), "parent item should remain at root")
      assertTrue(childItems.none { it.uuid == childCollectionItem.uuid }, "nested item should be removed")
    }

    @Test
    fun `returns false and does not modify items when id not found`() {
      val missingItemUuid = UUID.randomUUID()
      val presentItem = newItem(UUID.randomUUID())
      val items = mutableListOf(presentItem)
      val parentCollection = createParentCollection(items = items.toMutableList())

      val result = parentCollection.removeItem(missingItemUuid)

      assertFalse(result)
      assertEquals(items.size, parentCollection.items.size)
      assertEquals(items.map { it.uuid }, parentCollection.items.map { it.uuid })
    }

    @Test
    fun `returns false for empty items list`() {
      val parentCollection = createParentCollection(items = mutableListOf())

      val result = parentCollection.removeItem(UUID.randomUUID())

      assertFalse(result)
      assertTrue(parentCollection.items.isEmpty())
    }
  }

  @Nested
  inner class ReorderItem {

    @Test
    fun `reorders an item to a given index at the root level`() {
      val item1 = newItem(UUID.randomUUID())
      val item2 = newItem(UUID.randomUUID())
      val item3 = newItem(UUID.randomUUID())

      val items = mutableListOf(item1, item2, item3)
      val parentCollection = createParentCollection(items = items)

      val result = parentCollection.reorderItem(item2.uuid, 0)

      assertTrue(result)
      assertEquals(listOf(item2.uuid, item1.uuid, item3.uuid), parentCollection.items.map { it.uuid })
    }

    @Test
    fun `coerces negative target index to zero`() {
      val item1 = newItem(UUID.randomUUID())
      val item2 = newItem(UUID.randomUUID())

      val items = mutableListOf(item1, item2)
      val parentCollection = createParentCollection(items = items)

      val result = parentCollection.reorderItem(item2.uuid, -10)

      assertTrue(result)
      assertEquals(listOf(item2.uuid, item1.uuid), parentCollection.items.map { it.uuid })
    }

    @Test
    fun `coerces target index greater than size to end of list`() {
      val item1 = newItem(UUID.randomUUID())
      val item2 = newItem(UUID.randomUUID())
      val item3 = newItem(UUID.randomUUID())

      val items = mutableListOf(item1, item2, item3)
      val parentCollection = createParentCollection(items = items)

      val result = parentCollection.reorderItem(item1.uuid, 999)

      assertTrue(result)
      assertEquals(listOf(item2.uuid, item3.uuid, item1.uuid), parentCollection.items.map { it.uuid })
    }

    @Test
    fun `reorders nested item in child collection`() {
      val childItem1 = newItem(UUID.randomUUID())
      val childItem2 = newItem(UUID.randomUUID())

      val childCollectionItems = mutableListOf(childItem1, childItem2)
      val childCollection = createParentCollection(
        name = "child",
        items = childCollectionItems,
      )

      val parentCollectionItem = newItem(
        id = UUID.randomUUID(),
        collections = mutableListOf(childCollection),
      )

      val parentCollection = createParentCollection(items = mutableListOf(parentCollectionItem))

      val result = parentCollection.reorderItem(childItem2.uuid, 0)

      assertTrue(result)
      assertEquals(listOf(childItem2.uuid, childItem1.uuid), childCollection.items.map { it.uuid })
    }

    @Test
    fun `returns false when item not found at any level`() {
      val presentItem = newItem(UUID.randomUUID())
      val missingItemUuid = UUID.randomUUID()
      val parentCollection = createParentCollection(items = mutableListOf(presentItem))

      val originalOrder = parentCollection.items.map { it.uuid }

      val result = parentCollection.reorderItem(missingItemUuid, 0)

      assertFalse(result)
      assertEquals(originalOrder, parentCollection.items.map { it.uuid })
    }

    @Test
    fun `returns false and does nothing for empty items list`() {
      val parentCollection = createParentCollection(items = mutableListOf())

      val result = parentCollection.reorderItem(UUID.randomUUID(), 1)

      assertFalse(result)
      assertTrue(parentCollection.items.isEmpty())
    }
  }
}
