package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Query
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.exception.HandlerNotImplementedException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler.QueryHandler
import kotlin.reflect.KClass

class QueryHandlerRegistryTest {
  @Test
  fun `should return handler for registered query type`() {
    val queryKClass = mockk<KClass<out Query>>()
    val handler = mockk<QueryHandler<out Query>>()
    every { handler.type } returns queryKClass

    val registry = QueryHandlerRegistry(listOf(handler))

    assertEquals(handler, registry.getHandlerFor(queryKClass))
  }

  @Test
  fun `should throw when no handler is registered for the query`() {
    val registry = QueryHandlerRegistry(emptyList())

    val exception = assertThrows(HandlerNotImplementedException::class.java) {
      registry.getHandlerFor(Query::class)
    }

    assertEquals(exception.message, "Unable to dispatch query")
    assertEquals(exception.developerMessage, "No handler registered for type: Query")
  }

  @Test
  fun `should handle multiple query handlers`() {
    val queryKClass1 = mockk<KClass<out Query>>()
    val handler1 = mockk<QueryHandler<out Query>>()
    every { handler1.type } returns queryKClass1

    val queryKClass2 = mockk<KClass<out RequestableQuery>>()
    val handler2 = mockk<QueryHandler<out RequestableQuery>>()
    every { handler2.type } returns queryKClass2

    val registry = QueryHandlerRegistry(listOf(handler1, handler2))

    assertEquals(handler1, registry.getHandlerFor(queryKClass1))
    assertEquals(handler2, registry.getHandlerFor(queryKClass2))
  }
}
