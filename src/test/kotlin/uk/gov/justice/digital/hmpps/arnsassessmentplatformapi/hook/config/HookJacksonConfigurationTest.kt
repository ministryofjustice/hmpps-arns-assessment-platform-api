package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.TestableHook

// TestableHook carries no @JsonSubTypes entry anywhere - this proves the classpath scan in
// HookJacksonConfiguration finds and registers it purely from its @HookType annotation.
// Uses the Jackson 3 JsonMapper (tools.jackson.databind), matching the mapper Spring Boot actually
// wires up at runtime - a plain com.fasterxml.jackson.databind.ObjectMapper here would pass even if the
// module bean's type is invisible to Spring's Jackson 3 auto-configuration.
class HookJacksonConfigurationTest {
  private val objectMapper: JsonMapper = JsonMapper.builder()
    .addModule(HookJacksonConfiguration().hookSubtypeModule())
    .build()

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
