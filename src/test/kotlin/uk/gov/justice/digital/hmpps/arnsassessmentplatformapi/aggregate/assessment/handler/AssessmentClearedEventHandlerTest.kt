package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentClearedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

class AssessmentClearedEventHandlerTest : AbstractEventHandlerTest<AssessmentClearedEvent>() {
  override val handler = AssessmentClearedEventHandler::class
  override val eventType = AssessmentClearedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<AssessmentClearedEvent>("handles the event").apply {
      val victimCollectionUuid: UUID = UUID.randomUUID()
      val otherCollectionUuid: UUID = UUID.randomUUID()
      val otherItemUuid: UUID = UUID.randomUUID()
      val created = LocalDateTime.parse("2025-01-01T09:00:00")

      fun item(uuid: UUID) = CollectionItem(
        uuid = uuid,
        createdAt = created,
        updatedAt = created,
        answers = mutableMapOf(),
        properties = mutableMapOf(),
        collections = mutableListOf(),
      )

      event = eventEntityFor(
        AssessmentClearedEvent(
          properties = listOf("accommodation_section_complete"),
          collections = listOf("OFFENCE_ANALYSIS_VICTIM"),
        ),
      )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "properties" to event.data.properties,
          "collections" to event.data.collections,
        ),
        null,
      )

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = created,
            updatedAt = now,
            eventsTo = now,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              answers["some_answer"] = SingleValue("yes")
              properties["accommodation_section_complete"] = SingleValue("YES")
              properties["keep_me"] = SingleValue("still here")
              collections.add(
                Collection(
                  uuid = victimCollectionUuid,
                  createdAt = created,
                  updatedAt = created,
                  name = "OFFENCE_ANALYSIS_VICTIM",
                  items = mutableListOf(item(UUID.randomUUID()), item(UUID.randomUUID())),
                ),
              )
              collections.add(
                Collection(
                  uuid = otherCollectionUuid,
                  createdAt = created,
                  updatedAt = created,
                  name = "OTHER",
                  items = mutableListOf(item(otherItemUuid)),
                ),
              )
            },
          ),
        )
      }

      expectedState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = created,
            updatedAt = now,
            eventsTo = event.createdAt,
            numberOfEventsApplied = 1,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collaborators.add(user.uuid)
              properties["keep_me"] = SingleValue("still here")
              collections.add(
                Collection(
                  uuid = victimCollectionUuid,
                  createdAt = created,
                  updatedAt = created,
                  name = "OFFENCE_ANALYSIS_VICTIM",
                  items = mutableListOf(),
                ),
              )
              collections.add(
                Collection(
                  uuid = otherCollectionUuid,
                  createdAt = created,
                  updatedAt = created,
                  name = "OTHER",
                  items = mutableListOf(item(otherItemUuid)), // untouched
                ),
              )
            },
          ),
        )
      }
    },
    Scenario.Executes<AssessmentClearedEvent>("only clears answers when no properties or collections are listed").apply {
      val victimCollectionUuid: UUID = UUID.randomUUID()
      val otherCollectionUuid: UUID = UUID.randomUUID()
      val victimItem1Uuid: UUID = UUID.randomUUID()
      val victimItem2Uuid: UUID = UUID.randomUUID()
      val otherItemUuid: UUID = UUID.randomUUID()
      val created = LocalDateTime.parse("2025-01-01T09:00:00")

      fun item(uuid: UUID) = CollectionItem(
        uuid = uuid,
        createdAt = created,
        updatedAt = created,
        answers = mutableMapOf(),
        properties = mutableMapOf(),
        collections = mutableListOf(),
      )

      event = eventEntityFor(
        AssessmentClearedEvent(
          properties = emptyList(),
          collections = emptyList(),
        ),
      )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "properties" to event.data.properties,
          "collections" to event.data.collections,
        ),
        null,
      )

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = created,
            updatedAt = now,
            eventsTo = now,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              answers["some_answer"] = SingleValue("yes")
              properties["accommodation_section_complete"] = SingleValue("YES")
              properties["keep_me"] = SingleValue("still here")
              collections.add(
                Collection(
                  uuid = victimCollectionUuid,
                  createdAt = created,
                  updatedAt = created,
                  name = "OFFENCE_ANALYSIS_VICTIM",
                  items = mutableListOf(item(victimItem1Uuid), item(victimItem2Uuid)),
                ),
              )
              collections.add(
                Collection(
                  uuid = otherCollectionUuid,
                  createdAt = created,
                  updatedAt = created,
                  name = "OTHER",
                  items = mutableListOf(item(otherItemUuid)),
                ),
              )
            },
          ),
        )
      }

      expectedState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = created,
            updatedAt = now,
            eventsTo = event.createdAt,
            numberOfEventsApplied = 1,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collaborators.add(user.uuid)
              properties["accommodation_section_complete"] = SingleValue("YES")
              properties["keep_me"] = SingleValue("still here")
              collections.add(
                Collection(
                  uuid = victimCollectionUuid,
                  createdAt = created,
                  updatedAt = created,
                  name = "OFFENCE_ANALYSIS_VICTIM",
                  items = mutableListOf(item(victimItem1Uuid), item(victimItem2Uuid)),
                ),
              )
              collections.add(
                Collection(
                  uuid = otherCollectionUuid,
                  createdAt = created,
                  updatedAt = created,
                  name = "OTHER",
                  items = mutableListOf(item(otherItemUuid)), // untouched
                ),
              )
            },
          ),
        )
      }
    },
  )
}
