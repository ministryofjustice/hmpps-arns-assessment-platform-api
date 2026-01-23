package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.User

@Component
class AssessmentVersionQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<AssessmentVersionQuery> {
  override val type = AssessmentVersionQuery::class
  override fun handle(query: AssessmentVersionQuery): AssessmentVersionQueryResult {
    val assessment = services.assessmentService.findBy(query.assessmentIdentifier)

    val state = services.stateService.stateForType(AssessmentAggregate::class)
      .fetchOrCreateState(assessment, query.timestamp) as AssessmentState

    val aggregate = state.getForRead()
    val data = aggregate.data

    val collaborators = services.userDetailsService.findUsersByUuids(data.collaborators)
      .map(User::from)
      .toSet()
    val assignedUser = data.assignedUser?.let { userUuid ->
      collaborators.find { collaborator -> userUuid == collaborator.id }
        ?: services.userDetailsService.findByUserUuid(userUuid).run(User::from)
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
    )
  }
}
