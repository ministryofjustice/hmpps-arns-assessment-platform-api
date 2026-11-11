package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.hook.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregateView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFlagsCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.hook.PrepopulateFromOasys
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.DataMappingService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentFlagsUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.MultiValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.time.LocalDateTime
import java.util.UUID

class PrepopulateFromOasysHandlerTest {
  private val now: LocalDateTime = LocalDateTime.now()

  private val dataMappingService: DataMappingService = mockk()
  private val objectMapper = ObjectMapper()
  private val handler = PrepopulateFromOasysHandler(dataMappingService, objectMapper)

  private val persistenceContext: PersistenceContext = mockk()
  private val eventBus: EventBus = mockk()
  private val clock: Clock = mockk()
  private val services = CommandHandlerServiceBundle(persistenceContext, eventBus, clock)

  private val assessment = AssessmentEntity(type = "TEST", createdAt = now)
  private val commandUser = UserDetails("FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  private val userEntity = UserDetailsEntity(1, UUID.randomUUID(), "FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)

  private val aggregateData = AssessmentAggregate().apply { formVersion = "2.0" }
  private val aggregateEntity = AggregateEntity(
    updatedAt = now,
    eventsFrom = now,
    eventsTo = now,
    assessment = assessment,
    data = aggregateData,
  )
  private val aggregateState = AssessmentState(aggregateEntity)

  private val oasysData = mapOf("accommodation_type" to "SETTLED", "score" to 3)
  private val hook = PrepopulateFromOasys(data = oasysData)
  private val serialisedOasysData = SingleValue(objectMapper.writeValueAsString(oasysData))

  private val command: RequestableCommand = UpdateFlagsCommand(
    user = commandUser,
    assessmentUuid = assessment.uuid.toReference(),
    flags = listOf("SAN_BETA"),
  )

  private val handledEvents = slot<List<EventEntity<*>>>()

  @BeforeEach
  fun setUp() {
    every { persistenceContext.findAssessment(assessment.uuid) } returns assessment
    every { persistenceContext.getAggregateState(assessment, AssessmentAggregate::class, null) } returns aggregateState
    every { persistenceContext.findUserDetails(commandUser) } returns userEntity
    every { clock.requestDateTime() } returns now
    every { eventBus.handle(capture(handledEvents)) } just runs
  }

  private fun handledEventData() = handledEvents.captured.map { it.data }

  private fun collection(name: String, vararg items: CollectionItem) = Collection(
    uuid = UUID.randomUUID(),
    createdAt = now,
    updatedAt = now,
    name = name,
    items = items.toMutableList(),
  )

  private fun item(
    answers: Map<String, SingleValue> = emptyMap(),
    properties: Map<String, SingleValue> = emptyMap(),
    collections: List<Collection> = emptyList(),
  ) = CollectionItem(
    uuid = UUID.randomUUID(),
    createdAt = now,
    updatedAt = now,
    answers = answers.toMutableMap(),
    properties = properties.toMutableMap(),
    collections = collections.toMutableList(),
  )

  @Test
  fun `it stores the type of the hook it is built to handle`() {
    assertThat(handler.type).isEqualTo(PrepopulateFromOasys::class)
  }

  @Test
  fun `it maps the hook's OASys data to its SAN equivalent using the assessment's current state`() {
    val aggregateView = slot<AssessmentAggregateView>()
    every { dataMappingService.getSanEquivalent(capture(aggregateView), oasysData) } returns AssessmentAggregate()

    handler.handle(hook, command, services)

    assertThat(aggregateView.captured).isEqualTo(aggregateData)
    verify(exactly = 1) { dataMappingService.getSanEquivalent(any(), oasysData) }
  }

  @Test
  fun `it only raises a properties event containing the raw OASys data when the SAN equivalent is empty`() {
    every { dataMappingService.getSanEquivalent(any(), any()) } returns AssessmentAggregate()

    handler.handle(hook, command, services)

    assertThat(handledEventData()).containsExactly(
      AssessmentPropertiesUpdatedEvent(
        added = mapOf("oasys_prepopulation_data" to serialisedOasysData),
        removed = emptyList(),
      ),
    )
  }

  @Test
  fun `it merges the mapped SAN properties into the properties event alongside the raw OASys data`() {
    every { dataMappingService.getSanEquivalent(any(), any()) } returns AssessmentAggregate().apply {
      properties["foo_property"] = SingleValue("foo")
      properties["bar_property"] = MultiValue(listOf("bar", "baz"))
    }

    handler.handle(hook, command, services)

    assertThat(handledEventData()).containsExactly(
      AssessmentPropertiesUpdatedEvent(
        added = mapOf(
          "oasys_prepopulation_data" to serialisedOasysData,
          "foo_property" to SingleValue("foo"),
          "bar_property" to MultiValue(listOf("bar", "baz")),
        ),
        removed = emptyList(),
      ),
    )
  }

  @Test
  fun `it raises an answers updated event when the SAN equivalent has answers`() {
    every { dataMappingService.getSanEquivalent(any(), any()) } returns AssessmentAggregate().apply {
      answers["current_accommodation"] = SingleValue("SETTLED")
      answers["living_with"] = MultiValue(listOf("PARTNER", "CHILD"))
    }

    handler.handle(hook, command, services)

    assertThat(handledEventData()).containsExactly(
      AssessmentPropertiesUpdatedEvent(mapOf("oasys_prepopulation_data" to serialisedOasysData), emptyList()),
      AssessmentAnswersUpdatedEvent(
        added = mapOf(
          "current_accommodation" to SingleValue("SETTLED"),
          "living_with" to MultiValue(listOf("PARTNER", "CHILD")),
        ),
        removed = emptyList(),
      ),
    )
  }

