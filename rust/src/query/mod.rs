pub mod bus;
mod handler;
mod result;

use chrono::NaiveDate;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

pub use result::*;

use crate::{
    datetime::LocalDateTime,
    model::UserDetails,
    persistence::entity::{IdentifierPair, IdentifierType},
};

/// Identifies an assessment by UUID, or by an identifier from an upstream system.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(tag = "type")]
pub enum AssessmentIdentifier {
    #[serde(rename = "EXTERNAL")]
    External(ExternalIdentifier),
    #[serde(rename = "UUID")]
    Uuid(UuidIdentifier),
}

impl From<Uuid> for AssessmentIdentifier {
    fn from(uuid: Uuid) -> Self {
        Self::Uuid(UuidIdentifier { uuid })
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct ExternalIdentifier {
    pub identifier: String,
    pub identifier_type: IdentifierType,
    pub assessment_type: String,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
pub struct UuidIdentifier {
    pub uuid: Uuid,
}

/// A read of assessment data, identified by the `type` property. Most accept a `timestamp` to
/// read the assessment as it was at that time.
#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(tag = "type")]
pub enum Query {
    TimelineQuery(TimelineQuery),
    AssessmentVersionQuery(AssessmentVersionQuery),
    CollectionItemQuery(CollectionItemQuery),
    CollectionQuery(CollectionQuery),
    DailyVersionsQuery(DailyVersionsQuery),
    GetAssessmentsModifiedSinceQuery(GetAssessmentsModifiedSinceQuery),
    GetAssessmentsSoftDeletedSinceQuery(GetAssessmentsSoftDeletedSinceQuery),
}

impl Query {
    pub fn name(&self) -> &'static str {
        match self {
            Self::TimelineQuery(_) => "TimelineQuery",
            Self::AssessmentVersionQuery(_) => "AssessmentVersionQuery",
            Self::CollectionItemQuery(_) => "CollectionItemQuery",
            Self::CollectionQuery(_) => "CollectionQuery",
            Self::DailyVersionsQuery(_) => "DailyVersionsQuery",
            Self::GetAssessmentsModifiedSinceQuery(_) => "GetAssessmentsModifiedSinceQuery",
            Self::GetAssessmentsSoftDeletedSinceQuery(_) => "GetAssessmentsSoftDeletedSinceQuery",
        }
    }

    pub fn user(&self) -> &UserDetails {
        match self {
            Self::TimelineQuery(query) => &query.user,
            Self::AssessmentVersionQuery(query) => &query.user,
            Self::CollectionItemQuery(query) => &query.user,
            Self::CollectionQuery(query) => &query.user,
            Self::DailyVersionsQuery(query) => &query.user,
            Self::GetAssessmentsModifiedSinceQuery(query) => &query.user,
            Self::GetAssessmentsSoftDeletedSinceQuery(query) => &query.user,
        }
    }
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AssessmentVersionQuery {
    pub user: UserDetails,
    pub assessment_identifier: AssessmentIdentifier,
    #[serde(default)]
    pub timestamp: Option<LocalDateTime>,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CollectionItemQuery {
    pub user: UserDetails,
    pub assessment_identifier: AssessmentIdentifier,
    #[serde(default)]
    pub timestamp: Option<LocalDateTime>,
    pub collection_item_uuid: Uuid,
    /// How many levels of nested collections to include; -1 for all of them.
    #[serde(default)]
    pub depth: i32,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CollectionQuery {
    pub user: UserDetails,
    pub assessment_identifier: AssessmentIdentifier,
    #[serde(default)]
    pub timestamp: Option<LocalDateTime>,
    pub collection_uuid: Uuid,
    /// How many levels of nested collections to include; -1 for all of them.
    #[serde(default)]
    pub depth: i32,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct DailyVersionsQuery {
    pub user: UserDetails,
    pub assessment_identifier: AssessmentIdentifier,
    #[serde(default)]
    pub timestamp: Option<LocalDateTime>,
}

fn default_limit() -> i32 {
    50
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct GetAssessmentsModifiedSinceQuery {
    pub user: UserDetails,
    pub assessment_type: String,
    pub since: LocalDateTime,
    /// The `nextCursor` from the previous page.
    #[serde(default)]
    pub after: Option<Uuid>,
    #[serde(default = "default_limit")]
    pub limit: i32,
    #[serde(default)]
    pub timestamp: Option<LocalDateTime>,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct GetAssessmentsSoftDeletedSinceQuery {
    pub assessment_type: String,
    pub since: LocalDateTime,
    pub user: UserDetails,
    #[serde(default)]
    pub timestamp: Option<LocalDateTime>,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct TimelineQuery {
    pub user: UserDetails,
    #[serde(default)]
    pub timestamp: Option<LocalDateTime>,
    #[serde(default)]
    pub page_number: i32,
    #[serde(default = "default_limit")]
    pub page_size: i32,
    #[serde(default)]
    pub assessment_identifier: Option<AssessmentIdentifier>,
    /// Only entries by this user.
    #[serde(default)]
    pub subject: Option<UserDetails>,
    #[serde(default)]
    pub from: Option<LocalDateTime>,
    #[serde(default)]
    pub to: Option<LocalDateTime>,
    #[serde(default)]
    pub include_event_types: Option<Vec<String>>,
    #[serde(default)]
    pub exclude_event_types: Option<Vec<String>>,
    #[serde(default)]
    pub include_custom_types: Option<Vec<String>>,
    #[serde(default)]
    pub exclude_custom_types: Option<Vec<String>>,
}

/// Everything held about a person, for a subject access request. Not available through `/query`.
#[derive(Clone, Debug)]
pub struct SubjectAccessRequestQuery {
    pub timestamp: Option<LocalDateTime>,
    pub assessment_identifiers: Vec<IdentifierPair>,
    pub from: Option<NaiveDate>,
    pub to: Option<NaiveDate>,
}
