package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface EventHandler<C : Event> {
  val type: KClass<C>
  fun handle(event: EventEntity, data: C, state: AssessmentState): AssessmentState
  fun execute(event: EventEntity, state: AssessmentState) = handle(event, type.cast(event.data), state)
}
