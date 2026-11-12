use axum::http::StatusCode;
use uuid::Uuid;

use crate::{datetime::LocalDateTime, persistence::entity::IdentifierPair, query::AssessmentIdentifier};

const POSTGRES_UNIQUE_VIOLATION: &str = "23505";

#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// A failure with a user-facing message, a developer message and a status code. The
    /// equivalent of the Kotlin `AssessmentPlatformException` hierarchy.
    #[error("{message}")]
    Platform {
        message: String,
        developer_message: String,
        status: StatusCode,
    },
    #[error("{} was unable to handle {} with UUID: {}", .0.handler_name, .0.event_name, .0.event_uuid)]
    EventHandling(Box<EventHandlingError>),
    #[error("Optimistic lock failure: {0}")]
    OptimisticLock(String),
    #[error(transparent)]
    Database(#[from] sqlx::Error),
    #[error(transparent)]
    Serialization(#[from] serde_json::Error),
    #[error("{0}")]
    Internal(String),
}

#[derive(Debug)]
pub struct EventHandlingError {
    pub event_uuid: Uuid,
    pub event_name: String,
    pub handler_name: String,
    pub cause: Error,
}

pub type Result<T, E = Error> = std::result::Result<T, E>;

impl Error {
    fn platform(status: StatusCode, message: impl Into<String>, developer_message: impl Into<String>) -> Self {
        Self::Platform {
            message: message.into(),
            developer_message: developer_message.into(),
            status,
        }
    }

    pub fn status(&self) -> StatusCode {
        match self {
            Self::Platform { status, .. } => *status,
            Self::EventHandling(_) => StatusCode::BAD_REQUEST,
            Self::OptimisticLock(_) | Self::Database(_) | Self::Serialization(_) | Self::Internal(_) => {
                StatusCode::INTERNAL_SERVER_ERROR
            }
        }
    }

    pub fn developer_message(&self) -> String {
        match self {
            Self::Platform { developer_message, .. } => developer_message.clone(),
            Self::EventHandling(inner) => inner.cause.developer_message(),
            other => other.to_string(),
        }
    }

    /// Failures caused by another request writing the same assessment concurrently, which are
    /// worth retrying from the start.
    pub fn is_concurrency_conflict(&self) -> bool {
        match self {
            Self::OptimisticLock(_) => true,
            Self::Database(sqlx::Error::Database(db_error)) => {
                db_error.code().as_deref() == Some(POSTGRES_UNIQUE_VIOLATION)
            }
            _ => false,
        }
    }

    pub fn event_handling(event_uuid: Uuid, event_name: &str, cause: Error) -> Self {
        Self::EventHandling(Box::new(EventHandlingError {
            event_uuid,
            event_name: event_name.to_owned(),
            handler_name: format!("{event_name}Handler"),
            cause,
        }))
    }

    pub fn answer_not_found(field_code: &str, aggregate_uuid: Uuid) -> Self {
        Self::platform(
            StatusCode::BAD_REQUEST,
            "Answer not found",
            format!("The answer \"{field_code}\" could not be found on aggregate \"{aggregate_uuid}\""),
        )
    }

    pub fn property_not_found(property_name: &str, aggregate_uuid: Uuid) -> Self {
        Self::platform(
            StatusCode::BAD_REQUEST,
            "Property not found",
            format!("The property \"{property_name}\" could not be found on aggregate \"{aggregate_uuid}\""),
        )
    }

    pub fn collection_not_found(collection_uuid: Uuid, aggregate_uuid: Uuid) -> Self {
        Self::platform(
            StatusCode::BAD_REQUEST,
            "Collection not found",
            format!("No collection found with the UUID \"{collection_uuid}\" on aggregate \"{aggregate_uuid}\""),
        )
    }

    pub fn collection_item_not_found(collection_item_uuid: Uuid, aggregate_uuid: Uuid) -> Self {
        Self::platform(
            StatusCode::BAD_REQUEST,
            "Collection item not found",
            format!(
                "No collection item found with the UUID \"{collection_item_uuid}\" on aggregate \"{aggregate_uuid}\""
            ),
        )
    }

    pub fn collection_index_out_of_bounds(index: i32, size: usize) -> Self {
        Self::platform(
            StatusCode::BAD_REQUEST,
            "Collection index out of bounds",
            format!("Index: {index}, Size: {size}"),
        )
    }

    pub fn assessment_not_found(identifier: &AssessmentIdentifier) -> Self {
        let developer_message = match identifier {
            AssessmentIdentifier::External(external) => format!(
                "No assessment found for {}: {} and type: {}",
                external.identifier_type.as_str(),
                external.identifier,
                external.assessment_type
            ),
            AssessmentIdentifier::Uuid(by_uuid) => format!("No assessment found for UUID: {}", by_uuid.uuid),
        };
        Self::platform(StatusCode::NOT_FOUND, "Assessment not found", developer_message)
    }

    pub fn event_not_found(uuid: Uuid) -> Self {
        Self::platform(
            StatusCode::NOT_FOUND,
            "Event not found",
            format!("Event {uuid} not found"),
        )
    }

    pub fn timeline_not_found(uuid: Uuid) -> Self {
        Self::platform(
            StatusCode::NOT_FOUND,
            "Timeline not found",
            format!("Timeline {uuid} not found"),
        )
    }

    pub fn user_details_not_found() -> Self {
        Self::platform(StatusCode::NOT_FOUND, "User not found", "User not found")
    }

    pub fn invalid_timestamp(timestamp: LocalDateTime, message: &str) -> Self {
        Self::platform(
            StatusCode::BAD_REQUEST,
            format!("Invalid timestamp {timestamp}"),
            message,
        )
    }

    pub fn invalid_backdate(message: String) -> Self {
        Self::platform(StatusCode::BAD_REQUEST, "Invalid backdateTo parameter", message)
    }

    pub fn undelete_not_at_tail(assessment_uuid: Uuid, point_in_time: LocalDateTime) -> Self {
        Self::platform(
            StatusCode::CONFLICT,
            "Unable to undelete",
            format!(
                "Events deleted from {point_in_time} for assessment {assessment_uuid} are not at the end of the event stream, so they cannot be safely undeleted"
            ),
        )
    }

    pub fn placeholder_not_found(index: usize) -> Self {
        Self::platform(
            StatusCode::BAD_REQUEST,
            format!("Placeholder value not found @{index}"),
            format!("Null value found at index: {index}"),
        )
    }

    pub fn invalid_reference(raw: &str) -> Self {
        Self::invalid_command(format!(
            "Invalid reference \"{raw}\": expected a UUID or a placeholder such as @0"
        ))
    }

    pub fn invalid_command(message: impl Into<String>) -> Self {
        Self::platform(StatusCode::BAD_REQUEST, "Unable to process commands", message)
    }

    pub fn invalid_query(message: impl Into<String>) -> Self {
        Self::platform(StatusCode::BAD_REQUEST, "Unable to process queries", message)
    }

    pub fn timeline_bad_criteria() -> Self {
        Self::platform(
            StatusCode::BAD_REQUEST,
            "Unable to execute timeline query",
            "Must specify at least one of assessmentUuid or userUuid",
        )
    }

    pub fn collection_depth_out_of_bounds(depth: i32, collection_uuid: Uuid) -> Self {
        Self::platform(
            StatusCode::BAD_REQUEST,
            "Collection depth out of bounds",
            format!("Invalid depth {depth} for collection \"{collection_uuid}\""),
        )
    }

    pub fn subject_access_request_no_assessments(identifiers: &[IdentifierPair]) -> Self {
        let identifiers = identifiers
            .iter()
            .map(|pair| format!("{}={}", pair.identifier_type.as_str(), pair.id))
            .collect::<Vec<_>>()
            .join(", ");
        Self::platform(
            StatusCode::NO_CONTENT,
            "There are no assessments",
            format!("There were no assessments found for the identifiers: {identifiers}"),
        )
    }

    pub fn assessment_not_plan(assessment_uuid: Uuid) -> Self {
        Self::platform(
            StatusCode::BAD_REQUEST,
            "Assessment is not a Sentence Plan",
            format!("Assessment with UUID: {assessment_uuid} is not a Sentence Plan"),
        )
    }

    pub fn undefined_position(message: String) -> Self {
        Self::platform(StatusCode::BAD_REQUEST, message.clone(), message)
    }
}
