use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

use super::Answers;
use crate::datetime::LocalDateTime;

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct Collection {
    pub uuid: Uuid,
    pub created_at: LocalDateTime,
    pub updated_at: LocalDateTime,
    pub name: String,
    #[schema(no_recursion)]
    pub items: Vec<CollectionItem>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CollectionItem {
    pub uuid: Uuid,
    pub created_at: LocalDateTime,
    pub updated_at: LocalDateTime,
    pub answers: Answers,
    pub properties: Answers,
    #[schema(no_recursion)]
    pub collections: Vec<Collection>,
}

impl Collection {
    /// Find an item in this collection or any collection nested beneath it.
    pub fn find_item(&self, id: Uuid) -> Option<&CollectionItem> {
        self.items.iter().find(|item| item.uuid == id).or_else(|| {
            self.items
                .iter()
                .find_map(|item| item.collections.iter().find_map(|nested| nested.find_item(id)))
        })
    }

    pub fn find_item_mut(&mut self, id: Uuid) -> Option<&mut CollectionItem> {
        if let Some(index) = self.items.iter().position(|item| item.uuid == id) {
            return Some(&mut self.items[index]);
        }
        self.items
            .iter_mut()
            .find_map(|item| item.collections.iter_mut().find_map(|nested| nested.find_item_mut(id)))
    }

    /// Index of a direct item, or -1, like `List.indexOf`.
    pub fn index_of(&self, id: Uuid) -> i32 {
        self.items
            .iter()
            .position(|item| item.uuid == id)
            .map_or(-1, |index| index as i32)
    }

    pub fn remove_item(&mut self, id: Uuid) -> bool {
        let before = self.items.len();
        self.items.retain(|item| item.uuid != id);
        if self.items.len() != before {
            return true;
        }
        self.items
            .iter_mut()
            .any(|item| item.collections.iter_mut().any(|nested| nested.remove_item(id)))
    }

    /// Move an item to `target_index`, clamped to the bounds of its collection.
    pub fn reorder_item(&mut self, id: Uuid, target_index: i32) -> bool {
        if let Some(index) = self.items.iter().position(|item| item.uuid == id) {
            let last = self.items.len() as i32 - 1;
            let target = target_index.clamp(0, last) as usize;
            let item = self.items.remove(index);
            self.items.insert(target, item);
            return true;
        }
        self.items.iter_mut().any(|item| {
            item.collections
                .iter_mut()
                .any(|nested| nested.reorder_item(id, target_index))
        })
    }
}

impl CollectionItem {
    pub fn find_collection(&self, id: Uuid) -> Option<&Collection> {
        self.collections
            .iter()
            .find(|collection| collection.uuid == id)
            .or_else(|| {
                self.collections
                    .iter()
                    .find_map(|collection| collection.items.iter().find_map(|item| item.find_collection(id)))
            })
    }

    pub fn find_collection_mut(&mut self, id: Uuid) -> Option<&mut Collection> {
        if let Some(index) = self.collections.iter().position(|collection| collection.uuid == id) {
            return Some(&mut self.collections[index]);
        }
        self.collections.iter_mut().find_map(|collection| {
            collection
                .items
                .iter_mut()
                .find_map(|item| item.find_collection_mut(id))
        })
    }

    /// The collection, at any depth beneath this item, that directly contains the given item.
    pub fn find_collection_with_item_mut(&mut self, item_id: Uuid) -> Option<&mut Collection> {
        for collection in self.collections.iter_mut() {
            if collection.items.iter().any(|item| item.uuid == item_id) {
                return Some(collection);
            }
            if let Some(found) = collection
                .items
                .iter_mut()
                .find_map(|item| item.find_collection_with_item_mut(item_id))
            {
                return Some(found);
            }
        }
        None
    }
}

#[cfg(test)]
pub(crate) mod tests {
    use super::*;
    use crate::model::Value;

    pub fn timestamp() -> LocalDateTime {
        "2025-01-01T09:00:00".parse().unwrap()
    }

    pub fn item(uuid: Uuid, collections: Vec<Collection>) -> CollectionItem {
        CollectionItem {
            uuid,
            created_at: timestamp(),
            updated_at: timestamp(),
            answers: Answers::from([("q".to_owned(), Value::single("a"))]),
            properties: Answers::new(),
            collections,
        }
    }

    pub fn collection(uuid: Uuid, name: &str, items: Vec<CollectionItem>) -> Collection {
        Collection {
            uuid,
            created_at: timestamp(),
            updated_at: timestamp(),
            name: name.to_owned(),
            items,
        }
    }

    fn ids(collection: &Collection) -> Vec<Uuid> {
        collection.items.iter().map(|item| item.uuid).collect()
    }

    #[test]
    fn finds_nested_items_and_collections() {
        let (nested_item, nested_collection, outer_item) = (Uuid::new_v4(), Uuid::new_v4(), Uuid::new_v4());
        let goals = collection(
            Uuid::new_v4(),
            "GOALS",
            vec![item(
                outer_item,
                vec![collection(nested_collection, "NOTES", vec![item(nested_item, vec![])])],
            )],
        );

        assert_eq!(goals.find_item(nested_item).unwrap().uuid, nested_item);
        assert_eq!(goals.index_of(nested_item), -1);
        assert_eq!(goals.index_of(outer_item), 0);

        let mut outer = goals.items[0].clone();
        assert_eq!(outer.find_collection(nested_collection).unwrap().name, "NOTES");
        assert_eq!(
            outer.find_collection_with_item_mut(nested_item).unwrap().uuid,
            nested_collection
        );
        assert!(outer.find_collection_with_item_mut(outer_item).is_none());
    }

    #[test]
    fn removes_nested_items() {
        let nested_item = Uuid::new_v4();
        let mut goals = collection(
            Uuid::new_v4(),
            "GOALS",
            vec![item(
                Uuid::new_v4(),
                vec![collection(Uuid::new_v4(), "NOTES", vec![item(nested_item, vec![])])],
            )],
        );

        assert!(goals.remove_item(nested_item));
        assert!(goals.find_item(nested_item).is_none());
        assert!(!goals.remove_item(nested_item));
    }

    #[test]
    fn reorders_items_clamping_the_target_index() {
        let (a, b, c) = (Uuid::new_v4(), Uuid::new_v4(), Uuid::new_v4());
        let mut list = collection(
            Uuid::new_v4(),
            "LIST",
            vec![item(a, vec![]), item(b, vec![]), item(c, vec![])],
        );

        assert!(list.reorder_item(a, 1));
        assert_eq!(ids(&list), vec![b, a, c]);
        assert!(list.reorder_item(c, -5));
        assert_eq!(ids(&list), vec![c, b, a]);
        assert!(list.reorder_item(c, 99));
        assert_eq!(ids(&list), vec![b, a, c]);
        assert!(!list.reorder_item(Uuid::new_v4(), 0));
    }
}
