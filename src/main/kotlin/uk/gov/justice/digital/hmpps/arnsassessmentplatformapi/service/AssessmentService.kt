package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.criteria.AssessmentsByExternalIdentifiersCriteria
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentIdentifierRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class AssessmentService(
  private val assessmentRepository: AssessmentRepository,
  private val assessmentIdentifierRepository: AssessmentIdentifierRepository,
  private val stateService: StateService,
  private val auditService: AuditService,
  private val authenticationHolder: HmppsAuthenticationHolder,
) {
  fun findBy(uuid: UUID) = findBy(UuidIdentifier(uuid), LocalDateTime.now())

  fun findBy(assessmentIdentifier: AssessmentIdentifier, pointInTime: LocalDateTime) = when (assessmentIdentifier) {
    is ExternalIdentifier -> with(assessmentIdentifier) {
      assessmentIdentifierRepository.findFirstByExternalIdentifierTypeAndExternalIdentifierIdAndAssessmentTypeAndCreatedAtBeforeOrderByCreatedAtDesc(
        identifierType,
        identifier,
        assessmentType,
        pointInTime,
      )?.assessment
    }

    is UuidIdentifier -> with(assessmentIdentifier) {
      assessmentRepository.findByUuid(uuid)
    }
  } ?: throw AssessmentNotFoundException(assessmentIdentifier)

  fun findAllByExternalIdentifiers(
    externalIdentifiers: Set<IdentifierPair>,
    from: LocalDate? = null,
    to: LocalDate? = null,
  ): Set<AssessmentEntity> = assessmentRepository.findAll(
    AssessmentsByExternalIdentifiersCriteria(externalIdentifiers, from, to).toSpecification(),
  ).toSet()

  fun save(assessment: AssessmentEntity): AssessmentEntity = assessmentRepository.save(assessment)

  fun saveAll(assessments: List<AssessmentEntity>): List<AssessmentEntity> = assessmentRepository.saveAll(assessments)

  fun delete(assessment: AssessmentEntity) = assessmentRepository.delete(assessment)

  @Transactional
  fun rebuildAggregates(assessment: AssessmentEntity) {
    stateService.delete(assessment.uuid)
    val rebuiltState = stateService.rebuildFromEvents(assessment, LocalDateTime.now())
    val aggregateCount = rebuiltState.values.fold(0) { acc, state -> acc + state.aggregates.size }
    stateService.persist(mutableMapOf(assessment.uuid to rebuiltState))
    auditService.audit(
      authenticationHolder.principal,
      "RebuiltAggregates",
      "Rebuilt all ($aggregateCount) aggregates for assessment ${assessment.uuid}",
    )
  }
}
