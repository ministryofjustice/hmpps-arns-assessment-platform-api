package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model

import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertNull
import kotlin.test.assertSame

class CollectionItemTest {
  private fun newCollection(
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

  private fun newItem(
    id: UUID = UUID.randomUUID(),
    collections: MutableList<Collection> = mutableListOf(),
  ): CollectionItem = CollectionItem(
    uuid = id,
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
    answers = mockk(relaxed = true), // not used by findCollection
    properties = mockk(relaxed = true), // not used by findCollection
    collections = collections,
  )

  @Nested
  inner class FindCollection {

    @Test
    fun `returns collection when present directly in collections list`() {
      val otherCollection = newCollection()
      val targetCollection = newCollection()

      val collectionItem = newItem(
        collections = mutableListOf(otherCollection, targetCollection),
      )

      val result = collectionItem.findCollection(targetCollection.uuid)

      assertSame(targetCollection, result)
    }

    @Test
    fun `returns nested collection in child items`() {
      val targetCollection = newCollection()

      val childCollectionItem = newItem(
        collections = mutableListOf(targetCollection),
      )

      val childCollection = newCollection(
        items = mutableListOf(childCollectionItem),
      )

      val rootCollectionItem = newItem(
        collections = mutableListOf(childCollection),
      )

      val result = rootCollectionItem.findCollection(targetCollection.uuid)

      assertSame(targetCollection, result)
    }

    @Test
    fun `returns null when collection id is not found at any level`() {
      val missingId = UUID.randomUUID()

      val existingCollection = newCollection()
      val collectionItem = newItem(
        collections = mutableListOf(existingCollection),
      )

      val result = collectionItem.findCollection(missingId)

      assertNull(result)
    }

    @Test
    fun `returns null when collections list is empty`() {
      val collectionItem = newItem(collections = mutableListOf())

      val result = collectionItem.findCollection(UUID.randomUUID())

      assertNull(result)
    }

    @Test
    fun `returns first matching collection when duplicates exist at root level`() {
      val duplicateId = UUID.randomUUID()

      val first = newCollection(id = duplicateId)
      val second = newCollection(id = duplicateId)

      val collectionItem = newItem(
        collections = mutableListOf(first, second),
      )

      val result = collectionItem.findCollection(duplicateId)

      assertSame(first, result)
    }

    @Test
    fun `searches subsequent collections when earlier branch contains no match`() {
      val firstChildCollectionItem = newItem()
      val firstChildCollection = newCollection(
        items = mutableListOf(firstChildCollectionItem),
      )

      val targetCollection = newCollection()
      val secondChildCollectionItem = newItem(
        collections = mutableListOf(targetCollection),
      )
      val secondChildCollection = newCollection(
        items = mutableListOf(secondChildCollectionItem),
      )

      val parentCollectionItem = newItem(
        collections = mutableListOf(firstChildCollection, secondChildCollection),
      )

      val result = parentCollectionItem.findCollection(targetCollection.uuid)

      assertSame(targetCollection, result)
    }
  }
}
