package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import com.fasterxml.jackson.databind.node.JsonNodeFactory as HibernateJsonNodeFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.JsonNodeFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.AssessmentDraftRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.SavedDraft
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentDraftEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentDraftRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.DraftRevisionConflictException
import java.time.LocalDateTime
import java.util.UUID

class AssessmentDraftServiceTest {
  private val assessmentService: AssessmentService = mockk()
  private val userDetailsService: UserDetailsService = mockk()
  private val assessmentDraftRepository: AssessmentDraftRepository = mockk()
  private val clock: Clock = mockk()
  private val service = AssessmentDraftService(assessmentService, userDetailsService, assessmentDraftRepository, clock)

  private val now = LocalDateTime.parse("2026-09-18T10:00:00")
  private val assessmentUuid = UUID.randomUUID()
  private val owner = UserDetailsEntity(userId = "user", displayName = "User", authSource = testUser.authSource)
  private val baseAggregateUuid = UUID.randomUUID()
  private val request = AssessmentDraftRequest(
    user = testUser,
    draftKey = "goal:${UUID.randomUUID()}",
    formVersion = "1.0",
    baseAggregateUuid = baseAggregateUuid,
    baseAggregateVersion = 2,
    revision = 1,
    data = JsonNodeFactory.instance.objectNode().put("goal_title", "Find accommodation"),
    expiresAt = now.plusDays(7),
  )

  @Test
  fun `saves a new draft for the assessment owner`() {
    val savedDraft = slot<AssessmentDraftEntity>()
    every { assessmentService.findBy(assessmentUuid) } returns AssessmentEntity(type = "TEST", createdAt = now)
    every { userDetailsService.findOrCreate(testUser) } returns owner
    every { userDetailsService.saveAll(listOf(owner)) } returns listOf(owner)
    every {
      assessmentDraftRepository.findByAssessmentUuidAndOwnerUuidAndFormVersionAndDraftKey(
        assessmentUuid,
        owner.uuid,
        request.formVersion,
        request.draftKey,
      )
    } returns null
    every { assessmentDraftRepository.save(capture(savedDraft)) } answers { savedDraft.captured }
    every { clock.now() } returns now

    val result = service.save(assessmentUuid, request)

    assertThat(result.assessmentUuid).isEqualTo(assessmentUuid)
    assertThat(result.revision).isEqualTo(request.revision)
    assertThat(result.data).isEqualTo(request.data)
    assertThat(savedDraft.captured.baseAggregateUuid).isEqualTo(baseAggregateUuid)
    assertThat(savedDraft.captured.data.toString()).isEqualTo(request.data.toString())
    assertThat(savedDraft.captured.expiresAt).isEqualTo(request.expiresAt)
  }

  @Test
  fun `rejects a stale draft revision`() {
    val existing = draft(revision = request.revision)
    every { assessmentService.findBy(assessmentUuid) } returns AssessmentEntity(type = "TEST", createdAt = now)
    every { userDetailsService.findOrCreate(testUser) } returns owner
    every { userDetailsService.saveAll(listOf(owner)) } returns listOf(owner)
    every {
      assessmentDraftRepository.findByAssessmentUuidAndOwnerUuidAndFormVersionAndDraftKey(
        assessmentUuid,
        owner.uuid,
        request.formVersion,
        request.draftKey,
      )
    } returns existing
    every { clock.now() } returns now

    assertThrows<DraftRevisionConflictException> { service.save(assessmentUuid, request) }

    verify(exactly = 0) { assessmentDraftRepository.save(any()) }
  }

  @Test
  fun `returns only unexpired drafts belonging to the requested user`() {
    val draft = draft()
    every { assessmentService.findBy(assessmentUuid) } returns AssessmentEntity(type = "TEST", createdAt = now)
    every { userDetailsService.find(testUser) } returns owner
    every { clock.now() } returns now
    every {
      assessmentDraftRepository.findAllByAssessmentUuidAndOwnerUuidAndExpiresAtAfterOrderByUpdatedAtDesc(
        assessmentUuid,
        owner.uuid,
        now,
      )
    } returns listOf(draft)

    val results = service.findAll(assessmentUuid, testUser)

    assertThat(results).hasSize(1)
    assertThat(results.single().draftKey).isEqualTo(request.draftKey)
  }

  @Test
  fun `deletes only the acknowledged revision for its owner`() {
    val savedDraft = SavedDraft(assessmentUuid, testUser, request.draftKey, request.formVersion, request.revision)
    every { userDetailsService.find(testUser) } returns owner
    every {
      assessmentDraftRepository.deleteByAssessmentUuidAndOwnerUuidAndFormVersionAndDraftKeyAndRevisionLessThanEqual(
        assessmentUuid,
        owner.uuid,
        request.formVersion,
        request.draftKey,
        request.revision,
      )
    } returns 1

    service.deleteAcknowledged(savedDraft)

    verify(exactly = 1) {
      assessmentDraftRepository.deleteByAssessmentUuidAndOwnerUuidAndFormVersionAndDraftKeyAndRevisionLessThanEqual(
        assessmentUuid,
        owner.uuid,
        request.formVersion,
        request.draftKey,
        request.revision,
      )
    }
  }

  @Test
  fun `deletes expired drafts`() {
    every { clock.systemNow() } returns now
    every { assessmentDraftRepository.deleteByExpiresAtBefore(now) } returns 2

    service.deleteExpired()

    verify(exactly = 1) { assessmentDraftRepository.deleteByExpiresAtBefore(now) }
  }

  private fun draft(revision: Long = request.revision) = AssessmentDraftEntity(
    assessmentUuid = assessmentUuid,
    owner = owner,
    draftKey = request.draftKey,
    formVersion = request.formVersion,
    baseAggregateUuid = request.baseAggregateUuid,
    baseAggregateVersion = request.baseAggregateVersion,
    revision = revision,
    data = HibernateJsonNodeFactory.instance.objectNode().put("goal_title", "Find accommodation"),
    createdAt = now,
    updatedAt = now,
    expiresAt = request.expiresAt,
  )

  private companion object {
    private val testUser = UserDetails("user", "User")
  }
}
