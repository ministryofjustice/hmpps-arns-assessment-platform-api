package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandResult
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface CommandHandler<C : Command> {
  val type: KClass<C>
  fun handle(command: C): CommandResult
  fun execute(command: Command) = handle(type.cast(command))
}
