package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class AssessmentVersionQueryHandler(
  private val assessmentService: AssessmentService,
  private val stateService: StateService,
) : QueryHandler<AssessmentVersionQuery> {
  override val type = AssessmentVersionQuery::class
  override fun handle(query: AssessmentVersionQuery): AssessmentVersionQueryResult {
    val assessment = assessmentService.findByUuid(query.assessmentUuid)

    val state = stateService.ForType(AssessmentAggregate::class)
      .fetchState(assessment, query.timestamp) as AssessmentState

    val data = state.get().data

    return AssessmentVersionQueryResult(
      formVersion = data.formVersion,
      answers = data.answers,
      properties = data.properties,
      collections = data.collections,
      collaborators = data.collaborators,
    )
  }
}
