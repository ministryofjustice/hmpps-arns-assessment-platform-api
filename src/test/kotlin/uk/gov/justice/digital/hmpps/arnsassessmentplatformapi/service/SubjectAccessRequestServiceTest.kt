package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.SubjectAccessRequestQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus.QueryBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.exception.SubjectAccessRequestNoAssessmentsException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestAssessmentVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestQueryResult
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class SubjectAccessRequestServiceTest {
  val queryBus: QueryBus = mockk()
  val service = SubjectAccessRequestService(queryBus)

  @BeforeEach
  fun setup() {
    clearMocks(queryBus)
  }

  @Nested
  inner class GetContentFor {
    val crn = "X123456"
    val prn = "1234567"
    val fromDate: LocalDate = LocalDate.parse("2026-01-01")
    val toDate: LocalDate = LocalDate.parse("2026-02-01")
    val identifiers = setOf(
      IdentifierPair(IdentifierType.CRN, crn),
      IdentifierPair(IdentifierType.PRN, prn),
    )

    @Test
    fun `it dispatches the query and returns the result`() {
      val queryResult = SubjectAccessRequestQueryResult(
        results = listOf(
          SubjectAccessRequestAssessmentVersion(
            assessmentType = "TEST",
            createdAt = LocalDateTime.parse("2026-01-01T12:00:00"),
            updatedAt = LocalDateTime.parse("2026-01-01T14:30:00"),
            answers = emptyList(),
            properties = emptyList(),
            collections = emptyList(),
            identifiers = identifiers.associate { it.type to it.id },
          ),
        ),
      )

      every {
        queryBus.dispatch(
          match<SubjectAccessRequestQuery> { query ->
            listOf(
              query.assessmentIdentifiers == identifiers,
              query.from?.isEqual(fromDate) == true,
              query.to?.isEqual(toDate) == true,
            ).all { it }
          },
        )
      } returns queryResult

      val result = service.getContentFor(
        prn = prn,
        crn = crn,
        fromDate = fromDate,
        toDate = toDate,
      )

      assertEquals(HmppsSubjectAccessRequestContent(content = queryResult), result)
    }

    @Test
    fun `it bubbles up exceptions thrown by the query handler`() {
      every {
        queryBus.dispatch(any<SubjectAccessRequestQuery>())
      } throws SubjectAccessRequestNoAssessmentsException(identifiers)

      assertThrows<SubjectAccessRequestNoAssessmentsException> {
        service.getContentFor(
          prn = prn,
          crn = crn,
          fromDate = LocalDate.parse("2026-01-01"),
          toDate = LocalDate.parse("2026-02-01"),
        )
      }
    }
  }
}
