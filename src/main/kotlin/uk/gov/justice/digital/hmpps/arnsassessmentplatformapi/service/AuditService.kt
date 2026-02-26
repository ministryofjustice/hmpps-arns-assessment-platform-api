package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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

  private fun sendEvent(event: AuditableEvent) {
    log.info("Sending audit event ${event.what} for ${event.who}")
    sqsClient.sendMessage {
      it.queueUrl(queueUrl)
        .messageBody(json(event))
        .build()
    }.whenComplete { _, error ->
      if (error != null) {
        log.error("Failed to send audit event ${event.what} for ${event.who}", error)
      } else {
        log.info("Audit event ${event.what} for ${event.who} sent")
      }
    }
  }

  fun audit(command: RequestableCommand) = AuditableEvent(
    who = command.user.id,
    what = command::class.simpleName ?: "Unknown",
    service = serviceName,
    details = json(mapOf("assessmentUuid" to command.assessmentUuid.value)),
  ).run(::sendEvent)

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
    ).run(::sendEvent)

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
    ).run(::sendEvent)
    else -> {
      log.warn("${query::class.simpleName} has not been implemented in the audit service")
    }
  }

  fun LocalDateTime.intoAuditableTimestamp(): Instant = atZone(ZoneOffset.systemDefault()).toInstant()
}