  @Test
  fun `it raises a flags updated event when the SAN equivalent has flags`() {
    every { dataMappingService.getSanEquivalent(any(), any()) } returns AssessmentAggregate().apply {
      flags.addAll(listOf("FLAG_ONE", "FLAG_TWO"))
    }

    handler.handle(hook, command, services)

    assertThat(handledEventData()).containsExactly(
      AssessmentPropertiesUpdatedEvent(mapOf("oasys_prepopulation_data" to serialisedOasysData), emptyList()),
      AssessmentFlagsUpdatedEvent(listOf("FLAG_ONE", "FLAG_TWO")),
    )
  }

  @Test
  fun `it raises collection created and item added events for top level collections, preserving item order`() {
    val firstItem = item(answers = mapOf("a" to SingleValue("1")), properties = mapOf("p" to SingleValue("x")))
    val secondItem = item(answers = mapOf("a" to SingleValue("2")))
    val collection = collection("GOALS", firstItem, secondItem)
    val emptyCollection = collection("EMPTY")

    every { dataMappingService.getSanEquivalent(any(), any()) } returns AssessmentAggregate().apply {
      collections.add(collection)
      collections.add(emptyCollection)
    }

    handler.handle(hook, command, services)

    assertThat(handledEventData()).containsExactly(
      AssessmentPropertiesUpdatedEvent(mapOf("oasys_prepopulation_data" to serialisedOasysData), emptyList()),
      CollectionCreatedEvent(collection.uuid, "GOALS", null),
      CollectionItemAddedEvent(firstItem.uuid, collection.uuid, firstItem.answers, firstItem.properties, 0),
      CollectionItemAddedEvent(secondItem.uuid, collection.uuid, secondItem.answers, secondItem.properties, 1),
      CollectionCreatedEvent(emptyCollection.uuid, "EMPTY", null),
    )
  }

  @Test
  fun `it recursively raises events for nested collections, parented to the item that contains them`() {
    val grandchildItem = item(answers = mapOf("depth" to SingleValue("3")))
    val grandchildCollection = collection("GRANDCHILD", grandchildItem)
    val childItem = item(answers = mapOf("depth" to SingleValue("2")), collections = listOf(grandchildCollection))
    val childCollection = collection("CHILD", childItem)
    val firstParentItem = item(answers = mapOf("depth" to SingleValue("1")), collections = listOf(childCollection))
    val secondParentItem = item(answers = mapOf("depth" to SingleValue("1b")))
    val parentCollection = collection("PARENT", firstParentItem, secondParentItem)

    every { dataMappingService.getSanEquivalent(any(), any()) } returns AssessmentAggregate().apply {
      collections.add(parentCollection)
    }

    handler.handle(hook, command, services)

    assertThat(handledEventData()).containsExactly(
      AssessmentPropertiesUpdatedEvent(mapOf("oasys_prepopulation_data" to serialisedOasysData), emptyList()),
      CollectionCreatedEvent(parentCollection.uuid, "PARENT", null),
      CollectionItemAddedEvent(firstParentItem.uuid, parentCollection.uuid, firstParentItem.answers, firstParentItem.properties, 0),
      CollectionCreatedEvent(childCollection.uuid, "CHILD", firstParentItem.uuid),
      CollectionItemAddedEvent(childItem.uuid, childCollection.uuid, childItem.answers, childItem.properties, 0),
      CollectionCreatedEvent(grandchildCollection.uuid, "GRANDCHILD", childItem.uuid),
      CollectionItemAddedEvent(grandchildItem.uuid, grandchildCollection.uuid, grandchildItem.answers, grandchildItem.properties, 0),
      CollectionItemAddedEvent(secondParentItem.uuid, parentCollection.uuid, secondParentItem.answers, secondParentItem.properties, 1),
    )
  }

  @Test
  fun `it raises events in order of properties, answers, flags then collections`() {
    val collection = collection("GOALS")
    every { dataMappingService.getSanEquivalent(any(), any()) } returns AssessmentAggregate().apply {
      properties["prop"] = SingleValue("p")
      answers["answer"] = SingleValue("a")
      flags.add("FLAG")
      collections.add(collection)
    }

    handler.handle(hook, command, services)

    assertThat(handledEventData()).containsExactly(
      AssessmentPropertiesUpdatedEvent(
        mapOf("oasys_prepopulation_data" to serialisedOasysData, "prop" to SingleValue("p")),
        emptyList(),
      ),
      AssessmentAnswersUpdatedEvent(mapOf("answer" to SingleValue("a")), emptyList()),
      AssessmentFlagsUpdatedEvent(listOf("FLAG")),
      CollectionCreatedEvent(collection.uuid, "GOALS", null),
    )
  }

  @Test
  fun `it raises all events in a single batch against the resolved assessment and user, timestamped with the request time`() {
    every { dataMappingService.getSanEquivalent(any(), any()) } returns AssessmentAggregate().apply {
      answers["answer"] = SingleValue("a")
      flags.add("FLAG")
    }

    handler.handle(hook, command, services)

    verify(exactly = 1) { eventBus.handle(any<List<EventEntity<*>>>()) }
    verify(exactly = 0) { eventBus.handle(any<EventEntity<*>>()) }

    assertThat(handledEvents.captured).hasSize(3).allSatisfy { event ->
      assertThat(event.assessment).isEqualTo(assessment)
      assertThat(event.user).isEqualTo(userEntity)
      assertThat(event.createdAt).isEqualTo(now)
      assertThat(event.autosaved).isFalse()
    }
  }
}
