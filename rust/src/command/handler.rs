use std::sync::Arc;

use indexmap::IndexMap;
use sqlx::PgConnection;
use uuid::Uuid;

use super::*;
use crate::{
    clock::Clock,
    error::Result,
    event::{self, Event, bus},
    persistence::{
        PersistenceContext,
        entity::{AssessmentEntity, AssessmentIdentifierEntity, EventEntity, IdentifierPair, TimelineEntity},
        repository::timeline,
    },
    service::{self, state},
};

pub async fn handle(
    command: &Command,
    context: &mut PersistenceContext,
    conn: &mut PgConnection,
    clock: &Clock,
) -> Result<CommandResult> {
    // Most commands raise a single event; the rest are handled here and return early.
    let (data, result) = match command {
        Command::CreateAssessmentCommand(create) => return create_assessment(create, context, conn, clock).await,
        Command::CreateTimelineItemCommand(create) => return create_timeline_item(create, context, conn).await,
        Command::SoftDeleteCommand(soft_delete) => {
            let assessment = context
                .find_assessment(conn, soft_delete.assessment_uuid.uuid())
                .await?;
            service::event::soft_delete(conn, assessment.uuid, soft_delete.point_in_time).await?;
            timeline::soft_delete_from(conn, assessment.uuid, soft_delete.point_in_time).await?;
            rebuild_state(&assessment, context, conn, clock).await?;
            return add_custom_timeline(command, &assessment, context, conn, clock).await;
        }
        Command::UndeleteCommand(undelete) => {
            let assessment = context.find_assessment(conn, undelete.assessment_uuid.uuid()).await?;
            service::event::undelete(conn, assessment.uuid, undelete.point_in_time).await?;
            timeline::undelete_from(conn, assessment.uuid, undelete.point_in_time).await?;
            rebuild_state(&assessment, context, conn, clock).await?;
            return add_custom_timeline(command, &assessment, context, conn, clock).await;
        }
        Command::AddCollectionItemCommand(add) => (
            Event::CollectionItemAddedEvent(event::CollectionItemAddedEvent {
                collection_item_uuid: add.collection_item_uuid,
                collection_uuid: add.collection_uuid.uuid(),
                answers: add.answers.clone(),
                properties: add.properties.clone(),
                index: add.index,
            }),
            CommandResult::added_collection_item(add.collection_item_uuid),
        ),
        Command::CreateCollectionCommand(create) => (
            Event::CollectionCreatedEvent(event::CollectionCreatedEvent {
                collection_uuid: create.collection_uuid,
                name: create.name.clone(),
                parent_collection_item_uuid: create.parent_collection_item_uuid.as_ref().map(Reference::uuid),
            }),
            CommandResult::created_collection(create.collection_uuid),
        ),
        Command::RemoveCollectionItemCommand(remove) => (
            Event::CollectionItemRemovedEvent(event::CollectionItemRemovedEvent {
                collection_item_uuid: remove.collection_item_uuid.uuid(),
            }),
            CommandResult::success(),
        ),
        Command::ReorderCollectionItemCommand(reorder) => (
            Event::CollectionItemReorderedEvent(event::CollectionItemReorderedEvent {
                collection_item_uuid: reorder.collection_item_uuid.uuid(),
                index: reorder.index,
            }),
            CommandResult::success(),
        ),
        Command::RollbackCommand(rollback) => (
            Event::AssessmentRolledBackEvent(event::AssessmentRolledBackEvent {
                rolled_back_to: rollback.point_in_time,
            }),
            CommandResult::success(),
        ),
        Command::UpdateAssessmentAnswersCommand(update) => (
            Event::AssessmentAnswersUpdatedEvent(event::AssessmentAnswersUpdatedEvent {
                added: update.added.clone(),
                removed: update.removed.clone(),
            }),
            CommandResult::success(),
        ),
        Command::UpdateAssessmentPropertiesCommand(update) => (
            Event::AssessmentPropertiesUpdatedEvent(event::AssessmentPropertiesUpdatedEvent {
                added: update.added.clone(),
                removed: update.removed.clone(),
            }),
            CommandResult::success(),
        ),
        Command::UpdateCollectionItemAnswersCommand(update) => (
            Event::CollectionItemAnswersUpdatedEvent(event::CollectionItemAnswersUpdatedEvent {
                collection_item_uuid: update.collection_item_uuid.uuid(),
                added: update.added.clone(),
                removed: update.removed.clone(),
            }),
            CommandResult::success(),
        ),
        Command::UpdateCollectionItemPropertiesCommand(update) => (
            Event::CollectionItemPropertiesUpdatedEvent(event::CollectionItemPropertiesUpdatedEvent {
                collection_item_uuid: update.collection_item_uuid.uuid(),
                added: update.added.clone(),
                removed: update.removed.clone(),
            }),
            CommandResult::success(),
        ),
        Command::UpdateFlagsCommand(update) => (
            Event::AssessmentFlagsUpdatedEvent(event::AssessmentFlagsUpdatedEvent {
                flags: update.flags.clone(),
            }),
            CommandResult::success(),
        ),
        Command::UpdateFormVersionCommand(update) => (
            Event::FormVersionUpdatedEvent(event::FormVersionUpdatedEvent {
                version: update.version.clone(),
            }),
            CommandResult::success(),
        ),
    };

    let user = context.find_user_details(conn, command.user()).await?;
    let assessment = context.find_assessment(conn, command.assessment_uuid()).await?;
    let event = EventEntity::new(user, assessment, data, clock.request_date_time(), command.autosaved());

    let drafts = bus::handle(context, conn, clock, event).await?;
    context.add_timeline(drafts, command.timeline());
    Ok(result)
}

