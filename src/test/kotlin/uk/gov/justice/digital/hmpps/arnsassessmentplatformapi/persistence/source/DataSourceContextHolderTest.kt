package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.source

import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DataSourceContextHolderTest {

  @AfterEach
  fun tearDown() {
    // ensure ThreadLocal is cleared between tests
    DataSourceContextHolder.clear()
    unmockkAll()
  }

  @Test
  fun `get() should return WRITE by default`() {
    // when
    val result = DataSourceContextHolder.get()

    // then
    assertEquals(DataSourceContextHolder.WRITE, result)
  }

  @Test
  fun `set() should store the mode in ThreadLocal`() {
    // when
    DataSourceContextHolder.set(DataSourceContextHolder.READ)

    // then
    assertEquals(DataSourceContextHolder.READ, DataSourceContextHolder.get())
  }

  @Test
  fun `clear() should remove ThreadLocal and reset to WRITE`() {
    // given
    DataSourceContextHolder.set(DataSourceContextHolder.READ)

    // sanity check
    assertEquals(DataSourceContextHolder.READ, DataSourceContextHolder.get())

    // when
    DataSourceContextHolder.clear()

    // then
    assertEquals(DataSourceContextHolder.WRITE, DataSourceContextHolder.get())
  }

  @Test
  fun `ThreadLocal should be isolated between threads`() {
    // given - main thread sets READ
    DataSourceContextHolder.set(DataSourceContextHolder.READ)

    var threadValue: String? = null

    // when - new thread should not inherit the value
    val thread = Thread {
      threadValue = DataSourceContextHolder.get()
    }
    thread.start()
    thread.join()

    // then - new thread sees default WRITE
    assertEquals(DataSourceContextHolder.WRITE, threadValue)

    // and the main thread still has READ
    assertEquals(DataSourceContextHolder.READ, DataSourceContextHolder.get())
  }
}
