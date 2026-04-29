package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.DailyVersionDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.criteria.TimelineCriteria
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.TimelineRepository
import java.time.LocalDateTime
import java.util.UUID

@Service
class TimelineService(
  private val timelineRepository: TimelineRepository,
) {
  fun findAll(criteria: TimelineCriteria, pageable: Pageable): Page<TimelineEntity> = timelineRepository.findAll(criteria.getSpecification(), pageable)
  fun findDailyVersions(assessmentUuid: UUID) = timelineRepository.findDailyVersionsByAssessment(assessmentUuid)
    .map { DailyVersionDetails.from(it) }

  fun findAllIncludingDeleted(assessmentUuid: UUID) = timelineRepository.findAllIncludingDeleted(assessmentUuid)

  fun findByUuidsIncludingDeleted(timelineUuids: Set<UUID>): List<TimelineEntity> = timelineRepository.findByUuidsIncludingDeleted(timelineUuids)

  fun save(entity: TimelineEntity): TimelineEntity = timelineRepository.save(entity)

  fun saveAll(entities: List<TimelineEntity>): List<TimelineEntity> {
    entities.groupBy { it.assessment.uuid }
      .forEach { (assessmentUuid, timelines) ->
        val maxPosition = timelineRepository.findTopByAssessmentUuidOrderByPositionDesc(assessmentUuid)?.position ?: -1
        timelines.forEachIndexed { index, entity -> entity.position = maxPosition + 1 + index }
      }
    return timelineRepository.saveAll(entities)
  }

  fun softDelete(assessmentUuid: UUID, from: LocalDateTime) {
    timelineRepository.findByAssessmentUuidAndCreatedAtGreaterThanEqual(assessmentUuid, from).map {
      it.apply { deleted = true }
    }.run(timelineRepository::saveAll)
  }

  fun hardDelete(timelineEntities: List<TimelineEntity>) = timelineRepository.deleteAll(timelineEntities)
}
