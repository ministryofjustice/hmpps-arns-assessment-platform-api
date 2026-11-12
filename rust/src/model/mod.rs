pub(crate) mod collection;
mod value;

use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

pub use collection::{Collection, CollectionItem};
pub use value::{Answers, Value};

use crate::{
    aggregate::{Aggregate, AggregateEntity},
    datetime::LocalDateTime,
    error::{Error, Result},
    event::Event,
    persistence::entity::{AuthSource, EventEntity, TimelineEntity, UserDetailsEntity},
};

/// Arbitrary JSON object, kept in insertion order.
pub type JsonMap = serde_json::Map<String, serde_json::Value>;

/// The user a request is made on behalf of, as supplied by the calling service.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct UserDetails {
    pub id: String,
    pub name: String,
    #[serde(default)]
    pub auth_source: AuthSource,
}

/// A user as returned by queries.
#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct User {
    pub id: Uuid,
    pub name: String,
    #[serde(default)]
    pub user_id: Option<String>,
    #[serde(default)]
    pub auth_source: Option<AuthSource>,
}

impl User {
    pub fn from_entity(entity: Option<&UserDetailsEntity>) -> Self {
        match entity {
            Some(entity) => Self {
                id: entity.uuid,
                name: entity.display_name.clone(),
                user_id: Some(entity.user_id.clone()),
                auth_source: Some(entity.auth_source),
            },
            None => Self {
                id: Uuid::nil(),
                name: "UNKNOWN".to_owned(),
                user_id: None,
                auth_source: None,
            },
        }
    }
}

/// A custom timeline entry supplied alongside a command.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
pub struct Timeline {
    #[serde(rename = "type")]
    pub timeline_type: String,
    #[schema(value_type = Object)]
    pub data: JsonMap,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct TimelineItem {
    pub uuid: Uuid,
    pub position: i32,
    pub timestamp: LocalDateTime,
    pub user: User,
    pub assessment: Uuid,
    pub event: Option<String>,
    #[serde(default)]
    #[schema(value_type = Object)]
    pub data: JsonMap,
    #[serde(default)]
    pub custom_type: Option<String>,
    #[serde(default)]
    #[schema(value_type = Option<Object>)]
    pub custom_data: Option<JsonMap>,
}

impl TimelineItem {
    pub fn from_entity(entity: &TimelineEntity) -> Result<Self> {
        Ok(Self {
            uuid: entity.uuid,
            position: entity.position.ok_or_else(|| {
                Error::undefined_position(format!(
                    "Timeline item with UUID: {} does not have a valid position",
                    entity.uuid
                ))
            })?,
            timestamp: entity.created_at,
            user: User::from_entity(Some(&entity.user)),
            assessment: entity.assessment_uuid,
            event: entity.event_type.clone(),
            data: entity.data.clone(),
            custom_type: entity.custom_type.clone(),
            custom_data: entity.custom_data.clone(),
        })
    }
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct EventDto {
    pub uuid: Uuid,
    pub created_at: LocalDateTime,
    pub position: i32,
    pub data: Event,
    pub autosaved: bool,
}

impl EventDto {
    pub fn from_entity(event: &EventEntity) -> Result<Self> {
        Ok(Self {
            uuid: event.uuid,
            created_at: event.created_at,
            position: event.position.ok_or_else(|| {
                Error::undefined_position(format!(
                    "Event with UUID: {} does not have a valid position",
                    event.uuid
                ))
            })?,
            data: event.data.clone(),
            autosaved: event.autosaved,
        })
    }
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AggregateDto {
    pub uuid: Uuid,
    pub position: i32,
    pub updated_at: LocalDateTime,
    pub events_from: LocalDateTime,
    pub events_to: LocalDateTime,
    pub number_of_events_applied: i64,
    pub assessment_uuid: Uuid,
    pub data: Aggregate,
}

impl AggregateDto {
    pub fn from_entity(aggregate: &AggregateEntity) -> Result<Self> {
        Ok(Self {
            uuid: aggregate.uuid,
            position: aggregate.position.ok_or_else(|| {
                Error::undefined_position(format!(
                    "Aggregate with UUID: {} does not have a valid position",
                    aggregate.uuid
                ))
            })?,
            updated_at: aggregate.updated_at,
            events_from: aggregate.events_from,
            events_to: aggregate.events_to,
            number_of_events_applied: aggregate.events_applied,
            assessment_uuid: aggregate.assessment_uuid,
            data: aggregate.data.clone(),
        })
    }
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct DailyVersionDetails {
    pub created_at: LocalDateTime,
    pub updated_at: LocalDateTime,
    pub last_timeline_item_uuid: Uuid,
}
