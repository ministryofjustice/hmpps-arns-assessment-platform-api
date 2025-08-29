package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest

@Service
class CommandService(
  private val assessmentService: AssessmentService,
  private val oasysService: OasysService,
  private val commandExecutorHelper: CommandExecutorHelper,
) {
  fun process(request: CommandRequest) {
    listOf(assessmentService, oasysService)
      .map { commandExecutor -> commandExecutor.execute(request) }
      .flatten()
      .apply(commandExecutorHelper::handleSave)
      .also { logger.info("Executed ${it.size} commands for assessment ${request.assessmentUuid}") }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
