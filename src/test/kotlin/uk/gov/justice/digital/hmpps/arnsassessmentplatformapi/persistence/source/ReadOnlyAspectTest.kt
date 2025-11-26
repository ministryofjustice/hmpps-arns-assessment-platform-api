package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.source

import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReadOnlyAspectTest {

  private val aspect = ReadOnlyAspect()

  @AfterEach
  fun tearDown() {
    DataSourceContextHolder.clear()
  }

  @Test
  fun `setReadOnly() should set ThreadLocal to READ`() {
    // given
    mockkObject(DataSourceContextHolder)

    // when
    aspect.setReadOnly()

    // then
    verify { DataSourceContextHolder.set(DataSourceContextHolder.READ) }
  }

  @Test
  fun `clear() should call ThreadLocal clear`() {
    // given
    mockkObject(DataSourceContextHolder)

    // when
    aspect.clear()

    // then
    verify { DataSourceContextHolder.clear() }
  }

  @Test
  fun `order should be higher precedence than Transactional annotation`() {
    // this ensures the aspect executes BEFORE @Transactional advice
    val base = jakarta.transaction.Transactional::class.java.getAnnotation(
      org.springframework.core.annotation.Order::class.java
    )?.value ?: 0

    val expected = base - 1

    assertEquals(expected, aspect.order)
  }
}
