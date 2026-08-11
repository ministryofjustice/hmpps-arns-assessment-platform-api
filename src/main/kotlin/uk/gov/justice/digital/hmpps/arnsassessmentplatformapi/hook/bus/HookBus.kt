package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook

@Component
class HookBus(
  private val registry: HookHandlerRegistry,
) {
  fun dispatch(hooks: List<Hook>, command: RequestableCommand, serviceBundle: CommandHandlerServiceBundle) {
    hooks.forEach { hook -> registry.getHandlersFor(hook::class).forEach { it.execute(hook, command, serviceBundle) } }
  }
}
