package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse

@Service
class CommandBus(
  private val registry: CommandHandlerRegistry,
) {
  private fun handle(command: Command) = registry.getHandlerFor(command::class).execute(command)

  @Transactional
  fun dispatch(command: Command) = dispatch(listOf(command))

  @Transactional
  fun dispatch(commands: List<Command>) = CommandsResponse(commands.map { CommandResponse(it, handle(it)) })
}
