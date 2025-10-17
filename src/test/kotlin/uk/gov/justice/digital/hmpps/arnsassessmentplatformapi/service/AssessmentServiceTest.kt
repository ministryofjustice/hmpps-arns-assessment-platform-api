package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException
import java.util.UUID

class AssessmentServiceTest {
  val assessmentRepository: AssessmentRepository = mockk()
  val service = AssessmentService(
    assessmentRepository = assessmentRepository,
  )

  @Nested
  inner class FindByUuid {
    @Test
    fun `it finds and returns the assessment`() {
      val assessment = AssessmentEntity()

      every { assessmentRepository.findByUuid(assessment.uuid) } returns assessment

      val result = service.findByUuid(assessment.uuid)

      assertThat(result).isEqualTo(assessment)
    }

    @Test
    fun `it throws when unable to find the assessment`() {
      every { assessmentRepository.findByUuid(any<UUID>()) } returns null

      assertThrows<AssessmentNotFoundException> {
        service.findByUuid(UUID.randomUUID())
      }
    }
  }
}
