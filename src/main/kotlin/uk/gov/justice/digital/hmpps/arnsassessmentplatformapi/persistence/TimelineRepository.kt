package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface TimelineRepository : JpaRepository<TimelineEntity, Long> {
  fun findAllByAssessmentUuid(uuid: UUID): List<TimelineEntity>

  fun findAllByAssessmentUuid(uuid: UUID, pageable: Pageable): Page<TimelineEntity>
  fun findAllByUserUuid(uuid: UUID, pageable: Pageable): Page<TimelineEntity>

  fun findAllByAssessmentUuidAndCreatedAtBetween(uuid: UUID, from: LocalDateTime, to: LocalDateTime): List<TimelineEntity>
  fun findAllByUserUuidAndCreatedAtBetween(uuid: UUID, from: LocalDateTime, to: LocalDateTime): List<TimelineEntity>
}
