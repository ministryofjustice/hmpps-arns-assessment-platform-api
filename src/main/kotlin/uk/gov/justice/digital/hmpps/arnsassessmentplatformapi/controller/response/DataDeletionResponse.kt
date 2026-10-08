package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception.EventHandlingException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.AggregateDTO
import kotlin.reflect.KClass

data class DataDeletionResponse(
  val success: Boolean,
  val dryRun: Boolean,
  val exception: EventHandlingException? = null,
  val rebuiltState: Map<KClass<out Aggregate<*>>, List<AggregateDTO>>? = null,
)
