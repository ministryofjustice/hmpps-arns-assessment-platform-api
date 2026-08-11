package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.config

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.filter.AnnotationTypeFilter
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.HookType

private const val SCAN_BASE_PACKAGE = "uk.gov.justice.digital.hmpps.arnsassessmentplatformapi"

// Spring Boot auto-registers any Module bean with the auto-configured ObjectMapper, so this avoids depending on
// the (now deprecated-for-removal) Jackson2ObjectMapperBuilderCustomizer.
@Configuration
class HookJacksonConfiguration {
  @Bean
  fun hookSubtypeModule(): Module = object : SimpleModule("HookSubtypes") {
    override fun setupModule(context: Module.SetupContext) {
      super.setupModule(context)
      context.registerSubtypes(*discoverHookSubtypes())
    }
  }

  private fun discoverHookSubtypes(): Array<NamedType> {
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AnnotationTypeFilter(HookType::class.java))

    return scanner.findCandidateComponents(SCAN_BASE_PACKAGE)
      .map { Class.forName(it.beanClassName) }
      .filter { Hook::class.java.isAssignableFrom(it) }
      .map { NamedType(it, it.getAnnotation(HookType::class.java).name) }
      .toTypedArray()
  }
}
