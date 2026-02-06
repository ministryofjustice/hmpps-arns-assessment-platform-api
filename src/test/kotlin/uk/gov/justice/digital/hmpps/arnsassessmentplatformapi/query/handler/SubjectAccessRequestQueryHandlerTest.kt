package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Nested
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.SubjectAccessRequestQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.exception.SubjectAccessRequestNoAssessmentsException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.RenderedValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestAssessmentVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestQueryResult
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test

class SubjectAccessRequestQueryHandlerTest : AbstractQueryHandlerTest() {
  override val handler = SubjectAccessRequestQueryHandler::class

  val crn = IdentifierPair(
    IdentifierType.CRN,
    "X123456",
  )
  val identifiers = setOf(crn)
  val fromDate: LocalDate = LocalDate.parse("2025-01-01")
  val toDate: LocalDate = LocalDate.parse("2025-02-01")

  val aggregate = AggregateEntity(
    eventsFrom = fromDate.atTime(LocalTime.parse("12:00:00")),
    eventsTo = toDate.atTime(LocalTime.parse("12:00:00")),
    assessment = assessment.apply {
      identifiers.add(
        AssessmentIdentifierEntity(
          externalIdentifier = crn,
          assessment = this,
        ),
      )
    },
    data = AssessmentAggregate().apply {
      formVersion = "1"
      answers["foo"] = SingleValue("foo_value")
    },
  )

  @Nested
  inner class Handle {
    @Test
    fun `it handles the query`() {
      every {
        assessmentService.findAllByExternalIdentifiers(
          identifiers,
          fromDate,
          toDate,
        )
      } returns setOf(assessment)
      every {
        stateProvider.fetchLatestStateBefore(
          assessment,
          toDate.atTime(LocalTime.MAX),
        )
      } returns state

      val query = SubjectAccessRequestQuery(
        timestamp = toDate.atTime(LocalTime.parse("12:00:00")),
        assessmentIdentifiers = identifiers,
        from = fromDate,
        to = toDate,
      )

      val expected = SubjectAccessRequestQueryResult(
        results = listOf(
          SubjectAccessRequestAssessmentVersion(
            assessmentType = "TEST",
            createdAt = fromDate.atTime(LocalTime.parse("12:00:00")),
            updatedAt = toDate.atTime(LocalTime.parse("12:00:00")),
            answers = listOf(
              RenderedValue("foo", single = "foo_value"),
            ),
            properties = emptyList(),
            collections = emptyList(),
            identifiers = mapOf(
              crn.type to crn.id,
            ),
          ),
        ),
      )

      test(query, aggregate, expected)
    }
  }

  @Test
  fun `it throws when no assessments are found for the given identifiers`() {
    every {
      assessmentService.findAllByExternalIdentifiers(
        setOf(IdentifierPair(IdentifierType.CRN, "UNKNOWN")),
        fromDate,
        toDate,
      )
    } returns setOf()
    every {
      stateProvider.fetchLatestStateBefore(
        assessment,
        toDate.atTime(LocalTime.MAX),
      )
    } returns state

    val query = SubjectAccessRequestQuery(
      timestamp = toDate.atTime(LocalTime.parse("12:00:00")),
      assessmentIdentifiers = setOf(IdentifierPair(IdentifierType.CRN, "UNKNOWN")),
      from = fromDate,
      to = toDate,
    )

    testThrows(
      query,
      aggregate,
      SubjectAccessRequestNoAssessmentsException(identifiers),
    )
  }

  override fun assertSuccessMockCallCount() {
    verify(exactly = 1) {
      assessmentService.findAllByExternalIdentifiers(
        identifiers,
        fromDate,
        toDate,
      )
    }
    verify(exactly = 1) { state.getForRead() }
    verify(exactly = 1) { stateProvider.fetchLatestStateBefore(assessment, any()) }
    verify(exactly = 1) { stateService.stateForType(AssessmentAggregate::class) }
  }
}
