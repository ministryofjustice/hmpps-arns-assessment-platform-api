package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.CollectionItemQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.exception.CollectionDepthOutOfBoundsException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.CollectionItemQueryResult
import java.time.LocalDateTime
import java.util.UUID

class CollectionItemQueryHandlerTest : AbstractQueryHandlerTest() {
  override val handler = CollectionItemQueryHandler::class

  val uuid = (1..10).map { UUID.randomUUID() }
  val now: LocalDateTime = LocalDateTime.now()

  val allCollections = listOf(
    Collection(
      uuid = uuid[0],
      name = "LEVEL_0",
      createdAt = now,
      updatedAt = now,
      items = mutableListOf(
        CollectionItem(
          uuid = uuid[1],
          createdAt = now,
          updatedAt = now,
          answers = mutableMapOf("foo" to SingleValue("bar")),
          properties = mutableMapOf(),
          collections = mutableListOf(),
        ),
        CollectionItem(
          uuid = uuid[2],
          createdAt = now,
          updatedAt = now,
          answers = mutableMapOf("bar" to SingleValue("baz")),
          properties = mutableMapOf(),
          collections = mutableListOf(
            Collection(
              uuid = uuid[3],
              name = "LEVEL_1",
              createdAt = now,
              updatedAt = now,
              items = mutableListOf(
                CollectionItem(
                  uuid = uuid[4],
                  createdAt = now,
                  updatedAt = now,
                  answers = mutableMapOf("baz" to SingleValue("foo")),
                  properties = mutableMapOf(),
                  collections = mutableListOf(),
                ),
              ),
            ),
            Collection(
              uuid = uuid[5],
              name = "LEVEL_1",
              createdAt = now,
              updatedAt = now,
              items = mutableListOf(),
            ),
          ),
        ),
      ),
    ),
    Collection(
      uuid = uuid[6],
      name = "LEVEL_0",
      createdAt = now,
      updatedAt = now,
      items = mutableListOf(
        CollectionItem(
          uuid = uuid[7],
          createdAt = now,
          updatedAt = now,
          answers = mutableMapOf("foo" to SingleValue("baz")),
          properties = mutableMapOf(),
          collections = mutableListOf(),
        ),
      ),
    ),
  )

  val aggregate = AggregateEntity(
    assessment = assessment,
    data = AssessmentAggregate().apply {
      formVersion = "1"
      collections.addAll(allCollections)
    },
  )

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `returns the collection item data for depth=-1 and a point in time`(timestamp: LocalDateTime?) {
    val query = CollectionItemQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      collectionItemUuid = uuid[2],
      depth = -1,
      timestamp = timestamp,
    )

    val expectedResult = CollectionItemQueryResult(
      collectionItem = allCollections[0].items[1],
    )

    test(query, aggregate, expectedResult)
  }

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `returns the collection item data for depth=0 and a point in time`(timestamp: LocalDateTime?) {
    val query = CollectionItemQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      collectionItemUuid = uuid[2],
      depth = 0,
      timestamp = timestamp,
    )

    val expectedResult = CollectionItemQueryResult(
      collectionItem = CollectionItem(
        uuid = uuid[2],
        createdAt = now,
        updatedAt = now,
        answers = mutableMapOf("bar" to SingleValue("baz")),
        properties = mutableMapOf(),
        collections = mutableListOf(
          Collection(
            uuid = uuid[3],
            name = "LEVEL_1",
            createdAt = now,
            updatedAt = now,
            items = mutableListOf(),
          ),
          Collection(
            uuid = uuid[5],
            name = "LEVEL_1",
            createdAt = now,
            updatedAt = now,
            items = mutableListOf(),
          ),
        ),
      ),
    )

    test(query, aggregate, expectedResult)
  }

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `returns the collection item data for depth=1 and a point in time`(timestamp: LocalDateTime?) {
    val query = CollectionItemQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      collectionItemUuid = uuid[2],
      depth = 1,
      timestamp = timestamp,
    )

    val expectedResult = CollectionItemQueryResult(
      collectionItem = CollectionItem(
        uuid = uuid[2],
        createdAt = now,
        updatedAt = now,
        answers = mutableMapOf("bar" to SingleValue("baz")),
        properties = mutableMapOf(),
        collections = mutableListOf(
          Collection(
            uuid = uuid[3],
            name = "LEVEL_1",
            createdAt = now,
            updatedAt = now,
            items = mutableListOf(
              CollectionItem(
                uuid = uuid[4],
                createdAt = now,
                updatedAt = now,
                answers = mutableMapOf("baz" to SingleValue("foo")),
                properties = mutableMapOf(),
                collections = mutableListOf(),
              ),
            ),
          ),
          Collection(
            uuid = uuid[5],
            name = "LEVEL_1",
            createdAt = now,
            updatedAt = now,
            items = mutableListOf(),
          ),
        ),
      ),
    )

    test(query, aggregate, expectedResult)
  }

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `finds a deeply nested collection item and returns its data for depth=0 and a point in time`(timestamp: LocalDateTime?) {
    val query = CollectionItemQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      collectionItemUuid = uuid[4],
      depth = 0,
      timestamp = timestamp,
    )

    val expectedResult = CollectionItemQueryResult(
      collectionItem = CollectionItem(
        uuid = uuid[4],
        createdAt = now,
        updatedAt = now,
        answers = mutableMapOf("baz" to SingleValue("foo")),
        properties = mutableMapOf(),
        collections = mutableListOf(),
      ),
    )

    test(query, aggregate, expectedResult)
  }

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `throws when the collection item does not exist for a point in time`(timestamp: LocalDateTime?) {
    val query = CollectionItemQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      collectionItemUuid = UUID.randomUUID(),
      depth = 0,
      timestamp = timestamp,
    )

    testThrows(query, aggregate, CollectionItemNotFoundException(query.collectionItemUuid))
  }

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `throws when the requested depth is invalid`(timestamp: LocalDateTime?) {
    val query = CollectionItemQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      collectionItemUuid = uuid[2],
      depth = -2,
      timestamp = timestamp,
    )

    testThrows(query, aggregate, CollectionDepthOutOfBoundsException(query.depth, query.collectionItemUuid))
  }
}
