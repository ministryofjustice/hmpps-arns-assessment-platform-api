package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class AssessmentService(
  private val assessmentRepository: AssessmentRepository,
  private val aggregateRepository: AggregateRepository,
  private val eventService: EventService,
  private val eventBus: EventBus,
  private val clock: Clock,
) {
  private fun now(): LocalDateTime = LocalDateTime.now(clock)

  fun findByUuid(assessmentUuid: UUID) = assessmentRepository.findByUuid(assessmentUuid)
    ?: throw AssessmentNotFoundException(assessmentUuid)
}
