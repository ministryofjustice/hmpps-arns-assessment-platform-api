package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AssessmentRolledBackEvent::class, name = "AssessmentRolledBackEvent"),
  JsonSubTypes.Type(value = AssessmentAnswersUpdatedEvent::class, name = "AssessmentAnswersUpdatedEvent"),
  JsonSubTypes.Type(value = AssessmentCreatedEvent::class, name = "AssessmentCreatedEvent"),
  JsonSubTypes.Type(value = AssessmentPropertiesUpdatedEvent::class, name = "AssessmentPropertiesUpdatedEvent"),
  JsonSubTypes.Type(value = CollectionCreatedEvent::class, name = "CollectionCreatedEvent"),
  JsonSubTypes.Type(value = CollectionItemAddedEvent::class, name = "CollectionItemAddedEvent"),
  JsonSubTypes.Type(value = CollectionItemAnswersUpdatedEvent::class, name = "CollectionItemAnswersUpdatedEvent"),
  JsonSubTypes.Type(value = CollectionItemPropertiesUpdatedEvent::class, name = "CollectionItemPropertiesUpdatedEvent"),
  JsonSubTypes.Type(value = CollectionItemRemovedEvent::class, name = "CollectionItemRemovedEvent"),
  JsonSubTypes.Type(value = CollectionItemReorderedEvent::class, name = "CollectionItemReorderedEvent"),
  JsonSubTypes.Type(value = FormVersionUpdatedEvent::class, name = "FormVersionUpdatedEvent"),
  JsonSubTypes.Type(value = GroupEvent::class, name = "GroupEvent"),
)
sealed interface Event {
  val timeline: Timeline?
}
