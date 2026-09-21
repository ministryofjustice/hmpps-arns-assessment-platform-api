package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentDraftEntity
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface AssessmentDraftRepository : JpaRepository<AssessmentDraftEntity, Long> {
  fun findByAssessmentUuidAndOwnerUuidAndFormVersionAndDraftKey(
    assessmentUuid: UUID,
    ownerUuid: UUID,
    formVersion: String,
    draftKey: String,
  ): AssessmentDraftEntity?

  fun findAllByAssessmentUuidAndOwnerUuidAndExpiresAtAfterOrderByUpdatedAtDesc(
    assessmentUuid: UUID,
    ownerUuid: UUID,
    now: LocalDateTime,
  ): List<AssessmentDraftEntity>

  fun deleteByAssessmentUuidAndOwnerUuidAndFormVersionAndDraftKeyAndRevisionLessThanEqual(
    assessmentUuid: UUID,
    ownerUuid: UUID,
    formVersion: String,
    draftKey: String,
    revision: Long,
  ): Long

  fun deleteByExpiresAtBefore(now: LocalDateTime): Long
}
