package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.hook.handler

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.hook.UpdateOasysDataMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.DataMappingService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.handler.HookHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class UpdateOasysDataMappingHandler(
  val dataMappingService: DataMappingService,
  val objectMapper: ObjectMapper,
) : HookHandler<UpdateOasysDataMapping> {
  override val type = UpdateOasysDataMapping::class

  override fun handle(
    hook: UpdateOasysDataMapping,
    command: RequestableCommand,
    services: CommandHandlerServiceBundle,
  ) {
    val assessment = services.persistenceContext.findAssessment(command.assessmentUuid.value)
    val aggregateState = services.persistenceContext.getAggregateState(assessment, AssessmentAggregate::class, null) as AssessmentState
    val oasysEquivalent = dataMappingService.getOasysEquivalent(aggregateState.getForRead().data, hook.formConfig)

    val event = with(command) {
      EventEntity(
        user = services.persistenceContext.findUserDetails(user),
        assessment = assessment,
        data = AssessmentPropertiesUpdatedEvent(
          mapOf("oasys_equivalent" to SingleValue(objectMapper.writeValueAsString(oasysEquivalent))),
          listOf(),
        ),
        createdAt = services.clock.requestDateTime(),
      )
    }

    services.eventBus.handle(event)
  }
}
