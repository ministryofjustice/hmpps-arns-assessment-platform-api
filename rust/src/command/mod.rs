pub mod bus;
mod handler;
mod reference;
mod result;

use indexmap::IndexMap;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

pub use reference::Reference;
pub use result::*;

use crate::{
    datetime::LocalDateTime,
    hook::Hook,
    model::{Answers, Timeline, UserDetails},
    persistence::entity::IdentifierType,
};

/// A change to an assessment, identified by the `type` property.
#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(tag = "type")]
pub enum Command {
    AddCollectionItemCommand(AddCollectionItemCommand),
    CreateAssessmentCommand(CreateAssessmentCommand),
    CreateCollectionCommand(CreateCollectionCommand),
    CreateTimelineItemCommand(CreateTimelineItemCommand),
    SoftDeleteCommand(SoftDeleteCommand),
    UndeleteCommand(UndeleteCommand),
    RemoveCollectionItemCommand(RemoveCollectionItemCommand),
    ReorderCollectionItemCommand(ReorderCollectionItemCommand),
    RollbackCommand(RollbackCommand),
    UpdateAssessmentAnswersCommand(UpdateAssessmentAnswersCommand),
    UpdateAssessmentPropertiesCommand(UpdateAssessmentPropertiesCommand),
    UpdateCollectionItemAnswersCommand(UpdateCollectionItemAnswersCommand),
    UpdateCollectionItemPropertiesCommand(UpdateCollectionItemPropertiesCommand),
    UpdateFlagsCommand(UpdateFlagsCommand),
    UpdateFormVersionCommand(UpdateFormVersionCommand),
}

/// Evaluate an expression against whichever command variant this is.
macro_rules! with_command {
    ($command:expr, $inner:ident => $body:expr) => {
        match $command {
            Command::AddCollectionItemCommand($inner) => $body,
            Command::CreateAssessmentCommand($inner) => $body,
            Command::CreateCollectionCommand($inner) => $body,
            Command::CreateTimelineItemCommand($inner) => $body,
            Command::SoftDeleteCommand($inner) => $body,
            Command::UndeleteCommand($inner) => $body,
            Command::RemoveCollectionItemCommand($inner) => $body,
            Command::ReorderCollectionItemCommand($inner) => $body,
            Command::RollbackCommand($inner) => $body,
            Command::UpdateAssessmentAnswersCommand($inner) => $body,
            Command::UpdateAssessmentPropertiesCommand($inner) => $body,
            Command::UpdateCollectionItemAnswersCommand($inner) => $body,
            Command::UpdateCollectionItemPropertiesCommand($inner) => $body,
            Command::UpdateFlagsCommand($inner) => $body,
            Command::UpdateFormVersionCommand($inner) => $body,
        }
    };
}

