package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommandService(
  private val assessmentService: AssessmentService,
  private val oasysService: OasysService,
  private val commandExecutorHelper: CommandExecutorHelper,
) {
  fun process(request: CommandExecutorRequest): CommandExecutorResult {
    val services = listOf(assessmentService, oasysService)

    val results = services.map { it.execute(request) }

    // Enforce: all non-null assessment UUIDs must match
    val uuids = results.mapNotNull { it.getAssessmentUuid() }.distinct()

    require(uuids.size <= 1) {
      "Multiple assessment UUIDs returned by services: $uuids"
    }

    val merged = results.fold(CommandExecutorResult()) { result, otherResult ->
      if (otherResult.isOk()) result.mergeWith(otherResult) else result
    }

    val mergedAssessmentUuid = merged.getAssessmentUuid()
    if (merged.events.isNotEmpty() && mergedAssessmentUuid != null) {
      commandExecutorHelper.handleSave(mergedAssessmentUuid, merged.events)
      logger.info(
        "Executed {} commands for assessment {}",
        merged.events.size,
        mergedAssessmentUuid,
      )
    }

    return merged
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
