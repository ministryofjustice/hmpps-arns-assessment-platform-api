package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException
import java.util.UUID

@Service
class AssessmentService(
  private val assessmentRepository: AssessmentRepository,
) {
  fun findByUuid(assessmentUuid: UUID) = assessmentRepository.findByUuid(assessmentUuid)
    ?: throw AssessmentNotFoundException(assessmentUuid)
}
