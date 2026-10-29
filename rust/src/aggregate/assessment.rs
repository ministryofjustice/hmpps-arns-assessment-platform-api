use indexmap::IndexSet;
use serde::{Deserialize, Serialize};
use serde_json::json;
use sqlx::PgConnection;
use utoipa::ToSchema;
use uuid::Uuid;

use super::{AggregateState, AggregateType};
use crate::{
    clock::Clock,
    error::{Error, Result},
    event::Event,
    model::{Answers, Collection, CollectionItem, JsonMap},
    persistence::entity::{EventEntity, TimelineDraft},
    service::state,
};

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", default)]
pub struct AssessmentAggregate {
    pub form_version: String,
    pub assigned_user: Option<Uuid>,
    pub properties: Answers,
    pub answers: Answers,
    pub collections: Vec<Collection>,
    #[schema(value_type = Vec<Uuid>)]
    pub collaborators: IndexSet<Uuid>,
    pub flags: Vec<String>,
}

impl AssessmentAggregate {
    pub fn get_collection(&self, collection_uuid: Uuid) -> Option<&Collection> {
        self.collections
            .iter()
            .find(|collection| collection.uuid == collection_uuid)
            .or_else(|| {
                self.collections.iter().find_map(|collection| {
                    collection
                        .items
                        .iter()
                        .find_map(|item| item.find_collection(collection_uuid))
                })
            })
    }

    pub fn get_collection_mut(&mut self, collection_uuid: Uuid) -> Option<&mut Collection> {
        if let Some(index) = self
            .collections
            .iter()
            .position(|collection| collection.uuid == collection_uuid)
        {
            return Some(&mut self.collections[index]);
        }
        self.collections.iter_mut().find_map(|collection| {
            collection
                .items
                .iter_mut()
                .find_map(|item| item.find_collection_mut(collection_uuid))
        })
    }

    /// The collection, at any depth, that directly contains the given item.
    pub fn get_collection_with_item_mut(&mut self, collection_item_uuid: Uuid) -> Option<&mut Collection> {
        for collection in self.collections.iter_mut() {
            if collection.items.iter().any(|item| item.uuid == collection_item_uuid) {
                return Some(collection);
            }
            if let Some(found) = collection
                .items
                .iter_mut()
                .find_map(|item| item.find_collection_with_item_mut(collection_item_uuid))
            {
                return Some(found);
            }
        }
        None
    }

    pub fn get_collection_item(&self, collection_item_uuid: Uuid) -> Option<&CollectionItem> {
        self.collections
            .iter()
            .find_map(|collection| collection.find_item(collection_item_uuid))
    }

    pub fn get_collection_item_mut(&mut self, collection_item_uuid: Uuid) -> Option<&mut CollectionItem> {
        self.collections
            .iter_mut()
            .find_map(|collection| collection.find_item_mut(collection_item_uuid))
    }
}

/// Apply an event to the assessment aggregate, returning the timeline entry it produces.
pub async fn handle(
    event: &EventEntity,
    state: &mut AggregateState,
    conn: &mut PgConnection,
    clock: &Clock,
) -> Result<Option<TimelineDraft>> {
    let Event::AssessmentRolledBackEvent(rolled_back) = &event.data else {
        return apply(event, state, clock);
    };

    state.for_write(clock);
    let mut previous = state::fetch_or_create_state(
        conn,
        clock,
        &event.assessment,
        AggregateType::AssessmentAggregate,
        Some(rolled_back.rolled_back_to),
    )
    .await?;
    let restored = previous.for_write(clock).data.clone();

    let aggregate = state.for_write(clock);
    aggregate.data = restored;
    aggregate.data.assessment_mut().collaborators.insert(event.user.uuid);
    aggregate.applied(event.created_at, clock);

    Ok(Some(timeline(
        event,
        json!({ "rolledBackTo": rolled_back.rolled_back_to }),
    )))
}

