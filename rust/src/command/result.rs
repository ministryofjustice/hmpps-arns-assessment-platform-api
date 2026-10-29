use serde::Serialize;
use utoipa::ToSchema;
use uuid::Uuid;

use super::Command;

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(tag = "type")]
pub enum CommandResult {
    AddCollectionItemCommandResult(AddCollectionItemCommandResult),
    CommandSuccessCommandResult(CommandSuccessCommandResult),
    CreateAssessmentCommandResult(CreateAssessmentCommandResult),
    CreateCollectionCommandResult(CreateCollectionCommandResult),
}

impl CommandResult {
    pub fn success() -> Self {
        Self::CommandSuccessCommandResult(CommandSuccessCommandResult {
            message: "Done".to_owned(),
            success: true,
        })
    }

    pub fn created_assessment(assessment_uuid: Uuid) -> Self {
        Self::CreateAssessmentCommandResult(CreateAssessmentCommandResult {
            assessment_uuid,
            message: format!("Assessment created successfully with UUID {assessment_uuid}"),
            success: true,
        })
    }

    pub fn created_collection(collection_uuid: Uuid) -> Self {
        Self::CreateCollectionCommandResult(CreateCollectionCommandResult {
            collection_uuid,
            message: format!("Collection created successfully with UUID {collection_uuid}"),
            success: true,
        })
    }

    pub fn added_collection_item(collection_item_uuid: Uuid) -> Self {
        Self::AddCollectionItemCommandResult(AddCollectionItemCommandResult {
            collection_item_uuid,
            message: format!("Collection item added successfully with UUID {collection_item_uuid}"),
            success: true,
        })
    }
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AddCollectionItemCommandResult {
    pub collection_item_uuid: Uuid,
    pub message: String,
    pub success: bool,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
pub struct CommandSuccessCommandResult {
    pub message: String,
    pub success: bool,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CreateAssessmentCommandResult {
    pub assessment_uuid: Uuid,
    pub message: String,
    pub success: bool,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CreateCollectionCommandResult {
    pub collection_uuid: Uuid,
    pub message: String,
    pub success: bool,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
pub struct CommandResponse {
    pub request: Command,
    pub result: CommandResult,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
pub struct CommandsResponse {
    pub commands: Vec<CommandResponse>,
}
