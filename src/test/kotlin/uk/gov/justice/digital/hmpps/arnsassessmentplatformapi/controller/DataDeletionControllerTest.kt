package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandBusFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.DataDeletionOperation
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.DataDeletionRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.EventUpdate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.TimelineUpdate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.DataDeletionDataResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssignedToUserEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.TimelineRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult
import java.util.UUID

class DataDeletionControllerTest(
  @Autowired
  private val eventRepository: EventRepository,
  @Autowired
  private val timelineRepository: TimelineRepository,
  @Autowired
  private val commandBusFactory: CommandBusFactory,
) : IntegrationTestBase() {
  val commandBus = commandBusFactory.create()

  @Nested
  inner class GetData {
    @Test
    fun `it returns events and timeline data for an assessment`() {
      val assessment = CreateAssessmentCommand(
        UserDetails("test-user-1", "Test User"),
        assessmentType = "TEST",
        formVersion = "1",
      )
      val assessmentUuid = assessment.assessmentUuid.value

      val httpRequest = MockHttpServletRequest()
      RequestContextHolder.setRequestAttributes(ServletRequestAttributes(httpRequest))

      try {
        commandBus.dispatchAndPersist(listOf(assessment))
      } finally {
        RequestContextHolder.resetRequestAttributes()
      }

      val response = webTestClient.get().uri("/data-deletion/$assessmentUuid")
        .headers(setAuthorisation(roles = listOf("ROLE_AAP_DATA_DELETION")))
        .exchange()
        .expectStatus().isOk
        .expectBody(DataDeletionDataResponse::class.java)
        .returnResult()
        .responseBody

      val persistedEvents = eventRepository.findAllIncludingDeleted(assessmentUuid)
      val persistedTimeline = timelineRepository.findAllIncludingDeleted(assessmentUuid)

      assertThat(response?.events?.size).isEqualTo(persistedEvents.size)
      assertThat(response?.timeline?.size).isEqualTo(persistedTimeline.size)
    }

    @Test
    fun `it denies access with no roles`() {
      webTestClient.get().uri("/data-deletion/${UUID.randomUUID()}")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  inner class UpdateData {
    @Test
    fun `dry-run does not persist data`() {
      val assessment = CreateAssessmentCommand(
        UserDetails("test-user-1", "Test User"),
        assessmentType = "TEST",
        formVersion = "1",
      )
      val assessmentUuid = assessment.assessmentUuid.value

      val httpRequest = MockHttpServletRequest()
      RequestContextHolder.setRequestAttributes(ServletRequestAttributes(httpRequest))

      try {
        commandBus.dispatchAndPersist(listOf(assessment))
      } finally {
        RequestContextHolder.resetRequestAttributes()
      }

      val persistedEvents = eventRepository.findAllIncludingDeleted(assessmentUuid)
      val persistedTimeline = timelineRepository.findAllIncludingDeleted(assessmentUuid)

      val assessmentCreatedEvent = persistedEvents.first { it.position == 0 }
      val assessmentAssignedEvent = persistedEvents.first { it.position == 1 }

      assertInstanceOf<AssessmentCreatedEvent>(assessmentCreatedEvent.data)
      assertInstanceOf<AssignedToUserEvent>(assessmentAssignedEvent.data)

      val request = DataDeletionRequest(
        dryRun = true,
        events = listOf(
          EventUpdate(
            uuid = assessmentCreatedEvent.uuid,
            operation = DataDeletionOperation.UPDATE,
            event = AssessmentCreatedEvent(
              formVersion = "2",
              properties = (assessmentCreatedEvent.data as AssessmentCreatedEvent).properties,
              flags = (assessmentCreatedEvent.data as AssessmentCreatedEvent).flags,
            ),
          ),
          EventUpdate(
            uuid = assessmentAssignedEvent.uuid,
            operation = DataDeletionOperation.DELETE,
            event = assessmentAssignedEvent.data,
          ),
        ),
        timeline = listOf(
          TimelineUpdate(
            uuid = persistedTimeline.first().uuid,
            operation = DataDeletionOperation.DELETE,
            timeline = TimelineItem.from(persistedTimeline.first()),
          ),
        ),
      )

      webTestClient.post().uri("/data-deletion/$assessmentUuid")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AAP_DATA_DELETION")))
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk

      val assessmentVersion = query(
        AssessmentVersionQuery(
          user = UserDetails("test-user-1", "Test User"),
          assessmentIdentifier = UuidIdentifier(assessmentUuid),
        ),
      )
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result as AssessmentVersionQueryResult

      assertThat(assessmentVersion.formVersion).isEqualTo("1")

      val timelineResponse = query(
        TimelineQuery(
          user = UserDetails("test-user-1", "Test User"),
          assessmentIdentifier = UuidIdentifier(assessmentUuid),
        ),
      )
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result as TimelineQueryResult

      assertNotNull(timelineResponse.timeline.find { it.uuid == persistedTimeline.first().uuid })
    }

    @Test
    fun `data is persisted when dry-run is false`() {
      val assessment = CreateAssessmentCommand(
        UserDetails("test-user-1", "Test User"),
        assessmentType = "TEST",
        formVersion = "1",
      )
      val assessmentUuid = assessment.assessmentUuid.value

      val httpRequest = MockHttpServletRequest()
      RequestContextHolder.setRequestAttributes(ServletRequestAttributes(httpRequest))

      try {
        commandBus.dispatchAndPersist(listOf(assessment))
      } finally {
        RequestContextHolder.resetRequestAttributes()
      }

      val persistedEvents = eventRepository.findAllIncludingDeleted(assessmentUuid)
      val persistedTimeline = timelineRepository.findAllIncludingDeleted(assessmentUuid)

      val assessmentCreatedEvent = persistedEvents.first { it.position == 0 }
      val assessmentAssignedEvent = persistedEvents.first { it.position == 1 }

      assertInstanceOf<AssessmentCreatedEvent>(assessmentCreatedEvent.data)
      assertInstanceOf<AssignedToUserEvent>(assessmentAssignedEvent.data)

      val request = DataDeletionRequest(
        dryRun = false,
        events = listOf(
          EventUpdate(
            uuid = assessmentCreatedEvent.uuid,
            operation = DataDeletionOperation.UPDATE,
            event = AssessmentCreatedEvent(
              formVersion = "2",
              properties = (assessmentCreatedEvent.data as AssessmentCreatedEvent).properties,
              flags = (assessmentCreatedEvent.data as AssessmentCreatedEvent).flags,
            ),
          ),
          EventUpdate(
            uuid = assessmentAssignedEvent.uuid,
            operation = DataDeletionOperation.DELETE,
            event = assessmentAssignedEvent.data,
          ),
        ),
        timeline = listOf(
          TimelineUpdate(
            uuid = persistedTimeline.first().uuid,
            operation = DataDeletionOperation.DELETE,
            timeline = TimelineItem.from(persistedTimeline.first()),
          ),
        ),
      )

      webTestClient.post().uri("/data-deletion/$assessmentUuid")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AAP_DATA_DELETION")))
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk

      val assessmentVersion = query(
        AssessmentVersionQuery(
          user = UserDetails("test-user-1", "Test User"),
          assessmentIdentifier = UuidIdentifier(assessmentUuid),
        ),
      )
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result as AssessmentVersionQueryResult

      assertThat(assessmentVersion.formVersion).isEqualTo("2")

      val timelineResponse = query(
        TimelineQuery(
          user = UserDetails("test-user-1", "Test User"),
          assessmentIdentifier = UuidIdentifier(assessmentUuid),
        ),
      )
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result as TimelineQueryResult

      assertNull(timelineResponse.timeline.find { it.uuid == persistedTimeline.first().uuid })
    }

    @Test
    fun `it denies access with no roles`() {
      val request = DataDeletionRequest(
        dryRun = false,
        events = listOf(),
        timeline = listOf(),
      )

      webTestClient.post().uri("/data-deletion/${UUID.randomUUID()}")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf()))
        .bodyValue(request)
        .exchange()
        .expectStatus().isForbidden
    }
  }
}