/// Apply any event that doesn't need to read other state.
pub fn apply(event: &EventEntity, state: &mut AggregateState, clock: &Clock) -> Result<Option<TimelineDraft>> {
    let aggregate = state.for_write(clock);
    let aggregate_uuid = aggregate.uuid;
    let data = aggregate.data.assessment_mut();
    let user = event.user.uuid;

    let timeline_data = match &event.data {
        Event::AssessmentCreatedEvent(created) => {
            data.properties.extend(created.properties.clone());
            data.form_version = created.form_version.clone();
            data.collaborators.insert(user);
            data.flags.extend(created.flags.iter().cloned());
            json!({
                "formVersion": created.form_version,
                "properties": created.properties.keys().collect::<Vec<_>>(),
            })
        }
        Event::AssessmentAnswersUpdatedEvent(updated) => {
            data.answers.extend(updated.added.clone());
            for field_code in &updated.removed {
                if data.answers.shift_remove(field_code).is_none() {
                    return Err(Error::answer_not_found(field_code, aggregate_uuid));
                }
            }
            data.collaborators.insert(user);
            json!({
                "added": updated.added.keys().collect::<Vec<_>>(),
                "removed": updated.removed,
            })
        }
        Event::AssessmentPropertiesUpdatedEvent(updated) => {
            data.properties.extend(updated.added.clone());
            for property_name in &updated.removed {
                if data.properties.shift_remove(property_name).is_none() {
                    return Err(Error::property_not_found(property_name, aggregate_uuid));
                }
            }
            data.collaborators.insert(user);
            json!({
                "added": updated.added.keys().collect::<Vec<_>>(),
                "removed": updated.removed,
            })
        }
        Event::AssessmentFlagsUpdatedEvent(updated) => {
            data.flags.clone_from(&updated.flags);
            data.collaborators.insert(user);
            json!({ "flags": updated.flags })
        }
        Event::AssignedToUserEvent(assigned) => {
            data.assigned_user = Some(assigned.user_uuid);
            json!({ "assignee": event.user })
        }
        Event::FormVersionUpdatedEvent(updated) => {
            data.form_version.clone_from(&updated.version);
            data.collaborators.insert(user);
            json!({ "version": updated.version })
        }
        Event::CollectionCreatedEvent(created) => {
            let collection = Collection {
                uuid: created.collection_uuid,
                created_at: event.created_at,
                updated_at: event.created_at,
                name: created.name.clone(),
                items: Vec::new(),
            };
            let collections = match created.parent_collection_item_uuid {
                Some(parent) => {
                    &mut data
                        .get_collection_item_mut(parent)
                        .ok_or_else(|| Error::collection_item_not_found(parent, aggregate_uuid))?
                        .collections
                }
                None => &mut data.collections,
            };
            collections.push(collection);
            data.collaborators.insert(user);
            json!({
                "collection": created.name,
                "collectionUuid": created.collection_uuid,
            })
        }
        Event::CollectionItemAddedEvent(added) => {
            let item = CollectionItem {
                uuid: added.collection_item_uuid,
                created_at: event.created_at,
                updated_at: event.created_at,
                answers: added.answers.clone(),
                properties: added.properties.clone(),
                collections: Vec::new(),
            };
            let collection = data
                .get_collection_mut(added.collection_uuid)
                .ok_or_else(|| Error::collection_not_found(added.collection_uuid, aggregate_uuid))?;

            match added.index {
                Some(index) if index < 0 => {
                    return Err(Error::collection_index_out_of_bounds(index, collection.items.len()));
                }
                Some(index) if (index as usize) <= collection.items.len() => {
                    collection.items.insert(index as usize, item);
                }
                _ => collection.items.push(item),
            }

            let timeline_data = json!({
                "index": added.index.unwrap_or(collection.items.len() as i32 - 1),
                "collection": collection.name,
                "collectionItemUuid": added.collection_item_uuid,
                "collectionUuid": added.collection_uuid,
            });
            data.collaborators.insert(user);
            timeline_data
        }
        Event::CollectionItemAnswersUpdatedEvent(updated) => {
            let collection = data
                .get_collection_with_item_mut(updated.collection_item_uuid)
                .ok_or_else(|| Error::collection_item_not_found(updated.collection_item_uuid, aggregate_uuid))?;
            let index = collection.index_of(updated.collection_item_uuid);
            let name = collection.name.clone();
            let item = collection
                .find_item_mut(updated.collection_item_uuid)
                .ok_or_else(|| Error::collection_item_not_found(updated.collection_item_uuid, aggregate_uuid))?;

            item.updated_at = event.created_at;
            item.answers.extend(updated.added.clone());
            for field_code in &updated.removed {
                item.answers.shift_remove(field_code);
            }
            data.collaborators.insert(user);
            json!({
                "collection": name,
                "index": index,
                "added": updated.added.keys().collect::<Vec<_>>(),
                "removed": updated.removed,
            })
        }
        Event::CollectionItemPropertiesUpdatedEvent(updated) => {
            let collection = data
                .get_collection_with_item_mut(updated.collection_item_uuid)
                .ok_or_else(|| Error::collection_item_not_found(updated.collection_item_uuid, aggregate_uuid))?;
            let index = collection.index_of(updated.collection_item_uuid);
            let name = collection.name.clone();
            let item = collection
                .find_item_mut(updated.collection_item_uuid)
                .ok_or_else(|| Error::collection_item_not_found(updated.collection_item_uuid, aggregate_uuid))?;

            item.updated_at = event.created_at;
            item.properties.extend(updated.added.clone());
            for property_name in &updated.removed {
                item.properties.shift_remove(property_name);
            }
            data.collaborators.insert(user);
            json!({
                "collection": name,
                "index": index,
                "added": updated.added.keys().collect::<Vec<_>>(),
                "removed": updated.removed,
            })
        }
        Event::CollectionItemRemovedEvent(removed) => {
            let collection = data
                .get_collection_with_item_mut(removed.collection_item_uuid)
                .ok_or_else(|| Error::collection_item_not_found(removed.collection_item_uuid, aggregate_uuid))?;
            let timeline_data = json!({
                "collection": collection.name,
                "index": collection.index_of(removed.collection_item_uuid),
            });
            collection.remove_item(removed.collection_item_uuid);
            data.collaborators.insert(user);
            timeline_data
        }
        Event::CollectionItemReorderedEvent(reordered) => {
            let collection = data
                .get_collection_with_item_mut(reordered.collection_item_uuid)
                .ok_or_else(|| Error::collection_item_not_found(reordered.collection_item_uuid, aggregate_uuid))?;
            let timeline_data = json!({
                "collection": collection.name,
                "collectionItemUuid": reordered.collection_item_uuid,
                "index": reordered.index,
                "previousIndex": collection.index_of(reordered.collection_item_uuid),
            });
            collection.reorder_item(reordered.collection_item_uuid, reordered.index);
            data.collaborators.insert(user);
            timeline_data
        }
        Event::AssessmentRolledBackEvent(_) | Event::RedactedEvent(_) => {
            return Err(Error::Internal(format!(
                "{} cannot be applied without reading other state",
                event.data.name()
            )));
        }
    };

    aggregate.applied(event.created_at, clock);
    Ok(Some(timeline(event, timeline_data)))
}

