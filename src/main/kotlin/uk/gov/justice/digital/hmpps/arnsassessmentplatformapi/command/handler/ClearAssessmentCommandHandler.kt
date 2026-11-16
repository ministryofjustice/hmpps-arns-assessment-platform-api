package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ClearAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentClearedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

class ClearAssessmentCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<ClearAssessmentCommand> {
  override val type = ClearAssessmentCommand::class

  override fun handle(command: ClearAssessmentCommand): CommandSuccessCommandResult {
    val assessment = services.persistenceContext.findAssessment(command.assessmentUuid.value)
    val state =
      services.persistenceContext.getAggregateState(assessment, AssessmentAggregate::class, null) as AssessmentState

    val data = state.getForRead().data

    val patterns = command.properties.map(::toRegex)
    val properties = data.properties.keys.filter { key -> patterns.any { it.matches(key) } }
    val collections = data.collections.map { it.name }.filter { it in command.collections }.distinct()

    val event = EventEntity(
      user = services.persistenceContext.findUserDetails(command.user),
      assessment = assessment,
      data = AssessmentClearedEvent(properties, collections),
      createdAt = services.clock.requestDateTime(),
      autosaved = command.autosaved,
    )
    services.eventBus.handle(event).createTimeline(command.timeline)
    return CommandSuccessCommandResult()
  }

  private fun toRegex(pattern: String) = Regex(pattern.split("*").joinToString(".*") { Regex.escape(it) })
}
