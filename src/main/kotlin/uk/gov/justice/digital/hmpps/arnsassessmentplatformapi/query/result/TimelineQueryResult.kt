package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import org.springframework.data.domain.Page
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

data class TimelineQueryResult(
  val timeline: List<TimelineItem>,
  override val pageInfo: PageInfo,
) : QueryResult,
  PageableQueryResult {
  companion object {
    fun from(page: Page<TimelineEntity>) = TimelineQueryResult(
      timeline = page.map(TimelineItem::from).toList(),
      pageInfo = PageInfo(
        pageNumber = page.number,
        totalPages = page.totalPages,
      ),
    )
  }
}
