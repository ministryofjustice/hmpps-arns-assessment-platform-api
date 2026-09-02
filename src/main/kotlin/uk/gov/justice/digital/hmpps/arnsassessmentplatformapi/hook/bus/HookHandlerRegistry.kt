package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.handler.HookHandler
import kotlin.reflect.KClass

@Component
class HookHandlerRegistry(
  handlers: List<HookHandler<out Hook>>,
) {
  private val registry: Map<KClass<out Hook>, List<HookHandler<out Hook>>> = handlers.groupBy { it.type }

  fun getHandlersFor(hook: KClass<out Hook>): List<HookHandler<out Hook>> = registry[hook] ?: emptyList()
}