impl Command {
    pub fn name(&self) -> &'static str {
        match self {
            Self::AddCollectionItemCommand(_) => "AddCollectionItemCommand",
            Self::CreateAssessmentCommand(_) => "CreateAssessmentCommand",
            Self::CreateCollectionCommand(_) => "CreateCollectionCommand",
            Self::CreateTimelineItemCommand(_) => "CreateTimelineItemCommand",
            Self::SoftDeleteCommand(_) => "SoftDeleteCommand",
            Self::UndeleteCommand(_) => "UndeleteCommand",
            Self::RemoveCollectionItemCommand(_) => "RemoveCollectionItemCommand",
            Self::ReorderCollectionItemCommand(_) => "ReorderCollectionItemCommand",
            Self::RollbackCommand(_) => "RollbackCommand",
            Self::UpdateAssessmentAnswersCommand(_) => "UpdateAssessmentAnswersCommand",
            Self::UpdateAssessmentPropertiesCommand(_) => "UpdateAssessmentPropertiesCommand",
            Self::UpdateCollectionItemAnswersCommand(_) => "UpdateCollectionItemAnswersCommand",
            Self::UpdateCollectionItemPropertiesCommand(_) => "UpdateCollectionItemPropertiesCommand",
            Self::UpdateFlagsCommand(_) => "UpdateFlagsCommand",
            Self::UpdateFormVersionCommand(_) => "UpdateFormVersionCommand",
        }
    }

    pub fn user(&self) -> &UserDetails {
        with_command!(self, command => &command.user)
    }

    /// The assessment the command applies to. References must have been resolved.
    pub fn assessment_uuid(&self) -> Uuid {
        match self {
            Self::CreateAssessmentCommand(command) => command.assessment_uuid,
            other => with_command!(other, command => command.assessment_reference().uuid()),
        }
    }

    pub fn timeline(&self) -> Option<&Timeline> {
        match self {
            Self::CreateTimelineItemCommand(command) => Some(&command.timeline),
            other => with_command!(other, command => command.custom_timeline()),
        }
    }

    pub fn hooks(&self) -> &[Hook] {
        with_command!(self, command => command.hooks.as_deref().unwrap_or_default())
    }

    /// Only some commands can be autosaved; the rest never are.
    pub fn autosaved(&self) -> bool {
        match self {
            Self::AddCollectionItemCommand(command) => command.autosaved,
            Self::CreateCollectionCommand(command) => command.autosaved,
            Self::ReorderCollectionItemCommand(command) => command.autosaved,
            Self::UpdateAssessmentAnswersCommand(command) => command.autosaved,
            Self::UpdateAssessmentPropertiesCommand(command) => command.autosaved,
            Self::UpdateCollectionItemAnswersCommand(command) => command.autosaved,
            Self::UpdateCollectionItemPropertiesCommand(command) => command.autosaved,
            Self::UpdateFlagsCommand(command) => command.autosaved,
            Self::CreateAssessmentCommand(_)
            | Self::CreateTimelineItemCommand(_)
            | Self::SoftDeleteCommand(_)
            | Self::UndeleteCommand(_)
            | Self::RemoveCollectionItemCommand(_)
            | Self::RollbackCommand(_)
            | Self::UpdateFormVersionCommand(_) => false,
        }
    }

    fn references_mut(&mut self) -> Vec<&mut Reference> {
        match self {
            Self::CreateAssessmentCommand(_) => Vec::new(),
            Self::AddCollectionItemCommand(command) => {
                vec![&mut command.collection_uuid, &mut command.assessment_uuid]
            }
            Self::CreateCollectionCommand(command) => {
                let mut references = vec![&mut command.assessment_uuid];
                references.extend(command.parent_collection_item_uuid.as_mut());
                references
            }
            Self::RemoveCollectionItemCommand(command) => {
                vec![&mut command.collection_item_uuid, &mut command.assessment_uuid]
            }
            Self::ReorderCollectionItemCommand(command) => {
                vec![&mut command.collection_item_uuid, &mut command.assessment_uuid]
            }
            Self::UpdateCollectionItemAnswersCommand(command) => {
                vec![&mut command.collection_item_uuid, &mut command.assessment_uuid]
            }
            Self::UpdateCollectionItemPropertiesCommand(command) => {
                vec![&mut command.collection_item_uuid, &mut command.assessment_uuid]
            }
            Self::CreateTimelineItemCommand(command) => vec![&mut command.assessment_uuid],
            Self::SoftDeleteCommand(command) => vec![&mut command.assessment_uuid],
            Self::UndeleteCommand(command) => vec![&mut command.assessment_uuid],
            Self::RollbackCommand(command) => vec![&mut command.assessment_uuid],
            Self::UpdateAssessmentAnswersCommand(command) => vec![&mut command.assessment_uuid],
            Self::UpdateAssessmentPropertiesCommand(command) => vec![&mut command.assessment_uuid],
            Self::UpdateFlagsCommand(command) => vec![&mut command.assessment_uuid],
            Self::UpdateFormVersionCommand(command) => vec![&mut command.assessment_uuid],
        }
    }

    /// Resolve UUID and `@index` placeholder references against the earlier commands' results.
    pub fn resolve_references(&mut self, earlier: &[CommandResponse]) -> crate::error::Result<()> {
        self.references_mut()
            .into_iter()
            .try_for_each(|reference| reference.resolve(earlier))
    }
}

/// Fields every command other than [`CreateAssessmentCommand`] shares.
trait AssessmentCommand {
    fn assessment_reference(&self) -> &Reference;
    fn custom_timeline(&self) -> Option<&Timeline>;
}

