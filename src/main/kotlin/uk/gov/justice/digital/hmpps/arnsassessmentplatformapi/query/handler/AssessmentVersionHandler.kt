package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class AssessmentVersionHandler(
  private val assessmentService: AssessmentService,
  private val aggregateService: AggregateService,
) : QueryHandler<AssessmentVersion> {
  override val type = AssessmentVersion::class
  override fun handle(query: AssessmentVersion): AssessmentVersionResult {
    val aggregate = assessmentService.findByUuid(query.assessmentUuid)
      .let { assessment -> aggregateService.fetchOrCreateAggregate(assessment, AssessmentVersionAggregate.aggregateType, query.timestamp) }
      .data as AssessmentVersionAggregate

    return AssessmentVersionResult(
      formVersion = aggregate.getFormVersion(),
      answers = aggregate.getAnswers(),
      collaborators = aggregate.getCollaborators(),
    )
  }
}
