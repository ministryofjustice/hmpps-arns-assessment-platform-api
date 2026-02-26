package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentIdentifierRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException
import java.time.LocalDateTime
import java.util.UUID

class AssessmentServiceTest {
  val assessmentRepository: AssessmentRepository = mockk()
  val assessmentIdentifierRepository: AssessmentIdentifierRepository = mockk()
  val service = AssessmentService(
    assessmentRepository = assessmentRepository,
    assessmentIdentifierRepository = assessmentIdentifierRepository,
  )

  @Nested
  inner class FindByUuid {
    @Test
    fun `it finds and returns the assessment`() {
      val assessment = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.now())

      every { assessmentRepository.findByUuid(assessment.uuid) } returns assessment

      val result = service.findBy(assessment.uuid)

      assertThat(result).isEqualTo(assessment)
    }

    @Test
    fun `it throws when unable to find the assessment`() {
      every { assessmentRepository.findByUuid(any<UUID>()) } returns null

      assertThrows<AssessmentNotFoundException> {
        service.findBy(UUID.randomUUID())
      }
    }
  }

  @Nested
  inner class FindByUuidIdentifier {
    @Test
    fun `it finds and returns the assessment`() {
      val assessment = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.now())

      every { assessmentRepository.findByUuid(assessment.uuid) } returns assessment

      val result = service.findBy(UuidIdentifier(assessment.uuid), LocalDateTime.now())

      assertThat(result).isEqualTo(assessment)
    }

    @Test
    fun `it throws when unable to find the assessment`() {
      every { assessmentRepository.findByUuid(any<UUID>()) } returns null

      assertThrows<AssessmentNotFoundException> {
        service.findBy(UuidIdentifier(UUID.randomUUID()), LocalDateTime.now())
      }
    }
  }

  @Nested
  inner class FindByExternalIdentifier {
    val assessment = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.now())
    val identifier = AssessmentIdentifierEntity(
      externalIdentifier = IdentifierPair(IdentifierType.CRN, "CRN123"),
      assessment = assessment,
      createdAt = LocalDateTime.now(),
    )

    val externalIdentifier = ExternalIdentifier(
      identifierType = IdentifierType.CRN,
      identifier = "CRN123",
      assessmentType = "TEST",
    )

    val now = LocalDateTime.now()

    @Test
    fun `it finds and returns the assessment`() {
      every {
        assessmentIdentifierRepository.findFirstByExternalIdentifierTypeAndExternalIdentifierIdAndAssessmentTypeAndCreatedAtBeforeOrderByCreatedAtDesc(
          type = IdentifierType.CRN,
          identifier = "CRN123",
          assessmentType = "TEST",
          pointInTime = now,
        )
      } returns identifier

      val result = service.findBy(externalIdentifier, now)

      assertThat(result).isEqualTo(assessment)
    }

    @Test
    fun `it throws when unable to find the assessment`() {
      every {
        assessmentIdentifierRepository.findFirstByExternalIdentifierTypeAndExternalIdentifierIdAndAssessmentTypeAndCreatedAtBeforeOrderByCreatedAtDesc(
          type = IdentifierType.CRN,
          identifier = "CRN123",
          assessmentType = "TEST",
          pointInTime = now,
        )
      } returns null

      assertThrows<AssessmentNotFoundException> {
        service.findBy(externalIdentifier, now)
      }
    }
  }
}