macro_rules! assessment_command {
    ($($command:ident),* $(,)?) => {
        $(impl AssessmentCommand for $command {
            fn assessment_reference(&self) -> &Reference {
                &self.assessment_uuid
            }

            fn custom_timeline(&self) -> Option<&Timeline> {
                self.timeline.as_ref()
            }
        })*
    };
}

assessment_command!(
    AddCollectionItemCommand,
    CreateCollectionCommand,
    SoftDeleteCommand,
    UndeleteCommand,
    RemoveCollectionItemCommand,
    ReorderCollectionItemCommand,
    RollbackCommand,
    UpdateAssessmentAnswersCommand,
    UpdateAssessmentPropertiesCommand,
    UpdateCollectionItemAnswersCommand,
    UpdateCollectionItemPropertiesCommand,
    UpdateFlagsCommand,
    UpdateFormVersionCommand,
);

impl AssessmentCommand for CreateTimelineItemCommand {
    fn assessment_reference(&self) -> &Reference {
        &self.assessment_uuid
    }

    fn custom_timeline(&self) -> Option<&Timeline> {
        Some(&self.timeline)
    }
}

impl AssessmentCommand for CreateAssessmentCommand {
    fn assessment_reference(&self) -> &Reference {
        unreachable!("CreateAssessmentCommand generates its own assessment UUID")
    }

