package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import java.util.UUID

class AssessmentNotFoundException(
  message: String,
) : EntityNotFoundException(message)

@Service
class AssessmentService(
  private val assessmentRepository: AssessmentRepository,
) {
  fun findByUuid(uuid: UUID) = assessmentRepository.findByUuid(uuid)
    ?: throw AssessmentNotFoundException("No assessment found for UUID: $uuid")
}
