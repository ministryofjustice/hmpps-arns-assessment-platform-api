package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService
import kotlin.reflect.KClass
import kotlin.reflect.cast

@Component
data class CommandHandlerServiceBundle(
  val assessment: AssessmentService,
  val event: EventService,
  val state: StateService,
  val userDetails: UserDetailsService,
  val timeline: TimelineService,
  val eventBus: EventBus,
  @param:Lazy val commandBus: CommandBus,
  val clock: Clock,
)

interface CommandHandler<C : Command> {
  val type: KClass<C>
  fun handle(command: C): CommandResult
  fun execute(command: Command) = handle(type.cast(command))
}
