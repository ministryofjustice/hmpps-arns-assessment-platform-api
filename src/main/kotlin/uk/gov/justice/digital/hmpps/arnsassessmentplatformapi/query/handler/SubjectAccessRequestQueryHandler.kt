package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.SubjectAccessRequestQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.exception.SubjectAccessRequestNoAssessmentsException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestAssessmentVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.SubjectAccessRequestQueryResult
import java.time.LocalDateTime
import java.time.LocalTime

@Component
class SubjectAccessRequestQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<SubjectAccessRequestQuery> {
  override val type = SubjectAccessRequestQuery::class
  override fun handle(query: SubjectAccessRequestQuery): SubjectAccessRequestQueryResult {
    val assessments = services.assessment.findAllByExternalIdentifiers(query.assessmentIdentifiers, query.from, query.to)

    if (assessments.isEmpty()) throw SubjectAccessRequestNoAssessmentsException(query.assessmentIdentifiers)

    val results = assessments.map { assessment ->
      val aggregate = services.state.stateForType(AssessmentAggregate::class)
        .fetchLatestStateBefore(assessment, query.to?.atTime(LocalTime.MAX) ?: query.timestamp ?: LocalDateTime.now())
        .let { it as AssessmentState }
        .getForRead()

      SubjectAccessRequestAssessmentVersion.from(aggregate, assessment)
    }

    return SubjectAccessRequestQueryResult(results)
  }
}
