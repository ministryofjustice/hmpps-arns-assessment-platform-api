package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.TimelineRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.criteria.TimelineCriteria
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Service
class TimelineService(
  private val timelineRepository: TimelineRepository,
) {
  fun findAll(criteria: TimelineCriteria, pageable: Pageable): Page<TimelineEntity> = timelineRepository.findAll(criteria.getSpecification(), pageable)

  fun save(entity: TimelineEntity): TimelineEntity = timelineRepository.save(entity)
  fun saveAll(entities: List<TimelineEntity>): List<TimelineEntity> = timelineRepository.saveAll(entities)
}
