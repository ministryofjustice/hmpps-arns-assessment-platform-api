package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.TestableEvent
import kotlin.test.Test
import kotlin.test.assertSame

class EventHandlerRegistryTest {
  private class SomeEvent : TestableEvent()
  private class OtherEvent : TestableEvent()

  @Nested
  inner class GetHandlersFor {
    @Test
    fun `returns handlers registered for a given event type`() {
      val handler1 =
        mockk<EventHandler<out Event, out AggregateState<out Aggregate<*>>>>()
      val handler2 =
        mockk<EventHandler<out Event, out AggregateState<out Aggregate<*>>>>()

      every { handler1.eventType } returns SomeEvent::class
      every { handler2.eventType } returns SomeEvent::class

      val registry = EventHandlerRegistry(listOf(handler1, handler2))

      val result = registry.getHandlersFor(SomeEvent::class)

      assertEquals(2, result.size)
      assertSame(handler1, result[0])
      assertSame(handler2, result[1])
    }

    @Test
    fun `groups handlers by event type and only returns matching ones`() {
      val someEventHandler =
        mockk<EventHandler<out Event, out AggregateState<out Aggregate<*>>>>()
      val otherEventHandler =
        mockk<EventHandler<out Event, out AggregateState<out Aggregate<*>>>>()

      every { someEventHandler.eventType } returns SomeEvent::class
      every { otherEventHandler.eventType } returns OtherEvent::class

      val registry = EventHandlerRegistry(listOf(someEventHandler, otherEventHandler))

      val resultForSome = registry.getHandlersFor(SomeEvent::class)

      assertEquals(1, resultForSome.size)
      assertSame(someEventHandler, resultForSome.single())
    }

    @Test
    fun `returns empty list when no handlers registered for a given event type`() {
      val handler =
        mockk<EventHandler<out Event, out AggregateState<out Aggregate<*>>>>()

      every { handler.eventType } returns SomeEvent::class

      val registry = EventHandlerRegistry(listOf(handler))

      val result = registry.getHandlersFor(OtherEvent::class)

      assertEquals(0, result.size)
    }

    @Test
    fun `returns empty list when the registry is constructed with empty handler list`() {
      val registry = EventHandlerRegistry(emptyList())

      val result = registry.getHandlersFor(SomeEvent::class)

      assertEquals(0, result.size)
    }
  }
}
