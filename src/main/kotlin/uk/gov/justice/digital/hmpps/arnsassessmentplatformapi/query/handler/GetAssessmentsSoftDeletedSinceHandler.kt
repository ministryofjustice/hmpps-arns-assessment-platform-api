package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidQueryException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.GetAssessmentsSoftDeletedSinceQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.GetAssessmentsSoftDeletedSinceQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.QueryResult
import java.time.LocalDateTime
import java.util.UUID

@Component
class GetAssessmentsSoftDeletedSinceHandler(
  private val services: QueryHandlerServiceBundle,
  private val eventRepository: EventRepository,
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

    val assessments = eventRepository.findAssessmentsSoftDeletedSince(
      assessmentType = query.assessmentType,
      since = query.since,
    )

    val results = assessments.map(::buildResult)

    return GetAssessmentsSoftDeletedSinceQueryResult(results)
  }

  private fun buildResult(assessment: AssessmentEntity): AssessmentVersionQueryResult {
    return AssessmentVersionQueryResult(
      assessmentUuid = assessment.uuid,
      aggregateUuid = UUID.randomUUID(),
      assessmentType = assessment.type,
      formVersion = "",
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
      answers = emptyMap(),
      properties = emptyMap(),
      collections = emptyList(),
      collaborators = emptySet(),
      identifiers = emptyMap(),
      assignedUser = null,
      flags = emptyList(),
    )
  }
}