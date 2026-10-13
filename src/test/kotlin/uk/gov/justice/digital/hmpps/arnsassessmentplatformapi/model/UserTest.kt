package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserTest {
  @Test
  fun `returns the unknown user without source metadata when the entity is null`() {
    val user = User.from(null)

    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), user.id)
    assertEquals("UNKNOWN", user.name)
    assertNull(user.userId)
    assertNull(user.authSource)
  }
}
