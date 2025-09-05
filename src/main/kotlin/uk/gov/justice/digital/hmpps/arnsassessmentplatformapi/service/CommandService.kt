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
    val services = listOf(assessmentService, oasysService)
    val events = buildList {
      for (service in services) addAll(service.execute(request))
    }

    if (events.isNotEmpty()) {
      commandExecutorHelper.handleSave(request.assessmentUuid, events)
    }

    logger.info("Executed {} commands for assessment {}", events.size, request.assessmentUuid)
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
