package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentIdentifierRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException
import java.time.LocalDateTime
import java.util.UUID

@Service
class AssessmentService(
  private val assessmentRepository: AssessmentRepository,
  private val assessmentIdentifierRepository: AssessmentIdentifierRepository,
) {
  fun findBy(uuid: UUID) = findBy(UuidIdentifier(uuid), LocalDateTime.now())

  fun findBy(assessmentIdentifier: AssessmentIdentifier, pointInTime: LocalDateTime) = when (assessmentIdentifier) {
    is ExternalIdentifier -> with(assessmentIdentifier) {
      assessmentIdentifierRepository.findFirstByIdentifierTypeAndIdentifierAndAssessmentTypeAndCreatedAtBeforeOrderByCreatedAtDesc(
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

  fun save(assessment: AssessmentEntity): AssessmentEntity = assessmentRepository.save(assessment)

  fun delete(assessment: AssessmentEntity) = assessmentRepository.delete(assessment)
}
