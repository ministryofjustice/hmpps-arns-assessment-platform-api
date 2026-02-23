package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.MultiValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

class SubjectAccessRequestAssessmentVersionTest {
  val now: LocalDateTime = LocalDateTime.now()

  @Nested
  inner class From {
    @Test
    fun `it creates a an assessment version from an assessment and aggregate`() {
      val yesterday = LocalDateTime.now().minusDays(1)
      val today = LocalDateTime.now()

      val assessmentType = "TEST"
      val identifier = IdentifierPair(IdentifierType.CRN, "X123456")
      val collectionName = "TEST_COLLECTION_NAME"

      val assessment = AssessmentEntity(
        type = assessmentType,
        identifiers = mutableListOf(),
      ).apply {
        identifiers.add(
          AssessmentIdentifierEntity(
            externalIdentifier = identifier,
            assessment = this,
          ),
        )
      }

      val aggregate = AggregateEntity(
        assessment = assessment,
        eventsFrom = yesterday,
        eventsTo = today,
        data = AssessmentAggregate()
          .apply {
            formVersion = "v1.0"
            answers["foo"] = SingleValue("foo_value")
            properties["bar"] = MultiValue(listOf("foo", "bar", "baz"))
            collections.add(
              Collection(
                uuid = UUID.randomUUID(),
                createdAt = yesterday,
                updatedAt = today,
                name = collectionName,
                items = mutableListOf(),
              ),
            )
          },
      )

      val result = SubjectAccessRequestAssessmentVersion.from(aggregate = aggregate, assessment = assessment)

      val expected = SubjectAccessRequestAssessmentVersion(
        assessmentType = assessmentType,
        createdAt = yesterday,
        updatedAt = today,
        answers = listOf(RenderedValue("foo", "foo_value", null)),
        properties = listOf(RenderedValue("bar", null, listOf("foo", "bar", "baz"))),
        collections = listOf(
          RenderedCollection(
            name = collectionName,
            items = emptyList(),
          ),
        ),
        identifiers = mapOf(identifier.type to identifier.id),
      )

      assertEquals(expected, result)
    }
  }

  @Nested
  inner class MapToRenderedValues {
    @Test
    fun `it converts a SingleValue type`() {
      val result = mapOf("foo" to SingleValue("foo_value")).toRenderedValues()

      val expected = listOf(RenderedValue("foo", "foo_value", null))
      assertEquals(expected, result)
    }

    @Test
    fun `it converts a MultiValue type`() {
      val result = mapOf("foo" to MultiValue(listOf("bar_value", "baz_value"))).toRenderedValues()

      val expected = listOf(RenderedValue("foo", null, listOf("bar_value", "baz_value")))
      assertEquals(expected, result)
    }
  }

  @Nested
  inner class ListToRenderedCollection {
    val collectionName = "TEST_COLLECTION_NAME"

    @Test
    fun `it converts a Collection with no items`() {
      val collection = Collection(
        uuid = UUID.randomUUID(),
        createdAt = now.minusDays(1),
        updatedAt = now,
        name = collectionName,
        items = mutableListOf(),
      )

      val result = listOf(collection).toFlattenedRenderedCollections()

      val expected = listOf(
        RenderedCollection(
          name = collectionName,
          items = mutableListOf(),
        ),
      )
      assertEquals(expected, result)
    }

    @Test
    fun `it converts a Collection with items`() {
      val collection = Collection(
        uuid = UUID.randomUUID(),
        createdAt = now.minusDays(1),
        updatedAt = now,
        name = collectionName,
        items = mutableListOf(),
      )

      val items = listOf(
        CollectionItem(
          uuid = UUID.randomUUID(),
          createdAt = now,
          updatedAt = now,
          answers = mutableMapOf("first_foo" to SingleValue("first_foo_value")),
          properties = mutableMapOf("first_bar" to SingleValue("first_bar_value")),
          collections = mutableListOf(),
        ),
        CollectionItem(
          uuid = UUID.randomUUID(),
          createdAt = now,
          updatedAt = now,
          answers = mutableMapOf("second_foo" to SingleValue("second_foo_value")),
          properties = mutableMapOf("second_bar" to MultiValue(listOf("foo", "bar", "baz"))),
          collections = mutableListOf(),
        ),
      )

      collection.items.addAll(items)

      val result = listOf(collection).toFlattenedRenderedCollections()

      val expected = listOf(
        RenderedCollection(
          name = collectionName,
          items = mutableListOf(
            RenderedCollectionItem(
              name = "$collectionName/1",
              answers = listOf(RenderedValue("first_foo", "first_foo_value", null)),
              properties = listOf(RenderedValue("first_bar", "first_bar_value", null)),
            ),
            RenderedCollectionItem(
              name = "$collectionName/2",
              answers = listOf(RenderedValue("second_foo", "second_foo_value", null)),
              properties = listOf(RenderedValue("second_bar", null, listOf("foo", "bar", "baz"))),
            ),
          ),
        ),
      )
      assertEquals(expected, result)
    }

    @Test
    fun `it converts nested collections`() {
      val childCollectionName = "TEST_CHILD_COLLECTION_NAME"

      val parentCollection = Collection(
        uuid = UUID.randomUUID(),
        createdAt = now.minusDays(1),
        updatedAt = now,
        name = collectionName,
        items = mutableListOf(),
      )

      val parentItems = listOf(
        CollectionItem(
          uuid = UUID.randomUUID(),
          createdAt = now,
          updatedAt = now,
          answers = mutableMapOf("parent_foo" to SingleValue("parent_foo_value")),
          properties = mutableMapOf("parent_bar" to SingleValue("parent_bar_value")),
          collections = mutableListOf(),
        ),
      )

      val childCollection = Collection(
        uuid = UUID.randomUUID(),
        createdAt = now.minusDays(1),
        updatedAt = now,
        name = childCollectionName,
        items = mutableListOf(),
      )

      val childItems = listOf(
        CollectionItem(
          uuid = UUID.randomUUID(),
          createdAt = now,
          updatedAt = now,
          answers = mutableMapOf("child_foo" to SingleValue("child_foo_value")),
          properties = mutableMapOf("child_bar" to SingleValue("child_bar_value")),
          collections = mutableListOf(),
        ),
      )

      childCollection.items.addAll(childItems)
      parentItems.first().collections.add(childCollection)
      parentCollection.items.addAll(parentItems)

      val result = listOf(parentCollection).toFlattenedRenderedCollections()

      val expectedChildCollections = listOf(
        RenderedCollection(
          name = "$collectionName/1/$childCollectionName",
          items = listOf(
            RenderedCollectionItem(
              name = "$collectionName/1/$childCollectionName/1",
              answers = listOf(RenderedValue("child_foo", "child_foo_value", null)),
              properties = listOf(RenderedValue("child_bar", "child_bar_value", null)),
            ),
          ),
        ),
      )

      val expected = listOf(
        RenderedCollection(
          name = collectionName,
          items = mutableListOf(
            RenderedCollectionItem(
              name = "$collectionName/1",
              answers = listOf(RenderedValue("parent_foo", "parent_foo_value", null)),
              properties = listOf(RenderedValue("parent_bar", "parent_bar_value", null)),
            ),
          ),
        ),
      ).plus(expectedChildCollections)

      assertEquals(expected, result)
    }
  }
}
