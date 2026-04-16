package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateTimelineItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ReorderCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.SoftDeleteCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFormVersionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.exception.CommandHandlerNotImplementedException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.AddCollectionItemCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CreateAssessmentCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CreateCollectionCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CreateTimelineItemCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.RemoveCollectionItemCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.ReorderCollectionItemCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.RollbackCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.SoftDeleteCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.UpdateAssessmentAnswersCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.UpdateAssessmentPropertiesCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.UpdateCollectionItemAnswersCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.UpdateCollectionItemPropertiesCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.UpdateFormVersionCommandHandler

@Component
class CommandHandlerFactory {
  fun create(command: Command, serviceBundle: CommandHandlerServiceBundle) = when (command) {
    is AddCollectionItemCommand -> AddCollectionItemCommandHandler(serviceBundle)
    is CreateAssessmentCommand -> CreateAssessmentCommandHandler(serviceBundle)
    is CreateCollectionCommand -> CreateCollectionCommandHandler(serviceBundle)
    is CreateTimelineItemCommand -> CreateTimelineItemCommandHandler(serviceBundle)
    is SoftDeleteCommand -> SoftDeleteCommandHandler(serviceBundle)
    is RemoveCollectionItemCommand -> RemoveCollectionItemCommandHandler(serviceBundle)
    is ReorderCollectionItemCommand -> ReorderCollectionItemCommandHandler(serviceBundle)
    is RollbackCommand -> RollbackCommandHandler(serviceBundle)
    is UpdateAssessmentAnswersCommand -> UpdateAssessmentAnswersCommandHandler(serviceBundle)
    is UpdateAssessmentPropertiesCommand -> UpdateAssessmentPropertiesCommandHandler(serviceBundle)
    is UpdateCollectionItemAnswersCommand -> UpdateCollectionItemAnswersCommandHandler(serviceBundle)
    is UpdateCollectionItemPropertiesCommand -> UpdateCollectionItemPropertiesCommandHandler(serviceBundle)
    is UpdateFormVersionCommand -> UpdateFormVersionCommandHandler(serviceBundle)
    else -> throw CommandHandlerNotImplementedException("No handler registered for type: ${command::class.simpleName}")
  }
}
