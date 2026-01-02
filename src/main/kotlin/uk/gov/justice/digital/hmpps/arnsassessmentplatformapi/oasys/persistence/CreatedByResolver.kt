package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

object CreatedByResolver {
  fun from(event: EventEntity<*>) = when (val eventData = event.data) {
    is AssessmentCreatedEvent -> CreatedBy.CREATED
    is AssessmentPropertiesUpdatedEvent ->
      if (eventData.added["STATUS"] is SingleValue && (eventData.added["STATUS"] as SingleValue).value == "CLONED") {
        CreatedBy.CLONED
      } else {
        CreatedBy.DAILY_EDIT
      }

    else -> CreatedBy.DAILY_EDIT
  }
}
