package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext

data class CommandHandlerServiceBundle(
  val persistenceContext: PersistenceContext,
  val eventBus: EventBus,
  val clock: Clock,
)
