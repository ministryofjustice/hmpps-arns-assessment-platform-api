package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.TimelineRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.PageInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult
import java.time.LocalDateTime
import java.util.*

@Service
class TimelineService(
  private val timelineRepository: TimelineRepository,
) {
  fun findAllBetweenByAssessmentUuid(assessmentUuid: UUID, from: LocalDateTime, to: LocalDateTime) = timelineRepository.findAllByAssessmentUuidAndCreatedAtBetween(assessmentUuid, from, to).map(TimelineItem::from)

  fun findAllPageableByAssessmentUuid(assessmentUuid: UUID, count: Int, page: Int): Page<TimelineItem> = timelineRepository.findAllByAssessmentUuid(
    assessmentUuid,
    PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "created_at")),
  ).map(TimelineItem::from)

  fun findAllBetweenByUserUuid(userUuid: UUID, from: LocalDateTime, to: LocalDateTime) = timelineRepository.findAllByUserUuidAndCreatedAtBetween(userUuid, from, to).map(TimelineItem::from)

  fun findAllPageableByUserUuid(userUuid: UUID, count: Int, page: Int): Page<TimelineItem> = timelineRepository.findAllByUserUuid(
    userUuid,
    PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "created_at")),
  ).map(TimelineItem::from)

  fun save(entity: TimelineEntity): TimelineEntity = timelineRepository.save(entity)
  fun saveAll(entities: List<TimelineEntity>): List<TimelineEntity> = timelineRepository.saveAll(entities)

  private fun List<TimelineEntity>.toTimeLineItems() = TimelineQueryResult(
    timeline = this.map(TimelineItem::from),
  )

  private fun Page<TimelineEntity>.toTimeLineItemsWithPageInfo() = TimelineQueryResult(
    timeline = this.map(TimelineItem::from).toList(),
    pageInfo = PageInfo(
      pageNumber = this.number,
      totalPages = this.totalPages,
    ),
  )
}
