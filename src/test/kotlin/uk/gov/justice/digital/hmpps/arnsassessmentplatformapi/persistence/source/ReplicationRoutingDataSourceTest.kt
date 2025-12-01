package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.source

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReplicationRoutingDataSourceTest {

  private val routingDataSource = ReplicationRoutingDataSource()

  @BeforeEach
  fun setup() {
    // Allow mocking object methods
    mockkObject(DataSourceContextHolder)
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
    DataSourceContextHolder.clear()
  }

  @Test
  fun `determineCurrentLookupKey should return READ when context is READ`() {
    // given
    every { DataSourceContextHolder.get() } returns DataSourceContextHolder.READ

    // when
    val key = routingDataSource.determineCurrentLookupKey()

    // then
    assertEquals(DataSourceContextHolder.READ, key)
  }

  @Test
  fun `determineCurrentLookupKey should return WRITE when context is WRITE`() {
    // given
    every { DataSourceContextHolder.get() } returns DataSourceContextHolder.WRITE

    // when
    val key = routingDataSource.determineCurrentLookupKey()

    // then
    assertEquals(DataSourceContextHolder.WRITE, key)
  }

  @Test
  fun `determineCurrentLookupKey should return WRITE if ThreadLocal is unset`() {
    // given: no explicit set
    every { DataSourceContextHolder.get() } returns DataSourceContextHolder.WRITE

    // when
    val key = routingDataSource.determineCurrentLookupKey()

    // then
    assertEquals(DataSourceContextHolder.WRITE, key)
  }

  @Test
  fun `determineCurrentLookupKey should call DataSourceContextHolder_get exactly once`() {
    // given
    every { DataSourceContextHolder.get() } returns DataSourceContextHolder.READ

    // when
    routingDataSource.determineCurrentLookupKey()

    // then
    io.mockk.verify(exactly = 1) { DataSourceContextHolder.get() }
  }
}