fn timeline(event: &EventEntity, data: serde_json::Value) -> TimelineDraft {
    let serde_json::Value::Object(data) = data else {
        unreachable!("timeline data is always built as an object")
    };
    TimelineDraft::new(event, data as JsonMap)
}

#[cfg(test)]
mod tests {
    use std::sync::Arc;

    use super::*;
    use crate::{
        aggregate::{Aggregate, AggregateEntity, SNAPSHOT_THRESHOLD},
        datetime::LocalDateTime,
        event::*,
        model::{
            Value,
            collection::tests::{collection, item},
        },
        persistence::entity::{AssessmentEntity, AuthSource, UserDetailsEntity},
    };

    fn at(value: &str) -> LocalDateTime {
        value.parse().unwrap()
    }

    fn assessment() -> Arc<AssessmentEntity> {
        Arc::new(AssessmentEntity {
            id: None,
            uuid: Uuid::new_v4(),
            created_at: at("2025-01-01T09:00:00"),
            assessment_type: "TEST".to_owned(),
            identifiers: Vec::new(),
        })
    }

    fn user() -> UserDetailsEntity {
        UserDetailsEntity {
            id: Some(1),
            uuid: Uuid::new_v4(),
            user_id: "user".to_owned(),
            display_name: "A User".to_owned(),
            auth_source: AuthSource::HmppsAuth,
        }
    }

