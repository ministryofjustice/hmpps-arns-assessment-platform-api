package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.UserDetailsRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.cache.UserCache
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity

class UserDetailsServiceTest {

  private val userDetailsRepository: UserDetailsRepository = mockk()
  private val userCache: UserCache = mockk()
  private lateinit var service: UserDetailsService

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    service = UserDetailsService(userDetailsRepository, userCache)
  }

  @Test
  fun `returns existing user when found`() {
    // given
    val commandUser = UserDetails(
      id = "user-123",
      name = "Test User",
      authSource = AuthSource.HMPPS_AUTH,
    )

    val existingEntity = UserDetailsEntity(
      userId = "user-123",
      displayName = "Existing User",
      authSource = AuthSource.HMPPS_AUTH,
    )

    every { userCache.get(any()) } returns null
    every { userCache.put(existingEntity) } returns existingEntity

    every {
      userDetailsRepository.findByUserIdAndAuthSource(
        commandUser.id,
        commandUser.authSource,
      )
    } returns existingEntity

    // when
    val result = service.findOrCreate(commandUser)

    // then
    assertEquals(existingEntity, result)
    verify(exactly = 1) { userCache.get(commandUser) }
    verify(exactly = 1) { userCache.put(existingEntity) }
    verify(exactly = 1) {
      userDetailsRepository.findByUserIdAndAuthSource(
        commandUser.id,
        commandUser.authSource,
      )
    }
    verify(exactly = 0) {
      userDetailsRepository.save(any())
    }
  }

  @Test
  fun `returns existing user when found in cache`() {
    // given
    val commandUser = UserDetails(
      id = "user-123",
      name = "Test User",
      authSource = AuthSource.HMPPS_AUTH,
    )

    val existingEntity = UserDetailsEntity(
      userId = "user-123",
      displayName = "Existing User",
      authSource = AuthSource.HMPPS_AUTH,
    )

    every { userCache.get(commandUser) } returns existingEntity

    // when
    val result = service.findOrCreate(commandUser)

    // then
    assertEquals(existingEntity, result)
    verify(exactly = 1) { userCache.get(commandUser) }
    verify(exactly = 0) { userCache.put(any()) }
    verify(exactly = 0) { userDetailsRepository.findByUserIdAndAuthSource(any(), any()) }
    verify(exactly = 0) { userDetailsRepository.save(any()) }
  }

  @Test
  fun `creates and saves new user when not found`() {
    // given
    val commandUser = UserDetails(
      id = "user-456",
      name = "New User",
      authSource = AuthSource.HMPPS_AUTH,
    )

    every { userCache.get(any()) } returns null
    every { userCache.put(any()) } answers { firstArg() }

    every {
      userDetailsRepository.findByUserIdAndAuthSource(
        commandUser.id,
        commandUser.authSource,
      )
    } returns null

    every {
      userDetailsRepository.save(any())
    } answers { firstArg() }

    // when
    val result = service.findOrCreate(commandUser)

    // then
    assertEquals(commandUser.id, result.userId)
    assertEquals(commandUser.name, result.displayName)
    assertEquals(commandUser.authSource, result.authSource)

    verify(exactly = 1) { userCache.get(commandUser) }

    verify(exactly = 1) {
      userDetailsRepository.findByUserIdAndAuthSource(
        commandUser.id,
        commandUser.authSource,
      )
    }

    verify(exactly = 1) {
      userDetailsRepository.save(
        match {
          it.userId == commandUser.id &&
            it.displayName == commandUser.name &&
            it.authSource == commandUser.authSource
        },
      )
    }

    verify(exactly = 1) {
      userCache.put(
        match {
          it.userId == commandUser.id &&
            it.displayName == commandUser.name &&
            it.authSource == commandUser.authSource
        },
      )
    }
  }
}
