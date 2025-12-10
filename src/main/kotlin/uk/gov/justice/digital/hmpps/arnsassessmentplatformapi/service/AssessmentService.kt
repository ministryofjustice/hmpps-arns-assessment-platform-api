package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentIdentifierRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException

@Service
class AssessmentService(
  private val assessmentRepository: AssessmentRepository,
  private val assessmentIdentifierRepository: AssessmentIdentifierRepository,
) {
  fun findBy(assessmentIdentifier: AssessmentIdentifier) = when (assessmentIdentifier) {
    is ExternalIdentifier -> with(assessmentIdentifier) {
      assessmentIdentifierRepository.findByIdentifierTypeAndIdentifierAndAssessmentAssessmentType(
        identifierType,
        identifier,
        assessmentType,
      )?.assessment
    }

    is UuidIdentifier -> with(assessmentIdentifier) {
      assessmentRepository.findByUuid(uuid)
    }
  } ?: throw AssessmentNotFoundException(assessmentIdentifier)
}
