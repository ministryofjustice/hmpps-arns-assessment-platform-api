package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus

import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler.QueryHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.QueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService
import kotlin.test.assertEquals

class QueryBusTest {
  val auditService: AuditService = mockk()
  val queryResult: QueryResult = mockk()

  @BeforeEach
  fun setUp() {
    every { auditService.audit(any<RequestableQuery>()) } just runs
  }

  @Test
  fun `calls the handler for a given query and audits it`() {
    val handler = mockk<QueryHandler<out RequestableQuery>>()
    every { handler.execute(any()) } returns queryResult

    val registry: QueryHandlerRegistry = mockk()
    every { registry.getHandlerFor(any()) } returns handler

    val queryBus = QueryBus(registry, auditService)

    val result = queryBus.dispatch(mockk<RequestableQuery>())

    verify(exactly = 1) { registry.getHandlerFor(any()) }
    verify(exactly = 1) { handler.execute(any()) }
    verify(exactly = 1) { auditService.audit(any<RequestableQuery>()) }

    assertEquals(queryResult, result)
  }

  @Test
  fun `non-requestable query is not audited`() {
    val handler = mockk<QueryHandler<out TestableQuery>>()
    every { handler.execute(any()) } returns queryResult

    val registry: QueryHandlerRegistry = mockk()
    every { registry.getHandlerFor(any()) } returns handler

    val queryBus = QueryBus(registry, auditService)

    val result = queryBus.dispatch(mockk<TestableQuery>())

    verify(exactly = 1) { registry.getHandlerFor(any()) }
    verify(exactly = 1) { handler.execute(any()) }
    verify { auditService wasNot Called }

    assertEquals(queryResult, result)
  }
}
