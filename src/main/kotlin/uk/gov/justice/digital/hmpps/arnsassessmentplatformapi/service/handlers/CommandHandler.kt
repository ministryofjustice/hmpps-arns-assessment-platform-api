package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers.result.CommandResult
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface CommandHandler<C : Command> {
  val type: KClass<C>
  fun handle(command: C): CommandResult
  fun execute(command: Command) = handle(type.cast(command))
}