    fn custom_timeline(&self) -> Option<&Timeline> {
        self.timeline.as_ref()
    }
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct AddCollectionItemCommand {
    pub collection_uuid: Reference,
    pub answers: Answers,
    pub properties: Answers,
    pub index: Option<i32>,
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
    #[serde(default)]
    pub autosaved: bool,
    #[serde(skip, default = "Uuid::new_v4")]
    pub collection_item_uuid: Uuid,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CreateAssessmentCommand {
    pub user: UserDetails,
    pub form_version: String,
    pub assessment_type: String,
    pub identifiers: Option<IndexMap<IdentifierType, String>>,
    pub properties: Option<Answers>,
    #[serde(default)]
    pub flags: Vec<String>,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
    #[serde(skip, default = "Uuid::new_v4")]
    pub assessment_uuid: Uuid,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CreateCollectionCommand {
    pub name: String,
    pub parent_collection_item_uuid: Option<Reference>,
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
    #[serde(default)]
    pub autosaved: bool,
    #[serde(skip, default = "Uuid::new_v4")]
    pub collection_uuid: Uuid,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CreateTimelineItemCommand {
    pub timestamp: LocalDateTime,
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub timeline: Timeline,
    pub hooks: Option<Vec<Hook>>,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct RemoveCollectionItemCommand {
    pub collection_item_uuid: Reference,
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct ReorderCollectionItemCommand {
    pub collection_item_uuid: Reference,
    pub index: i32,
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
    #[serde(default)]
    pub autosaved: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct RollbackCommand {
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub point_in_time: LocalDateTime,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
}

/// Marks every event and timeline entry from the point in time onwards as deleted.
#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct SoftDeleteCommand {
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub point_in_time: LocalDateTime,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
}

/// Reverses a [`SoftDeleteCommand`], provided nothing has been written since.
#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct UndeleteCommand {
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub point_in_time: LocalDateTime,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct UpdateAssessmentAnswersCommand {
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub added: Answers,
    pub removed: Vec<String>,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
    #[serde(default)]
    pub autosaved: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct UpdateAssessmentPropertiesCommand {
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub added: Answers,
    pub removed: Vec<String>,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
    #[serde(default)]
    pub autosaved: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct UpdateCollectionItemAnswersCommand {
    pub collection_item_uuid: Reference,
    pub added: Answers,
    pub removed: Vec<String>,
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
    #[serde(default)]
    pub autosaved: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct UpdateCollectionItemPropertiesCommand {
    pub collection_item_uuid: Reference,
    pub added: Answers,
    pub removed: Vec<String>,
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
    #[serde(default)]
    pub autosaved: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct UpdateFlagsCommand {
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub flags: Vec<String>,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
    #[serde(default)]
    pub autosaved: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct UpdateFormVersionCommand {
    pub user: UserDetails,
    pub assessment_uuid: Reference,
    pub version: String,
    pub timeline: Option<Timeline>,
    pub hooks: Option<Vec<Hook>>,
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    fn user() -> serde_json::Value {
        json!({"id": "user-1", "name": "A User"})
    }

    #[test]
    fn deserialises_commands_by_type() {
        let command: Command = serde_json::from_value(json!({
            "type": "UpdateAssessmentAnswersCommand",
            "user": user(),
            "assessmentUuid": "@0",
            "added": {"q1": {"type": "Single", "value": "YES"}},
            "removed": [],
        }))
        .unwrap();

        let Command::UpdateAssessmentAnswersCommand(update) = &command else {
            panic!("wrong command type: {command:?}");
        };
        assert_eq!(
            update.user.auth_source,
            crate::persistence::entity::AuthSource::NotSpecified
        );
        assert!(!update.autosaved);
        assert!(command.hooks().is_empty());
    }

    #[test]
    fn rejects_unknown_command_types() {
        let result = serde_json::from_value::<Command>(json!({"type": "LaunchRocketCommand", "user": user()}));
        assert!(result.is_err());
    }

    #[test]
    fn generated_uuids_are_not_read_from_or_written_to_json() {
        let command: Command = serde_json::from_value(json!({
            "type": "CreateAssessmentCommand",
            "user": user(),
            "formVersion": "1",
            "assessmentType": "SENTENCE_PLAN",
            "assessmentUuid": "00000000-0000-0000-0000-000000000001",
            "identifiers": {"CRN": "X123456"},
        }))
        .unwrap();

        assert_ne!(
            command.assessment_uuid().to_string(),
            "00000000-0000-0000-0000-000000000001"
        );
        let written = serde_json::to_value(&command).unwrap();
        assert_eq!(written["type"], "CreateAssessmentCommand");
        assert!(written.get("assessmentUuid").is_none());
        assert_eq!(written["identifiers"], json!({"CRN": "X123456"}));
    }

    #[test]
    fn resolves_placeholders_from_earlier_results() {
        let assessment = Uuid::new_v4();
        let collection = Uuid::new_v4();
        let earlier = vec![
            CommandResponse {
                request: serde_json::from_value(json!({
                    "type": "CreateAssessmentCommand", "user": user(), "formVersion": "1", "assessmentType": "T",
                }))
                .unwrap(),
                result: CommandResult::created_assessment(assessment),
            },
            CommandResponse {
                request: serde_json::from_value(json!({
                    "type": "CreateCollectionCommand", "user": user(), "name": "GOALS", "assessmentUuid": "@0",
                }))
                .unwrap(),
                result: CommandResult::created_collection(collection),
            },
        ];
        let mut command: Command = serde_json::from_value(json!({
            "type": "AddCollectionItemCommand",
            "user": user(),
            "assessmentUuid": "@0",
            "collectionUuid": "@1",
            "answers": {},
            "properties": {},
        }))
        .unwrap();

        command.resolve_references(&earlier).unwrap();

        assert_eq!(command.assessment_uuid(), assessment);
        let Command::AddCollectionItemCommand(add) = &command else {
            unreachable!()
        };
        assert_eq!(add.collection_uuid.uuid(), collection);
        assert_eq!(serde_json::to_value(&command).unwrap()["collectionUuid"], "@1");
    }

    #[test]
    fn placeholders_must_point_at_a_result_with_a_uuid() {
        let earlier = vec![CommandResponse {
            request: serde_json::from_value(json!({
                "type": "RollbackCommand", "user": user(), "assessmentUuid": Uuid::new_v4(), "pointInTime": "2025-01-01T00:00:00",
            }))
            .unwrap(),
            result: CommandResult::success(),
        }];
        let mut command: Command = serde_json::from_value(json!({
            "type": "UpdateFlagsCommand", "user": user(), "assessmentUuid": "@0", "flags": [],
        }))
        .unwrap();

        let error = command.resolve_references(&earlier).unwrap_err();
        assert_eq!(error.to_string(), "Placeholder value not found @0");

        let mut out_of_range = command.clone();
        assert!(out_of_range.resolve_references(&[]).is_err());
    }
}
