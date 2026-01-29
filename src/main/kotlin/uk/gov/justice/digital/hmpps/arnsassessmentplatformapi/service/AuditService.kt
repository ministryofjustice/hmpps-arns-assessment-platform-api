package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AuditableEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UserTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import kotlin.also
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
    }
      .get()
      .also { log.info("Audit event ${event.what} for ${event.who} sent") }
  }

  fun audit(command: RequestableCommand) = AuditableEvent(
    who = command.user.id,
    what = command::class.simpleName ?: "Unknown",
    service = serviceName,
    details = json(mapOf("assessmentUuid" to command.assessmentUuid)),
  ).run(::sendEvent)

  fun audit(query: RequestableQuery) = AuditableEvent(
    who = query.user.id,
    what = query::class.simpleName ?: "Unknown",
    service = serviceName,
    details = json(
      when (query) {
        is AssessmentQuery -> mapOf("assessmentIdentifier" to query.assessmentIdentifier)
        is AssessmentTimelineQuery -> when (query.identifier) {
          is ExternalIdentifier -> mapOf(
            "assessmentIdentifier" to query.identifier.identifier,
            "assessmentIdentifierType" to query.identifier.identifierType,
          )

          is UuidIdentifier -> mapOf(
            "assessmentUuid" to query.identifier.uuid,
          )
        }

        is UserTimelineQuery -> mapOf("userIdentifier" to query.user.id)
      },
    ),
  ).run(::sendEvent)
}
