package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AggregateResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AssessmentVersionResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate

@Component
class AssessmentVersionMapper : AggregateResponseMapper {
  override val aggregateType = AssessmentVersionAggregate.aggregateType

  override fun intoResponse(aggregate: Aggregate): AggregateResponse = (aggregate as AssessmentVersionAggregate)
    .run {
      AssessmentVersionResponse(
        formVersion = getFormVersion(),
        answers = getAnswers(),
        collaborators = getCollaborators(),
      )
    }
}