async fn create_assessment(
    command: &CreateAssessmentCommand,
    context: &mut PersistenceContext,
    conn: &mut PgConnection,
    clock: &Clock,
) -> Result<CommandResult> {
    let created_at = clock.request_date_time();
    let identifiers = command
        .identifiers
        .iter()
        .flatten()
        .map(|(identifier_type, id)| AssessmentIdentifierEntity {
            id: None,
            uuid: Uuid::new_v4(),
            created_at,
            external_identifier: IdentifierPair {
                identifier_type: *identifier_type,
                id: id.clone(),
            },
        })
        .collect();
    let assessment = Arc::new(AssessmentEntity {
        id: None,
        uuid: command.assessment_uuid,
        created_at,
        assessment_type: command.assessment_type.clone(),
        identifiers,
    });
    context.assessments.push(Arc::clone(&assessment));

    let user = context.find_user_details(conn, &command.user).await?;

    let created = EventEntity::new(
        user.clone(),
        Arc::clone(&assessment),
        Event::AssessmentCreatedEvent(event::AssessmentCreatedEvent {
            form_version: command.form_version.clone(),
            properties: command.properties.clone().unwrap_or_default(),
            flags: command.flags.clone(),
        }),
        created_at,
        false,
    );
    let assigned = EventEntity::new(
        user.clone(),
        Arc::clone(&assessment),
        Event::AssignedToUserEvent(event::AssignedToUserEvent { user_uuid: user.uuid }),
        assessment.created_at,
        false,
    );

    for event in [created, assigned] {
        let drafts = bus::handle(context, conn, clock, event).await?;
        context.add_timeline(drafts, command.timeline.as_ref());
    }

    Ok(CommandResult::created_assessment(assessment.uuid))
}

async fn create_timeline_item(
    command: &CreateTimelineItemCommand,
    context: &mut PersistenceContext,
    conn: &mut PgConnection,
) -> Result<CommandResult> {
    let assessment = context.find_assessment(conn, command.assessment_uuid.uuid()).await?;
    let user = context.find_user_details(conn, &command.user).await?;

    context.timeline.push(TimelineEntity::custom(
        command.timestamp,
        user,
        assessment.uuid,
        &command.timeline,
    ));
    Ok(CommandResult::success())
}

/// Replace the assessment's snapshots with ones rebuilt from the events that remain.
async fn rebuild_state(
    assessment: &Arc<AssessmentEntity>,
    context: &mut PersistenceContext,
    conn: &mut PgConnection,
    clock: &Clock,
) -> Result<()> {
    state::delete(conn, assessment.uuid).await?;
    let rebuilt = state::rebuild_from_events(conn, clock, assessment, None).await?;
    let evicted = state::persist(conn, &mut IndexMap::from([(assessment.uuid, rebuilt)])).await?;
    context.evictions.extend(evicted);
    Ok(())
}

async fn add_custom_timeline(
    command: &Command,
    assessment: &AssessmentEntity,
    context: &mut PersistenceContext,
    conn: &mut PgConnection,
    clock: &Clock,
) -> Result<CommandResult> {
    if let Some(timeline) = command.timeline() {
        let user = context.find_user_details(conn, command.user()).await?;
        context.timeline.push(TimelineEntity::custom(
            clock.request_date_time(),
            user,
            assessment.uuid,
            timeline,
        ));
    }
    Ok(CommandResult::success())
}
