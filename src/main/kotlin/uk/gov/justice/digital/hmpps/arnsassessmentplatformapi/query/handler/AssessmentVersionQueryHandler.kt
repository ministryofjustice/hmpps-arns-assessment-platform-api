package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime

@Component
class AssessmentVersionQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<AssessmentVersionQuery> {
  override val type = AssessmentVersionQuery::class
  override fun handle(query: AssessmentVersionQuery): AssessmentVersionQueryResult {
    val assessment = services.assessment.findBy(query.assessmentIdentifier, LocalDateTime.now())

    val state = services.state.stateForType(AssessmentAggregate::class)
      .fetchOrCreateState(assessment, query.timestamp) as AssessmentState

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
