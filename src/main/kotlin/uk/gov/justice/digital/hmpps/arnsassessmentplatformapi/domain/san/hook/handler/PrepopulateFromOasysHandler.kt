package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.hook.handler

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.hook.PrepopulateFromOasys
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.DataMappingService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentFlagsUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.handler.HookHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.util.UUID

@Component
class PrepopulateFromOasysHandler(
  val dataMappingService: DataMappingService,
  val objectMapper: ObjectMapper,
) : HookHandler<PrepopulateFromOasys> {
  override val type = PrepopulateFromOasys::class

  override fun handle(
    hook: PrepopulateFromOasys,
    command: RequestableCommand,
    services: CommandHandlerServiceBundle,
  ) {
    val assessment = services.persistenceContext.findAssessment(command.assessmentUuid.value)
    val aggregateState = services.persistenceContext.getAggregateState(assessment, AssessmentAggregate::class, null) as AssessmentState
    val sanEquivalent = dataMappingService.getSanEquivalent(aggregateState.getForRead().data, hook.data)

    val events = mutableListOf<Event>()

    AssessmentPropertiesUpdatedEvent(
      mapOf("oasys_prepopulation_data" to SingleValue(objectMapper.writeValueAsString(hook.data)))
        .plus(sanEquivalent.properties),
      listOf(),
    ).run(events::add)

    if (sanEquivalent.answers.isNotEmpty()) {
      AssessmentAnswersUpdatedEvent(sanEquivalent.answers, listOf()).run(events::add)
    }

    if (sanEquivalent.flags.isNotEmpty()) {
      AssessmentFlagsUpdatedEvent(sanEquivalent.flags).run(events::add)
    }

    fun createCollection(collection: Collection, parentCollectionItemUuid: UUID?) {
      with(collection) {
        CollectionCreatedEvent(uuid, name, parentCollectionItemUuid).run(events::add)
      }
      collection.items.forEachIndexed { index, item ->
        CollectionItemAddedEvent(
          item.uuid,
          collection.uuid,
          item.answers,
          item.properties,
          index,
        ).run(events::add)

        item.collections.forEach { createCollection(it, item.uuid) }
      }
    }

    sanEquivalent.collections.forEach { createCollection(it, null) }

    events.map { event ->
      with(command) {
        EventEntity(
          user = services.persistenceContext.findUserDetails(user),
          assessment = assessment,
          data = event,
          createdAt = services.clock.requestDateTime(),
          autosaved = autosaved,
        )
      }
    }.run(services.eventBus::handle)
  }
}