    fn state(data: AssessmentAggregate) -> AggregateState {
        let created = at("2025-01-01T09:00:00");
        AggregateState::new(AggregateEntity::new(
            Uuid::new_v4(),
            Aggregate::AssessmentAggregate(data),
            created,
            created,
            created,
        ))
    }

    fn event(data: Event) -> EventEntity {
        EventEntity::new(user(), assessment(), data, at("2025-01-02T10:00:00"), false)
    }

    fn data(state: &AggregateState) -> &AssessmentAggregate {
        state.latest().data.assessment()
    }

    #[test]
    fn assessment_created_sets_initial_state() {
        let mut state = state(AssessmentAggregate::default());
        let event = event(Event::AssessmentCreatedEvent(AssessmentCreatedEvent {
            form_version: "1.0".to_owned(),
            properties: Answers::from([("status".to_owned(), Value::single("NEW"))]),
            flags: vec!["FLAG".to_owned()],
        }));

        let timeline = apply(&event, &mut state, &Clock::system()).unwrap().unwrap();

        assert_eq!(data(&state).form_version, "1.0");
        assert_eq!(data(&state).properties["status"], Value::single("NEW"));
        assert_eq!(data(&state).flags, vec!["FLAG"]);
        assert!(data(&state).collaborators.contains(&event.user.uuid));
        assert_eq!(state.latest().events_applied, 1);
        assert_eq!(state.latest().events_to, event.created_at);
        assert_eq!(timeline.event_type, "AssessmentCreatedEvent");
        assert_eq!(
            serde_json::Value::Object(timeline.data),
            json!({"formVersion": "1.0", "properties": ["status"]})
        );
    }

    #[test]
    fn removing_an_unknown_answer_fails() {
        let mut state = state(AssessmentAggregate::default());
        let event = event(Event::AssessmentAnswersUpdatedEvent(AssessmentAnswersUpdatedEvent {
            added: Answers::new(),
            removed: vec!["missing".to_owned()],
        }));

        let error = apply(&event, &mut state, &Clock::system()).unwrap_err();

        assert!(
            error
                .developer_message()
                .contains("The answer \"missing\" could not be found")
        );
    }

    #[test]
    fn answers_are_added_and_removed() {
        let mut aggregate = AssessmentAggregate::default();
        aggregate.answers.insert("old".to_owned(), Value::single("x"));
        let mut state = state(aggregate);
        let event = event(Event::AssessmentAnswersUpdatedEvent(AssessmentAnswersUpdatedEvent {
            added: Answers::from([("new".to_owned(), Value::multi(["a", "b"]))]),
            removed: vec!["old".to_owned()],
        }));

        apply(&event, &mut state, &Clock::system()).unwrap();

        assert_eq!(
            data(&state).answers,
            Answers::from([("new".to_owned(), Value::multi(["a", "b"]))])
        );
    }

