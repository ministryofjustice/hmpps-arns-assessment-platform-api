//! Sentence plans.

use serde::Deserialize;
use utoipa::ToSchema;
use uuid::Uuid;

use crate::{
    AppState,
    clock::Clock,
    command::{
        self, AddCollectionItemCommand, Command, CreateCollectionCommand, CreateTimelineItemCommand,
        RemoveCollectionItemCommand, UpdateCollectionItemAnswersCommand, UpdateCollectionItemPropertiesCommand,
    },
    error::{Error, Result},
    model::{Answers, Timeline, UserDetails, Value},
    query::{self, AssessmentVersionQuery, Query, QueryResult},
};

const NOTE: &str = "Automatically removed as the previous supervision period has ended.";

#[derive(Clone, Debug, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct NewPeriodOfSupervisionRequest {
    pub user: UserDetails,
    pub assessment_uuid: Uuid,
}

/// Remove the plan's active and future goals, noting why, and its agreements.
pub async fn start_new_period_of_supervision(
    app: &AppState,
    clock: &Clock,
    request: NewPeriodOfSupervisionRequest,
) -> Result<()> {
    let NewPeriodOfSupervisionRequest { user, assessment_uuid } = request;

    let plan = {
        let mut conn = app.pool.acquire().await?;
        let query = Query::AssessmentVersionQuery(AssessmentVersionQuery {
            user: user.clone(),
            assessment_identifier: assessment_uuid.into(),
            timestamp: None,
        });
        match query::bus::dispatch(app, &mut conn, clock, &query).await? {
            QueryResult::AssessmentVersionQueryResult(result) => *result,
            _ => unreachable!("an assessment version query returns an assessment version"),
        }
    };

    if plan.assessment_type != "SENTENCE_PLAN" {
        return Err(Error::assessment_not_plan(assessment_uuid));
    }

    let now = Clock::system().now().to_java_string();
    let goals_to_remove: Vec<_> = plan
        .collections
        .iter()
        .find(|collection| collection.name == "GOALS")
        .map(|goals| {
            goals
                .items
                .iter()
                .filter(|goal| {
                    matches!(goal.properties.get("status"), Some(Value::Single { value }) if value == "ACTIVE" || value == "FUTURE")
                })
                .collect()
        })
        .unwrap_or_default();

    let mut commands = Vec::new();
    for goal in &goals_to_remove {
        let existing_notes = goal.collections.iter().find(|collection| collection.name == "NOTES");
        let create_notes = existing_notes.is_none().then(|| CreateCollectionCommand {
            name: "NOTES".to_owned(),
            parent_collection_item_uuid: Some(goal.uuid.into()),
            user: user.clone(),
            assessment_uuid: assessment_uuid.into(),
            timeline: None,
            hooks: None,
            autosaved: false,
            collection_uuid: Uuid::new_v4(),
        });
        let notes_uuid = existing_notes
            .map(|notes| notes.uuid)
            .or_else(|| create_notes.as_ref().map(|create| create.collection_uuid))
            .expect("the notes collection exists or is being created");

        commands.push(Command::UpdateCollectionItemPropertiesCommand(
            UpdateCollectionItemPropertiesCommand {
                collection_item_uuid: goal.uuid.into(),
                added: Answers::from([
                    ("status".to_owned(), Value::single("REMOVED")),
                    ("status_date".to_owned(), Value::single(now.clone())),
                ]),
                removed: Vec::new(),
                user: user.clone(),
                assessment_uuid: assessment_uuid.into(),
                timeline: None,
                hooks: None,
                autosaved: false,
            },
        ));
        commands.push(Command::UpdateCollectionItemAnswersCommand(
            UpdateCollectionItemAnswersCommand {
                collection_item_uuid: goal.uuid.into(),
                added: Answers::new(),
                removed: vec!["target_date".to_owned()],
                user: user.clone(),
                assessment_uuid: assessment_uuid.into(),
                timeline: None,
                hooks: None,
                autosaved: false,
            },
        ));
        commands.extend(create_notes.map(Command::CreateCollectionCommand));
        commands.push(Command::AddCollectionItemCommand(AddCollectionItemCommand {
            collection_uuid: notes_uuid.into(),
            answers: Answers::from([
                ("note".to_owned(), Value::single(NOTE)),
                ("created_by".to_owned(), Value::single("System")),
            ]),
            properties: Answers::from([
                ("type".to_owned(), Value::single("REMOVED")),
                ("created_at".to_owned(), Value::single(now.clone())),
            ]),
            index: None,
            user: user.clone(),
            assessment_uuid: assessment_uuid.into(),
            timeline: None,
            hooks: None,
            autosaved: false,
            collection_item_uuid: Uuid::new_v4(),
        }));
    }

    if let Some(agreements) = plan
        .collections
        .iter()
        .find(|collection| collection.name == "PLAN_AGREEMENTS")
    {
        commands.extend(agreements.items.iter().map(|agreement| {
            Command::RemoveCollectionItemCommand(RemoveCollectionItemCommand {
                collection_item_uuid: agreement.uuid.into(),
                user: user.clone(),
                assessment_uuid: assessment_uuid.into(),
                timeline: None,
                hooks: None,
            })
        }));
    }

    commands.push(Command::CreateTimelineItemCommand(CreateTimelineItemCommand {
        timestamp: Clock::system().now(),
        user: user.clone(),
        assessment_uuid: assessment_uuid.into(),
        timeline: Timeline {
            timeline_type: "NEW_PERIOD_OF_SUPERVISION".to_owned(),
            data: [("Goals removed".to_owned(), goals_to_remove.len().into())]
                .into_iter()
                .collect(),
        },
        hooks: None,
    }));

    command::bus::dispatch(app, clock, commands).await?;
    Ok(())
}
