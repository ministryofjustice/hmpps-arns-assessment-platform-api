use serde::{Deserialize, Deserializer, Serialize, Serializer};
use uuid::Uuid;

use super::{CommandResponse, CommandResult};
use crate::error::{Error, Result};

/// A UUID in a command, given either directly or as `@index` to use the UUID created by an
/// earlier command in the same request. Serialised back exactly as it was given.
#[derive(Clone, Debug)]
pub struct Reference {
    raw: String,
    value: Option<Uuid>,
}

impl Reference {
    pub fn resolve(&mut self, earlier: &[CommandResponse]) -> Result<()> {
        let value = match self.raw.strip_prefix('@') {
            Some(index) => {
                let index: usize = index.parse().map_err(|_| Error::invalid_reference(&self.raw))?;
                earlier
                    .get(index)
                    .and_then(|response| created_uuid(&response.result))
                    .ok_or_else(|| Error::placeholder_not_found(index))?
            }
            None => self.raw.parse().map_err(|_| Error::invalid_reference(&self.raw))?,
        };
        self.value = Some(value);
        Ok(())
    }

    /// The resolved UUID. Commands are always resolved before they are handled.
    pub fn uuid(&self) -> Uuid {
        self.value
            .unwrap_or_else(|| panic!("reference '{}' used before it was resolved", self.raw))
    }
}

fn created_uuid(result: &CommandResult) -> Option<Uuid> {
    match result {
        CommandResult::AddCollectionItemCommandResult(result) => Some(result.collection_item_uuid),
        CommandResult::CreateAssessmentCommandResult(result) => Some(result.assessment_uuid),
        CommandResult::CreateCollectionCommandResult(result) => Some(result.collection_uuid),
        CommandResult::CommandSuccessCommandResult(_) => None,
    }
}

impl From<Uuid> for Reference {
    fn from(uuid: Uuid) -> Self {
        Self {
            raw: uuid.to_string(),
            value: Some(uuid),
        }
    }
}

impl Serialize for Reference {
    fn serialize<S: Serializer>(&self, serializer: S) -> std::result::Result<S::Ok, S::Error> {
        serializer.serialize_str(&self.raw)
    }
}

impl<'de> Deserialize<'de> for Reference {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> std::result::Result<Self, D::Error> {
        Ok(Self {
            raw: String::deserialize(deserializer)?,
            value: None,
        })
    }
}

impl utoipa::PartialSchema for Reference {
    fn schema() -> utoipa::openapi::RefOr<utoipa::openapi::schema::Schema> {
        utoipa::openapi::ObjectBuilder::new()
            .schema_type(utoipa::openapi::schema::Type::String)
            .description(Some(
                "A UUID, or @index to use the UUID created by an earlier command in the same request",
            ))
            .into()
    }
}

impl utoipa::ToSchema for Reference {
    fn name() -> std::borrow::Cow<'static, str> {
        "Reference".into()
    }
}
