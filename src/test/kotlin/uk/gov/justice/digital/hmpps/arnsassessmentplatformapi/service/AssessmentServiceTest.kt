package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentIdentifierRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.LocalDateTime
import java.util.UUID

class AssessmentServiceTest {
  val assessmentRepository: AssessmentRepository = mockk()
  val assessmentIdentifierRepository: AssessmentIdentifierRepository = mockk()
  val stateService: StateService = mockk()
  val auditService: AuditService = mockk()
  val authenticationHolder: HmppsAuthenticationHolder = mockk()

  val service = AssessmentService(
    assessmentRepository = assessmentRepository,
    assessmentIdentifierRepository = assessmentIdentifierRepository,
    stateService = stateService,
    auditService = auditService,
    authenticationHolder = authenticationHolder,
  )

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  @Nested
  inner class FindByUuid {
    @Test
    fun `it finds and returns the assessment`() {
      val assessment = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.now())

      every { assessmentRepository.findByUuid(assessment.uuid) } returns assessment

      val result = service.findBy(assessment.uuid)

      assertThat(result).isEqualTo(assessment)

      verify(exactly = 1) { assessmentRepository.findByUuid(assessment.uuid) }
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

  @Nested
  inner class RebuildAggregates {
    val assessment = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.now())

    private fun stateWithAggregates(count: Int): State = mutableMapOf(
      AssessmentAggregate::class to mockk<AggregateState<AssessmentAggregate>> {
        every { aggregates } returns MutableList(count) { mockk<AggregateEntity<AssessmentAggregate>>() }
      },
    )

    @BeforeEach
    fun setUp() {
      every { stateService.delete(any()) } just Runs
      every { stateService.persist(any()) } just Runs
      every { authenticationHolder.principal } returns "REBUILDER"
      every { auditService.audit(any(), any(), any()) } just Runs
    }

    @Test
    fun `it deletes existing aggregates, rebuilds from events and persists the rebuilt state`() {
      val rebuiltState = stateWithAggregates(3)
      val persisted = slot<MutableMap<UUID, State>>()

      every { stateService.rebuildFromEvents(assessment, null) } returns rebuiltState
      every { stateService.persist(capture(persisted)) } just Runs

      service.rebuildAggregates(assessment)

      verifyOrder {
        stateService.delete(assessment.uuid)
        stateService.rebuildFromEvents(assessment, null)
        stateService.persist(any())
      }

      assertThat(persisted.captured).containsOnlyKeys(assessment.uuid)
      assertThat(persisted.captured[assessment.uuid]).isSameAs(rebuiltState)
    }

    @Test
    fun `it audits the rebuild with the principal and aggregate count`() {
      every { stateService.rebuildFromEvents(assessment, null) } returns stateWithAggregates(3)

      service.rebuildAggregates(assessment)

      verify(exactly = 1) {
        auditService.audit(
          "REBUILDER",
          "RebuiltAggregates",
          "Rebuilt all (3) aggregates for assessment ${assessment.uuid}",
        )
      }
    }

    @Test
    fun `it reports zero aggregates when the rebuilt state is empty`() {
      every { stateService.rebuildFromEvents(assessment, null) } returns mutableMapOf()

      service.rebuildAggregates(assessment)

      verify(exactly = 1) { stateService.persist(mutableMapOf(assessment.uuid to mutableMapOf())) }
      verify(exactly = 1) {
        auditService.audit(
          "REBUILDER",
          "RebuiltAggregates",
          "Rebuilt all (0) aggregates for assessment ${assessment.uuid}",
        )
      }
    }

    @Test
    fun `it does not persist or audit when rebuilding from events fails`() {
      every { stateService.rebuildFromEvents(assessment, null) } throws IllegalStateException("boom")

      assertThrows<IllegalStateException> {
        service.rebuildAggregates(assessment)
      }

      verify(exactly = 1) { stateService.delete(assessment.uuid) }
      verify(exactly = 0) { stateService.persist(any()) }
      verify(exactly = 0) { auditService.audit(any(), any(), any()) }
    }
  }
}
