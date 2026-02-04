package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.SubjectAccessRequestQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestAssessmentVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestQueryResult

@Component
class SubjectAccessRequestQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<SubjectAccessRequestQuery> {
  override val type = SubjectAccessRequestQuery::class
  override fun handle(query: SubjectAccessRequestQuery): SubjectAccessRequestQueryResult {
    val results = services.assessment.findAllByExternalIdentifier(query.assessmentIdentifiers)
      .map { assessment ->
        val aggregate = services.state.stateForType(AssessmentAggregate::class)
          .fetchOrCreateState(assessment, query.timestamp).let { it as AssessmentState }
          .getForRead()

        SubjectAccessRequestAssessmentVersion.from(aggregate, assessment)
      }

    return SubjectAccessRequestQueryResult(results)
  }
}
