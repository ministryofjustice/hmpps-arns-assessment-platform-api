package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import com.fasterxml.jackson.databind.json.JsonMapper as HibernateJsonMapper
import org.springframework.stereotype.Service
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper as ApiJsonMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.AssessmentDraftRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.SavedDraft
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.AssessmentDraftResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentDraftEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentDraftRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.DraftRevisionConflictException
import java.util.UUID

@Service
class AssessmentDraftService(
  private val assessmentService: AssessmentService,
  private val userDetailsService: UserDetailsService,
  private val assessmentDraftRepository: AssessmentDraftRepository,
  private val clock: Clock,
) {
  @Transactional
  fun save(assessmentUuid: UUID, request: AssessmentDraftRequest): AssessmentDraftResponse {
    assessmentService.findBy(assessmentUuid)
    val owner = userDetailsService.findOrCreate(request.user).let { userDetailsService.saveAll(listOf(it)).single() }
    val now = clock.now()
    val draft = assessmentDraftRepository.findByAssessmentUuidAndOwnerUuidAndFormVersionAndDraftKey(
      assessmentUuid,
      owner.uuid,
      request.formVersion,
      request.draftKey,
    )

    if (draft != null && request.revision <= draft.revision) throw DraftRevisionConflictException()

    val saved = (draft ?: AssessmentDraftEntity(
      assessmentUuid = assessmentUuid,
      owner = owner,
      draftKey = request.draftKey,
      formVersion = request.formVersion,
      baseAggregateUuid = request.baseAggregateUuid,
      baseAggregateVersion = request.baseAggregateVersion,
      revision = request.revision,
      data = entityJsonMapper.readTree(request.data.toString()),
      createdAt = now,
      updatedAt = now,
      expiresAt = request.expiresAt,
    )).apply {
      baseAggregateUuid = request.baseAggregateUuid
      baseAggregateVersion = request.baseAggregateVersion
      revision = request.revision
      data = entityJsonMapper.readTree(request.data.toString())
      updatedAt = now
      expiresAt = request.expiresAt
    }.let(assessmentDraftRepository::save)

    return toResponse(saved)
  }

  @Transactional(readOnly = true)
  fun findAll(assessmentUuid: UUID, user: UserDetails): List<AssessmentDraftResponse> {
    assessmentService.findBy(assessmentUuid)
    val owner = userDetailsService.find(user)
    return assessmentDraftRepository
      .findAllByAssessmentUuidAndOwnerUuidAndExpiresAtAfterOrderByUpdatedAtDesc(assessmentUuid, owner.uuid, clock.now())
      .map(::toResponse)
  }

  @Transactional
  fun deleteAcknowledged(savedDraft: SavedDraft) {
    val owner = userDetailsService.find(savedDraft.user)
    assessmentDraftRepository.deleteByAssessmentUuidAndOwnerUuidAndFormVersionAndDraftKeyAndRevisionLessThanEqual(
      savedDraft.assessmentUuid,
      owner.uuid,
      savedDraft.formVersion,
      savedDraft.draftKey,
      savedDraft.revision,
    )
  }

  @Scheduled(cron = "\${app.draft.cleanup-cron:0 0 * * * *}")
  @Transactional
  fun deleteExpired() {
    assessmentDraftRepository.deleteByExpiresAtBefore(clock.systemNow())
  }

  private fun toResponse(draft: AssessmentDraftEntity) = AssessmentDraftResponse(
    assessmentUuid = draft.assessmentUuid,
    draftKey = draft.draftKey,
    formVersion = draft.formVersion,
    baseAggregateUuid = draft.baseAggregateUuid,
    baseAggregateVersion = draft.baseAggregateVersion,
    revision = draft.revision,
    data = apiJsonMapper.readTree(draft.data.toString()),
    updatedAt = draft.updatedAt,
    expiresAt = draft.expiresAt,
  )

  private companion object {
    val entityJsonMapper = HibernateJsonMapper.builder().build()
    val apiJsonMapper = ApiJsonMapper.builder().build()
  }
}
