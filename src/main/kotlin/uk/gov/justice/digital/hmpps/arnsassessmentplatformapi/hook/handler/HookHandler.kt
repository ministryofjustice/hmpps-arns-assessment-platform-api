package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface HookHandler<H : Hook> {
  val type: KClass<H>
  fun handle(hook: H, command: RequestableCommand, services: CommandHandlerServiceBundle)
  fun execute(hook: Hook, command: RequestableCommand, services: CommandHandlerServiceBundle) = handle(type.cast(hook), command, services)
}
