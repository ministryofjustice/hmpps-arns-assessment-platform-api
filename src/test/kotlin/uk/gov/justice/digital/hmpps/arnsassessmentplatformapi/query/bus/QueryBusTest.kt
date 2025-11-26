package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus

import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler.QueryHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.QueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TestableQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService
import kotlin.test.assertEquals

class QueryBusTest {

  @Nested
  inner class DispatchSingle {
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

  @Nested
  inner class DispatchMultiple {
    private val queryBus = spyk(QueryBus(mockk(), mockk()), recordPrivateCalls = true)

    @Test
    fun `dispatch should map each Query into a QueryResult`() {
      // given
      val queries = listOf(
        TestableQuery(param = "test-1"),
        TestableQuery(param = "test-2"),
        TestableQuery(param = "test-3"),
      )

      // mock single-query dispatch
      every { queryBus.dispatch(any<TestableQuery>()) } answers { TestableQueryResult("result-${firstArg<TestableQuery>().param}") }

      // when
      val response = queryBus.dispatch(queries)

      // then: ensure the correct number of responses
      assertEquals(3, response.queries.size)

      // ensure objects are mapped correctly
      assertEquals(TestableQuery(param = "test-1"), response.queries[0].request)
      assertEquals(TestableQueryResult("result-test-1"), response.queries[0].result)

      assertEquals(TestableQuery(param = "test-2"), response.queries[1].request)
      assertEquals(TestableQueryResult("result-test-2"), response.queries[1].result)

      assertEquals(TestableQuery(param = "test-3"), response.queries[2].request)
      assertEquals(TestableQueryResult("result-test-3"), response.queries[2].result)

      // verify the inner dispatch(query) was called once per query
      verify(exactly = 3) { queryBus.dispatch(any<TestableQuery>()) }
    }

    @Test
    fun `dispatch should work with empty list`() {
      // when
      val response = queryBus.dispatch(emptyList())

      // then
      assertEquals(0, response.queries.size)
    }
  }
}
