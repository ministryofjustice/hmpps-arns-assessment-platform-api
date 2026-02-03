package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.UserDetailsRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity

class UserDetailsServiceTest {

  private val userDetailsRepository: UserDetailsRepository = mockk()
  private lateinit var service: UserDetailsService

  @BeforeEach
  fun setUp() {
    service = UserDetailsService(userDetailsRepository)
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
  fun `creates and saves new user when not found`() {
    // given
    val commandUser = UserDetails(
      id = "user-456",
      name = "New User",
      authSource = AuthSource.HMPPS_AUTH,
    )

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
  }
}
