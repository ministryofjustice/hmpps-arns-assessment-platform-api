package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidQueryException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.GetAssessmentsModifiedSinceQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.GetAssessmentsModifiedSinceQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.PageInfo

@Component
class GetAssessmentsModifiedSinceQueryHandler(
  private val services: QueryHandlerServiceBundle,
  private val eventRepository: EventRepository,
  @param:Value($$"${app.query.max-lookback-days:1}")
  private val maxLookbackDays: Long,
) : QueryHandler<GetAssessmentsModifiedSinceQuery> {
  override val type = GetAssessmentsModifiedSinceQuery::class

  override fun handle(query: GetAssessmentsModifiedSinceQuery): GetAssessmentsModifiedSinceQueryResult {
    validateMaxLookback(query)

    val page = eventRepository.findAssessmentsModifiedSince(
      query.assessmentType,
      query.since,
      PageRequest.of(query.pageNumber, query.pageSize),
    )

    val assessments = page.content.map { assessment -> buildResult(assessment) }

    return GetAssessmentsModifiedSinceQueryResult(
      assessments = assessments,
      pageInfo = PageInfo(
        pageNumber = page.number,
        totalPages = page.totalPages,
      ),
    )
  }

  private fun validateMaxLookback(query: GetAssessmentsModifiedSinceQuery) {
    if (maxLookbackDays > 0 && query.since.isBefore(services.clock.now().minusDays(maxLookbackDays))) {
      throw InvalidQueryException(
        "The 'since' parameter cannot be older than $maxLookbackDays day(s)",
      )
    }
  }

  private fun buildResult(assessment: AssessmentEntity): AssessmentVersionQueryResult {
    val state = services.state.stateForType(AssessmentAggregate::class)
      .fetchOrCreateState(assessment, null) as AssessmentState

    val aggregate = state.getForRead()
    val data = aggregate.data

    val collaborators = services.userDetails.findUsersByUuids(data.collaborators)
      .map(User::from)
      .toSet()
    val assignedUser = data.assignedUser?.let { userUuid ->
      collaborators.find { collaborator -> userUuid == collaborator.id }
        ?: services.userDetails.findByUserUuid(userUuid).run(User::from)
    }

    return AssessmentVersionQueryResult(
      assessmentUuid = assessment.uuid,
      aggregateUuid = aggregate.uuid,
      assessmentType = assessment.type,
      formVersion = data.formVersion,
      createdAt = aggregate.eventsFrom,
      updatedAt = aggregate.eventsTo,
      answers = data.answers,
      properties = data.properties,
      collections = data.collections,
      collaborators = collaborators,
      identifiers = assessment.identifiersMap(),
      assignedUser = assignedUser,
      flags = data.flags,
    )
  }
}
