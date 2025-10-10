package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AssessmentVersionResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate

@Component
class AssessmentVersionMapper : AggregateResponseMapper<AssessmentVersionAggregate> {
  override val aggregateType = AssessmentVersionAggregate::class

  override fun createResponseFrom(aggregate: AssessmentVersionAggregate) = AssessmentVersionResponse(
    formVersion = aggregate.getFormVersion(),
    answers = aggregate.getAnswers(),
    collaborators = aggregate.getCollaborators(),
  )
}
