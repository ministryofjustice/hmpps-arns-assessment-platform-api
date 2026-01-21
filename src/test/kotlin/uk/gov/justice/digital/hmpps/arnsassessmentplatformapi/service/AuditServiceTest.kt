package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AuditableEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditServiceTest {

  private val mockQueueService: HmppsQueueService = mockk()
  private val mockObjectMapper: ObjectMapper = mockk()
  private val mockSqsClient: SqsAsyncClient = mockk(relaxed = true)
  private val serviceName = "arns-assessment-platform-api"

  private lateinit var service: AuditService

  private val queueUrl = "http://sqs/audit-queue"

  @BeforeEach
  fun setup() {
    val hmppsQueue = HmppsQueue("audit", mockSqsClient, "audit", mockk(relaxed = true))
    every { mockQueueService.findByQueueId("audit") } returns hmppsQueue

    service = AuditService(
      hmppsQueueService = mockQueueService,
      objectMapper = mockObjectMapper,
      serviceName = serviceName,
    )
  }

  @ParameterizedTest
  @MethodSource("provideAuditable")
  fun `it should send an audit event`(auditable: Any) {
    val capturedEvent = slot<AuditableEvent>()
    every {
      mockObjectMapper.writeValueAsString(capture(capturedEvent))
    } returns "serialized event"

    val capturedEventDetails = slot<Map<*, *>>()
    every {
      mockObjectMapper.writeValueAsString(capture(capturedEventDetails))
    } returns "serialized event details"

    val capturedConsumer = slot<Consumer<SendMessageRequest.Builder>>()
    every {
      mockSqsClient.sendMessage(capture(capturedConsumer))
    } returns CompletableFuture.completedFuture(SendMessageResponse.builder().build())

    every {
      mockSqsClient.getQueueUrl(any<GetQueueUrlRequest>())
    } returns CompletableFuture.completedFuture(
      GetQueueUrlResponse.builder().queueUrl(queueUrl).build(),
    )

    when (auditable) {
      is RequestableCommand -> service.audit(auditable)
      is RequestableQuery -> service.audit(auditable)
      else -> fail("Unexpected auditable type $auditable")
    }

    val builder = SendMessageRequest.builder()
    capturedConsumer.captured.accept(builder)
    val request = builder.build()

    verify(exactly = 1) {
      mockSqsClient.sendMessage(capture(capturedConsumer))
    }
    verify(exactly = 1) {
      mockObjectMapper.writeValueAsString(capture(capturedEventDetails))
    }
    verify(exactly = 1) {
      mockObjectMapper.writeValueAsString(capture(capturedEvent))
    }

    val user = when (auditable) {
      is RequestableCommand -> auditable.user
      is RequestableQuery -> auditable.user
      else -> fail("Unexpected auditable type $auditable")
    }

    assertEquals(queueUrl, request.queueUrl())
    assertTrue(request.messageBody().contains("serialized event"))
    assertEquals("serialized event details", capturedEvent.captured.details)
    assertEquals(serviceName, capturedEvent.captured.service)
    assertEquals(user.userId, capturedEvent.captured.who)
    assertEquals(auditable::class.simpleName, capturedEvent.captured.what)

    when (auditable) {
      is RequestableCommand -> assertEquals(mapOf("assessmentUuid" to assessmentUuid), capturedEventDetails.captured)
      is RequestableQuery -> assertEquals(mapOf("assessmentIdentifier" to UuidIdentifier(assessmentUuid)), capturedEventDetails.captured)
      else -> fail("Unexpected auditable type $auditable")
    }
  }

  @Test
  fun `should throw if audit queue does not exist`() {
    every { mockQueueService.findByQueueId("audit") } returns null

    assertThrows<RuntimeException> {
      AuditService(mockQueueService, mockObjectMapper, serviceName).audit(mockk<RequestableCommand>())
    }
  }

  companion object {
    private val assessmentUuid = UUID.randomUUID()
    private val user = UserDetailsEntity("TEST_USER")

    @JvmStatic
    fun provideAuditable(): Stream<Any> = Stream.of(
      UpdateAssessmentPropertiesCommand(
        user = user,
        assessmentUuid = assessmentUuid,
        added = mapOf("STATUS" to SingleValue("TEST_STATUS")),
        removed = emptyList(),
        timeline = null,
      ),
      AssessmentTimelineQuery(user, UuidIdentifier(assessmentUuid), Clock.now()), // RequestableQuery
    )
  }
}
