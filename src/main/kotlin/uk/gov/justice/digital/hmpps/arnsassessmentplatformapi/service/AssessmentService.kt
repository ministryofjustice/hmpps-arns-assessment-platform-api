package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentIdentifierRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.cache.AssessmentCache
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.criteria.AssessmentsByExternalIdentifiersCriteria
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class AssessmentService(
  private val assessmentRepository: AssessmentRepository,
  private val assessmentIdentifierRepository: AssessmentIdentifierRepository,
  private val assessmentCache: AssessmentCache,
) {
  fun findBy(uuid: UUID) = assessmentCache.get(uuid)
    ?: assessmentCache.put(findBy(UuidIdentifier(uuid), LocalDateTime.now()))

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

  fun delete(assessment: AssessmentEntity) = assessmentRepository.delete(assessment)
}
