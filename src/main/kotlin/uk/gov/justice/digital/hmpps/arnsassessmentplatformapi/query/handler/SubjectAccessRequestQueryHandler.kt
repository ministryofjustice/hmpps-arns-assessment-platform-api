package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.SubjectAccessRequestQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestAssessmentVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestQueryResult
import java.time.LocalTime

@Component
class SubjectAccessRequestQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<SubjectAccessRequestQuery> {
  override val type = SubjectAccessRequestQuery::class
  override fun handle(query: SubjectAccessRequestQuery): SubjectAccessRequestQueryResult {
    val results = services.assessment.findAllByExternalIdentifiers(query.assessmentIdentifiers, query.to, query.from)
      .map { assessment ->
        val aggregate = services.state.stateForType(AssessmentAggregate::class)
          .fetchOrCreateState(assessment, query.to?.atTime(LocalTime.MAX) ?: query.timestamp)
          .let { it as AssessmentState }
          .getForRead()

        SubjectAccessRequestAssessmentVersion.from(aggregate, assessment)
      }

    return SubjectAccessRequestQueryResult(results)
  }
}
