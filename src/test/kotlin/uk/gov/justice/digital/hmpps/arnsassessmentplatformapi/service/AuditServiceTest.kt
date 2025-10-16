package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AuditableEvent
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
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

  @Test
  fun `should send audit event for a command`() {
    val command = CreateAssessment(User("TEST_USER"))

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

    service.audit(command)

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

    assertEquals(queueUrl, request.queueUrl())
    assertTrue(request.messageBody().contains("serialized event"))
    assertEquals(mapOf("assessmentUuid" to command.assessmentUuid), capturedEventDetails.captured)
    assertEquals("serialized event details", capturedEvent.captured.details)
    assertEquals(serviceName, capturedEvent.captured.service)
    assertEquals(command.user.id, capturedEvent.captured.who)
    assertEquals(command::class.simpleName, capturedEvent.captured.what)
  }

  @Test
  fun `should throw if audit queue does not exist`() {
    every { mockQueueService.findByQueueId("audit") } returns null

    assertThrows<RuntimeException> {
      AuditService(mockQueueService, mockObjectMapper, serviceName).audit(mockk())
    }
  }
}
