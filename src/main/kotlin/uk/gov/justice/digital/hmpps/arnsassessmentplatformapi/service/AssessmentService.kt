package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import java.util.UUID

class AssessmentNotFoundException(
  assessmentUuid: UUID,
) : AssessmentPlatformException(
  message = "Assessment not found",
  developerMessage = "No assessment found with UUID: $assessmentUuid",
  statusCode = HttpStatus.NOT_FOUND,
)

@Service
class AssessmentService(
  private val assessmentRepository: AssessmentRepository,
) {
  fun findByUuid(assessmentUuid: UUID) = assessmentRepository.findByUuid(assessmentUuid)
    ?: throw AssessmentNotFoundException(assessmentUuid)
}
