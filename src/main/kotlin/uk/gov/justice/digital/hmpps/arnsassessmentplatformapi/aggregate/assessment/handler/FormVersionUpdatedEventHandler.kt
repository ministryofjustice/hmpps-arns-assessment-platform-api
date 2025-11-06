package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

@Component
class FormVersionUpdatedEventHandler(
  private val clock: Clock,
): EventHandler<FormVersionUpdatedEvent, AssessmentState> {
  override val eventType = FormVersionUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<FormVersionUpdatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.get()

    aggregate.data.timeline.add(
      TimelineItem(
        details = "Form version updated to ${event.data.version}",
        timestamp = event.createdAt,
      )
    )

    aggregate.data.formVersion = event.data.version

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }
}
