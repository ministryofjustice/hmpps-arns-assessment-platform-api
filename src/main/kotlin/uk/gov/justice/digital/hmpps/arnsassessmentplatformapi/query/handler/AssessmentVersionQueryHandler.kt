package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class AssessmentVersionQueryHandler(
  private val assessmentService: AssessmentService,
) : QueryHandler<AssessmentVersionQuery> {
  override val type = AssessmentVersionQuery::class
  override fun handle(query: AssessmentVersionQuery): AssessmentVersionQueryResult {
    val aggregate = assessmentService.findByUuid(query.assessmentUuid)
      .let { assessment -> assessmentService.fetchState(assessment, query.timestamp) }
      .current().data

    return AssessmentVersionQueryResult(
      formVersion = aggregate.formVersion,
      answers = aggregate.answers,
      collections = aggregate.collections,
      collaborators = aggregate.collaborators,
    )
  }
}
