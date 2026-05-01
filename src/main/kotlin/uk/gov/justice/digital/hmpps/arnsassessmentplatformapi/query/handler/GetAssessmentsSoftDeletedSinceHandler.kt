package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidQueryException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.GetAssessmentsSoftDeletedSinceQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.GetAssessmentsSoftDeletedSinceQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.QueryResult
import java.util.UUID

@Component
class GetAssessmentsSoftDeletedSinceHandler(
  private val services: QueryHandlerServiceBundle,
  @param:Value($$"${app.query.max-lookback-days:1}")
  private val maxLookbackDays: Long,
) : QueryHandler<GetAssessmentsSoftDeletedSinceQuery> {
  override val type = GetAssessmentsSoftDeletedSinceQuery::class

  private fun validateMaxLookback(query: GetAssessmentsSoftDeletedSinceQuery) {
    if (maxLookbackDays > 0 && query.since.isBefore(services.clock.now().minusDays(maxLookbackDays))) {
      throw InvalidQueryException(
        "The 'since' parameter cannot be older than $maxLookbackDays day(s)",
      )
    }
  }

  override fun handle(query: GetAssessmentsSoftDeletedSinceQuery): QueryResult {
    validateMaxLookback(query)

    val assessments = services.event.findAssessmentsSoftDeletedSince(
      assessmentType = query.assessmentType,
      since = query.since,
    )

    val results = assessments.map(::buildResult)

    return GetAssessmentsSoftDeletedSinceQueryResult(results)
  }

  private fun buildResult(assessment: AssessmentEntity): UUID {
    return assessment.uuid
  }
}