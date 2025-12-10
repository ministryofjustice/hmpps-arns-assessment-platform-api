package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID

class AssessmentVersionQueryHandlerTest : AbstractQueryHandlerTest() {
  override val handler = AssessmentVersionQueryHandler::class

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `returns the assessment data for a point in time`(timestamp: LocalDateTime?) {
    val aggregate = AggregateEntity(
      assessment = assessment,
      eventsFrom = LocalDateTime.parse("2020-06-01T10:42:43"),
      eventsTo = LocalDateTime.parse("2020-07-01T10:42:43"),
      data = AssessmentAggregate().apply {
        formVersion = "1"
        answers.put("foo", SingleValue("foo"))
        properties.put("bar", SingleValue("bar"))
        collections.add(
          Collection(
            uuid = UUID.randomUUID(),
            createdAt = LocalDateTime.parse("2020-06-01T10:42:43"),
            updatedAt = LocalDateTime.parse("2020-07-01T10:42:43"),
            name = "TEST_COLLECTION",
            items = mutableListOf(),
          ),
        )
        collaborators.add(user)
      },
    )

    val query = AssessmentVersionQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      timestamp = timestamp,
    )

    val expectedResult = AssessmentVersionQueryResult(
      assessmentUuid = assessment.uuid,
      aggregateUuid = aggregate.uuid,
      formVersion = "1",
      createdAt = aggregate.eventsFrom,
      updatedAt = aggregate.eventsTo,
      answers = aggregate.data.answers,
      properties = aggregate.data.properties,
      collections = aggregate.data.collections,
      collaborators = aggregate.data.collaborators,
    )

    test(query, aggregate, expectedResult)
  }
}
