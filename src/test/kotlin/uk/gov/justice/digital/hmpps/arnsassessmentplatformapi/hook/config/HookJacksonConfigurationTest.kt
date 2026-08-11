package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.TestableHook

// TestableHook carries no @JsonSubTypes entry anywhere - this proves the classpath scan in
// HookJacksonConfiguration finds and registers it purely from its @HookType annotation.
class HookJacksonConfigurationTest {
  private val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(HookJacksonConfiguration().hookSubtypeModule())

  @Test
  fun `serializes a Hook implementation tagged with its @HookType name`() {
    val json = objectMapper.writeValueAsString(TestableHook(param = "test") as Hook)

    assertTrue(json.contains(""""type":"TestableHook""""))
  }

  @Test
  fun `deserializes JSON back into the concrete Hook implementation by its @HookType name`() {
    val hook = objectMapper.readValue("""{"type":"TestableHook","param":"test"}""", Hook::class.java)

    assertEquals(TestableHook(param = "test"), hook)
  }
}
