package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AuditableEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Query
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.SubjectAccessRequestQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimelineQuery
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.jvm.java

@Service
class AuditService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  @param:Value("\${spring.application.name}")
  private val serviceName: String,
) {
  private val auditQueue by lazy {
    hmppsQueueService.findByQueueId("audit") ?: throw kotlin.RuntimeException("Queue with ID 'audit' does not exist'")
  }
  private val sqsClient by lazy { auditQueue.sqsClient }
  private val queueUrl by lazy { auditQueue.queueUrl }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private fun json(details: Any) = objectMapper.writeValueAsString(details)

  private fun sendEvent(events: List<AuditableEvent>) {
    val batches = events.mapIndexed { index, event ->
      SendMessageBatchRequestEntry.builder()
        .id("msg-$index")
        .messageBody(json(event))
        .build()
    }.chunked(10)

    batches.forEachIndexed { index, entries ->
      log.info("Sending batch of ${events.size} audit events (${index + 1}/${batches.size})")

      sqsClient.sendMessageBatch {
        it.queueUrl(queueUrl)
          .entries(entries)
          .build()
      }.whenComplete { _, error ->
        if (error != null) {
          log.error("Error during sending audit messages", error)
        } else {
          log.info("Successfully sent ${events.size} audit events")
        }
      }
    }


  }

  fun audit(commands: List<RequestableCommand>) = commands.map { command -> AuditableEvent(
    who = command.user.id,
    what = command::class.simpleName ?: "Unknown",
    service = serviceName,
    details = json(mapOf("assessmentUuid" to command.assessmentUuid.value)),
  ) }.also { events -> sendEvent(events) }

  fun audit(query: Query) = when (query) {
    is RequestableQuery -> AuditableEvent(
      who = query.user.id,
      what = query::class.simpleName ?: "Unknown",
      service = serviceName,
      details = json(
        when (query) {
          is AssessmentQuery -> mapOf("assessmentIdentifier" to query.assessmentIdentifier)
          is TimelineQuery -> mapOf(
            "assessmentIdentifier" to query.assessmentIdentifier,
            "subject" to query.subject?.id,
          ).filter { it.value != null }
        },
      ),
    ).also {sendEvent(listOf(it))}

    is SubjectAccessRequestQuery -> AuditableEvent(
      who = "SAR",
      what = query::class.simpleName ?: "Unknown",
      `when` = LocalDateTime.now().intoAuditableTimestamp(),
      service = serviceName,
      details = json(
        mapOf(
          "subject" to query.assessmentIdentifiers,
          "from" to query.from,
          "to" to query.to,
          "timestamp" to query.timestamp?.intoAuditableTimestamp(),
        ),
      ),
    ).also {sendEvent(listOf(it))}
    else -> {
      log.warn("${query::class.simpleName} has not been implemented in the audit service")
    }
  }

  fun LocalDateTime.intoAuditableTimestamp(): Instant = atZone(ZoneOffset.systemDefault()).toInstant()
}
