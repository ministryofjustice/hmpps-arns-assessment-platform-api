use std::{fmt, str::FromStr, sync::Arc};

use indexmap::IndexMap;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

use crate::{
    datetime::LocalDateTime,
    event::Event,
    model::{JsonMap, Timeline},
};

#[derive(Debug, thiserror::Error)]
#[error("Unknown {kind} '{value}'")]
pub struct UnknownVariant {
    kind: &'static str,
    value: String,
}

#[allow(clippy::upper_case_acronyms)]
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize, ToSchema)]
pub enum IdentifierType {
    CRN,
    PRN,
    #[serde(rename = "NOMIS_ID")]
    NomisId,
}

impl IdentifierType {
    pub fn as_str(self) -> &'static str {
        match self {
            Self::CRN => "CRN",
            Self::PRN => "PRN",
            Self::NomisId => "NOMIS_ID",
        }
    }
}

impl FromStr for IdentifierType {
    type Err = UnknownVariant;

    fn from_str(value: &str) -> Result<Self, Self::Err> {
        match value {
            "CRN" => Ok(Self::CRN),
            "PRN" => Ok(Self::PRN),
            "NOMIS_ID" => Ok(Self::NomisId),
            _ => Err(UnknownVariant {
                kind: "identifier type",
                value: value.to_owned(),
            }),
        }
    }
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Hash, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum AuthSource {
    Oasys,
    HmppsAuth,
    #[default]
    NotSpecified,
}

impl AuthSource {
    pub fn as_str(self) -> &'static str {
        match self {
            Self::Oasys => "OASYS",
            Self::HmppsAuth => "HMPPS_AUTH",
            Self::NotSpecified => "NOT_SPECIFIED",
        }
    }
}

impl FromStr for AuthSource {
    type Err = UnknownVariant;

    fn from_str(value: &str) -> Result<Self, Self::Err> {
        match value {
            "OASYS" => Ok(Self::Oasys),
            "HMPPS_AUTH" => Ok(Self::HmppsAuth),
            "NOT_SPECIFIED" => Ok(Self::NotSpecified),
            _ => Err(UnknownVariant {
                kind: "auth source",
                value: value.to_owned(),
            }),
        }
    }
}

/// An identifier from an upstream system, such as a CRN.
#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize, ToSchema)]
pub struct IdentifierPair {
    #[serde(rename = "type")]
    pub identifier_type: IdentifierType,
    pub id: String,
}

impl fmt::Display for IdentifierPair {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "IdentifierPair(type={}, id={})",
            self.identifier_type.as_str(),
            self.id
        )
    }
}

#[derive(Clone, Debug)]
pub struct AssessmentEntity {
    pub id: Option<i64>,
    pub uuid: Uuid,
    pub created_at: LocalDateTime,
    pub assessment_type: String,
    pub identifiers: Vec<AssessmentIdentifierEntity>,
}

impl AssessmentEntity {
    pub fn identifiers_map(&self) -> IndexMap<IdentifierType, String> {
        self.identifiers
            .iter()
            .map(|identifier| {
                (
                    identifier.external_identifier.identifier_type,
                    identifier.external_identifier.id.clone(),
                )
            })
            .collect()
    }
}

#[derive(Clone, Debug)]
pub struct AssessmentIdentifierEntity {
    pub id: Option<i64>,
    pub uuid: Uuid,
    pub created_at: LocalDateTime,
    pub external_identifier: IdentifierPair,
}

/// Serialised as-is into the timeline when a user is assigned to an assessment.
#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserDetailsEntity {
    pub id: Option<i64>,
    pub uuid: Uuid,
    pub user_id: String,
    pub display_name: String,
    pub auth_source: AuthSource,
}

#[derive(Clone, Debug)]
pub struct EventEntity {
    pub id: Option<i64>,
    pub uuid: Uuid,
    pub position: Option<i32>,
    pub created_at: LocalDateTime,
    pub user: UserDetailsEntity,
    pub assessment: Arc<AssessmentEntity>,
    pub data: Event,
    pub autosaved: bool,
    pub deleted: bool,
}

impl EventEntity {
    pub fn new(
        user: UserDetailsEntity,
        assessment: Arc<AssessmentEntity>,
        data: Event,
        created_at: LocalDateTime,
        autosaved: bool,
    ) -> Self {
        Self {
            id: None,
            uuid: Uuid::new_v4(),
            position: None,
            created_at,
            user,
            assessment,
            data,
            autosaved,
            deleted: false,
        }
    }
}

#[derive(Clone, Debug)]
pub struct TimelineEntity {
    pub id: Option<i64>,
    pub uuid: Uuid,
    pub position: Option<i32>,
    pub created_at: LocalDateTime,
    pub user: UserDetailsEntity,
    pub assessment_uuid: Uuid,
    pub event_type: Option<String>,
    pub data: JsonMap,
    pub custom_type: Option<String>,
    pub custom_data: Option<JsonMap>,
    pub deleted: bool,
}

impl TimelineEntity {
    /// A timeline entry that isn't tied to an event.
    pub fn custom(
        created_at: LocalDateTime,
        user: UserDetailsEntity,
        assessment_uuid: Uuid,
        timeline: &Timeline,
    ) -> Self {
        Self {
            id: None,
            uuid: Uuid::new_v4(),
            position: None,
            created_at,
            user,
            assessment_uuid,
            event_type: None,
            data: JsonMap::new(),
            custom_type: Some(timeline.timeline_type.clone()),
            custom_data: Some(timeline.data.clone()),
            deleted: false,
        }
    }
}

/// The timeline entry an event produces, completed with the custom timeline (if any) supplied
/// by the command that raised the event.
#[derive(Clone, Debug)]
pub struct TimelineDraft {
    pub created_at: LocalDateTime,
    pub user: UserDetailsEntity,
    pub assessment_uuid: Uuid,
    pub event_type: String,
    pub data: JsonMap,
}

impl TimelineDraft {
    pub fn new(event: &EventEntity, data: JsonMap) -> Self {
        Self {
            created_at: event.created_at,
            user: event.user.clone(),
            assessment_uuid: event.assessment.uuid,
            event_type: event.data.name().to_owned(),
            data,
        }
    }

    pub fn complete(&self, custom: Option<&Timeline>) -> TimelineEntity {
        TimelineEntity {
            id: None,
            uuid: Uuid::new_v4(),
            position: None,
            created_at: self.created_at,
            user: self.user.clone(),
            assessment_uuid: self.assessment_uuid,
            event_type: Some(self.event_type.clone()),
            data: self.data.clone(),
            custom_type: custom.map(|timeline| timeline.timeline_type.clone()),
            custom_data: custom.map(|timeline| timeline.data.clone()),
            deleted: false,
        }
    }
}