    #[test]
    fn a_full_snapshot_starts_a_new_one() {
        let mut state = state(AssessmentAggregate::default());
        state.aggregates[0].events_applied = SNAPSHOT_THRESHOLD;
        let event = event(Event::FormVersionUpdatedEvent(FormVersionUpdatedEvent {
            version: "2.0".to_owned(),
        }));

        apply(&event, &mut state, &Clock::system()).unwrap();

        assert_eq!(state.aggregates.len(), 2);
        assert_eq!(state.aggregates[0].data.assessment().form_version, "");
        assert_eq!(data(&state).form_version, "2.0");
        assert_eq!(state.latest().events_applied, 1);
        assert_eq!(state.latest().events_from, state.aggregates[0].events_from);
    }

    #[test]
    fn collection_items_are_added_at_an_index_into_nested_collections() {
        let (goals, goal, notes) = (Uuid::new_v4(), Uuid::new_v4(), Uuid::new_v4());
        let mut aggregate = AssessmentAggregate::default();
        aggregate.collections.push(collection(
            goals,
            "GOALS",
            vec![item(
                goal,
                vec![collection(notes, "NOTES", vec![item(Uuid::new_v4(), vec![])])],
            )],
        ));
        let mut state = state(aggregate);
        let new_note = Uuid::new_v4();
        let event = event(Event::CollectionItemAddedEvent(CollectionItemAddedEvent {
            collection_item_uuid: new_note,
            collection_uuid: notes,
            answers: Answers::new(),
            properties: Answers::new(),
            index: Some(0),
        }));

        let timeline = apply(&event, &mut state, &Clock::system()).unwrap().unwrap();

        let notes = data(&state).get_collection(notes).unwrap();
        assert_eq!(notes.items.len(), 2);
        assert_eq!(notes.items[0].uuid, new_note);
        assert_eq!(timeline.data["index"], json!(0));
        assert_eq!(timeline.data["collection"], json!("NOTES"));
    }

    #[test]
    fn collection_item_updates_record_the_containing_collection() {
        let (goal, step) = (Uuid::new_v4(), Uuid::new_v4());
        let mut aggregate = AssessmentAggregate::default();
        aggregate.collections.push(collection(
            Uuid::new_v4(),
            "GOALS",
            vec![item(
                goal,
                vec![collection(
                    Uuid::new_v4(),
                    "STEPS",
                    vec![item(Uuid::new_v4(), vec![]), item(step, vec![])],
                )],
            )],
        ));
        let mut state = state(aggregate);
        let event = event(Event::CollectionItemPropertiesUpdatedEvent(
            CollectionItemPropertiesUpdatedEvent {
                collection_item_uuid: step,
                added: Answers::from([("status".to_owned(), Value::single("DONE"))]),
                removed: vec![],
            },
        ));

        let timeline = apply(&event, &mut state, &Clock::system()).unwrap().unwrap();

        let step_item = data(&state).get_collection_item(step).unwrap();
        assert_eq!(step_item.properties["status"], Value::single("DONE"));
        assert_eq!(step_item.updated_at, event.created_at);
        assert_eq!(timeline.data["collection"], json!("STEPS"));
        assert_eq!(timeline.data["index"], json!(1));
    }

    #[test]
    fn unknown_collection_items_fail() {
        let mut state = state(AssessmentAggregate::default());
        let event = event(Event::CollectionItemRemovedEvent(CollectionItemRemovedEvent {
            collection_item_uuid: Uuid::new_v4(),
        }));

        assert!(apply(&event, &mut state, &Clock::system()).is_err());
    }

    #[test]
    fn assigning_a_user_records_them_on_the_timeline() {
        let mut state = state(AssessmentAggregate::default());
        let event = event(Event::AssignedToUserEvent(AssignedToUserEvent {
            user_uuid: Uuid::new_v4(),
        }));

        let timeline = apply(&event, &mut state, &Clock::system()).unwrap().unwrap();

        assert_eq!(
            data(&state).assigned_user,
            Some(match &event.data {
                Event::AssignedToUserEvent(assigned) => assigned.user_uuid,
                _ => unreachable!(),
            })
        );
        assert!(data(&state).collaborators.is_empty());
        assert_eq!(timeline.data["assignee"]["displayName"], json!("A User"));
    }
}
