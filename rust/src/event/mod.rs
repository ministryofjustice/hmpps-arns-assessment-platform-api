pub mod bus;

use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

use crate::{aggregate::AggregateType, datetime::LocalDateTime, model::Answers};

/// Everything that has happened to an assessment. Stored as JSONB in the `event` table, with the
/// variant name in the `type` property.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(tag = "type")]
pub enum Event {
    AssessmentRolledBackEvent(AssessmentRolledBackEvent),
    AssessmentAnswersUpdatedEvent(AssessmentAnswersUpdatedEvent),
    AssessmentCreatedEvent(AssessmentCreatedEvent),
    AssessmentFlagsUpdatedEvent(AssessmentFlagsUpdatedEvent),
    AssessmentPropertiesUpdatedEvent(AssessmentPropertiesUpdatedEvent),
    AssignedToUserEvent(AssignedToUserEvent),
    CollectionCreatedEvent(CollectionCreatedEvent),
    CollectionItemAddedEvent(CollectionItemAddedEvent),
    CollectionItemAnswersUpdatedEvent(CollectionItemAnswersUpdatedEvent),
    CollectionItemPropertiesUpdatedEvent(CollectionItemPropertiesUpdatedEvent),
    CollectionItemRemovedEvent(CollectionItemRemovedEvent),
    CollectionItemReorderedEvent(CollectionItemReorderedEvent),
    FormVersionUpdatedEvent(FormVersionUpdatedEvent),
    RedactedEvent(RedactedEvent),
}

impl Event {
    pub fn name(&self) -> &'static str {
        match self {
            Self::AssessmentRolledBackEvent(_) => "AssessmentRolledBackEvent",
            Self::AssessmentAnswersUpdatedEvent(_) => "AssessmentAnswersUpdatedEvent",
            Self::AssessmentCreatedEvent(_) => "AssessmentCreatedEvent",
            Self::AssessmentFlagsUpdatedEvent(_) => "AssessmentFlagsUpdatedEvent",
            Self::AssessmentPropertiesUpdatedEvent(_) => "AssessmentPropertiesUpdatedEvent",
            Self::AssignedToUserEvent(_) => "AssignedToUserEvent",
            Self::CollectionCreatedEvent(_) => "CollectionCreatedEvent",
            Self::CollectionItemAddedEvent(_) => "CollectionItemAddedEvent",
            Self::CollectionItemAnswersUpdatedEvent(_) => "CollectionItemAnswersUpdatedEvent",
            Self::CollectionItemPropertiesUpdatedEvent(_) => "CollectionItemPropertiesUpdatedEvent",
            Self::CollectionItemRemovedEvent(_) => "CollectionItemRemovedEvent",
            Self::CollectionItemReorderedEvent(_) => "CollectionItemReorderedEvent",
            Self::FormVersionUpdatedEvent(_) => "FormVersionUpdatedEvent",
            Self::RedactedEvent(_) => "RedactedEvent",
        }
    }

    /// The aggregates that are derived from this event.
    pub fn aggregate_types(&self) -> &'static [AggregateType] {
        match self {
            Self::RedactedEvent(_) => &[],
            _ => &[AggregateType::AssessmentAggregate],
        }
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AssessmentAnswersUpdatedEvent {
    pub added: Answers,
    pub removed: Vec<String>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AssessmentCreatedEvent {
    pub form_version: String,
    pub properties: Answers,
    #[serde(default)]
    pub flags: Vec<String>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AssessmentFlagsUpdatedEvent {
    pub flags: Vec<String>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AssessmentPropertiesUpdatedEvent {
    pub added: Answers,
    pub removed: Vec<String>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AssessmentRolledBackEvent {
    pub rolled_back_to: LocalDateTime,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AssignedToUserEvent {
    pub user_uuid: Uuid,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CollectionCreatedEvent {
    pub collection_uuid: Uuid,
    pub name: String,
    pub parent_collection_item_uuid: Option<Uuid>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CollectionItemAddedEvent {
    pub collection_item_uuid: Uuid,
    pub collection_uuid: Uuid,
    pub answers: Answers,
    pub properties: Answers,
    pub index: Option<i32>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CollectionItemAnswersUpdatedEvent {
    pub collection_item_uuid: Uuid,
    pub added: Answers,
    pub removed: Vec<String>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CollectionItemPropertiesUpdatedEvent {
    pub collection_item_uuid: Uuid,
    pub added: Answers,
    pub removed: Vec<String>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CollectionItemRemovedEvent {
    pub collection_item_uuid: Uuid,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CollectionItemReorderedEvent {
    pub collection_item_uuid: Uuid,
    pub index: i32,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct FormVersionUpdatedEvent {
    pub version: String,
}

/// Replaces an event removed by a data deletion request.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct RedactedEvent {
    pub event_type: String,
    pub date_redacted: LocalDateTime,
    pub redacted_by: String,
}
