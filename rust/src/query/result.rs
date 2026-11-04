use indexmap::IndexMap;
use serde::Serialize;
use utoipa::ToSchema;
use uuid::Uuid;

use super::Query;
use crate::{
    aggregate::AggregateEntity,
    datetime::LocalDateTime,
    model::{Answers, Collection, CollectionItem, DailyVersionDetails, TimelineItem, User, Value},
    persistence::entity::{AssessmentEntity, IdentifierType},
};

/// Every result struct writes its own `type` (so results nested in other results carry it too,
/// as they did from the Kotlin service), so the enum itself is untagged.
#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(untagged)]
pub enum QueryResult {
    TimelineQueryResult(TimelineQueryResult),
    AssessmentVersionQueryResult(Box<AssessmentVersionQueryResult>),
    CollectionItemQueryResult(CollectionItemQueryResult),
    CollectionQueryResult(CollectionQueryResult),
    DailyVersionsQueryResult(DailyVersionsQueryResult),
    GetAssessmentsModifiedSinceQueryResult(GetAssessmentsModifiedSinceQueryResult),
    GetAssessmentsSoftDeletedSinceQueryResult(GetAssessmentsSoftDeletedSinceQueryResult),
    SubjectAccessRequestQueryResult(SubjectAccessRequestQueryResult),
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(tag = "type", rename_all = "camelCase")]
pub struct AssessmentVersionQueryResult {
    pub assessment_uuid: Uuid,
    pub aggregate_uuid: Uuid,
    pub assessment_type: String,
    pub form_version: String,
    pub created_at: LocalDateTime,
    pub updated_at: LocalDateTime,
    pub answers: Answers,
    pub properties: Answers,
    pub collections: Vec<Collection>,
    pub collaborators: Vec<User>,
    pub identifiers: IndexMap<IdentifierType, String>,
    pub assigned_user: Option<User>,
    pub flags: Vec<String>,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(tag = "type", rename_all = "camelCase")]
pub struct CollectionItemQueryResult {
    pub collection_item: CollectionItem,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(tag = "type", rename_all = "camelCase")]
pub struct CollectionQueryResult {
    pub collection: Collection,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(tag = "type", rename_all = "camelCase")]
pub struct DailyVersionsQueryResult {
    pub versions: Vec<DailyVersionDetails>,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(tag = "type", rename_all = "camelCase")]
pub struct GetAssessmentsModifiedSinceQueryResult {
    pub assessments: Vec<AssessmentVersionQueryResult>,
    /// Pass as `after` to get the next page, when there may be one.
    pub next_cursor: Option<Uuid>,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(tag = "type", rename_all = "camelCase")]
pub struct GetAssessmentsSoftDeletedSinceQueryResult {
    pub assessments: Vec<Uuid>,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct PageInfo {
    pub page_number: i32,
    pub total_pages: i32,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(tag = "type", rename_all = "camelCase")]
pub struct TimelineQueryResult {
    pub timeline: Vec<TimelineItem>,
    pub page_info: PageInfo,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(tag = "type", rename_all = "camelCase")]
pub struct SubjectAccessRequestQueryResult {
    pub results: Vec<SubjectAccessRequestAssessmentVersion>,
}

/// An assessment flattened for the subject access request template.
#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct SubjectAccessRequestAssessmentVersion {
    pub assessment_type: String,
    pub created_at: LocalDateTime,
    pub updated_at: LocalDateTime,
    pub answers: Vec<RenderedValue>,
    pub properties: Vec<RenderedValue>,
    pub collections: Vec<RenderedCollection>,
    pub identifiers: IndexMap<IdentifierType, String>,
    pub has_answers_or_properties: bool,
}

impl SubjectAccessRequestAssessmentVersion {
    pub fn from_aggregate(aggregate: &AggregateEntity, assessment: &AssessmentEntity) -> Self {
        let data = aggregate.data.assessment();
        let answers = rendered_values(&data.answers);
        let properties = rendered_values(&data.properties);
        Self {
            assessment_type: assessment.assessment_type.clone(),
            created_at: aggregate.events_from,
            updated_at: aggregate.events_to,
            has_answers_or_properties: !answers.is_empty() || !properties.is_empty(),
            answers,
            properties,
            collections: data
                .collections
                .iter()
                .flat_map(|collection| flatten(collection, &[]))
                .collect(),
            identifiers: assessment.identifiers_map(),
        }
    }
}

#[derive(Clone, Debug, Serialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct RenderedValue {
    pub key: String,
    pub single: Option<String>,
    pub multiple: Option<Vec<String>>,
    pub has_single: bool,
    pub has_multiple: bool,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
pub struct RenderedCollectionItem {
    pub name: String,
    pub answers: Vec<RenderedValue>,
    pub properties: Vec<RenderedValue>,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
pub struct RenderedCollection {
    pub name: String,
    pub items: Vec<RenderedCollectionItem>,
}

fn rendered_values(values: &Answers) -> Vec<RenderedValue> {
    values
        .iter()
        .map(|(key, value)| match value {
            Value::Single { value } => RenderedValue {
                key: key.clone(),
                single: Some(value.clone()),
                multiple: None,
                has_single: true,
                has_multiple: false,
            },
            Value::Multi { values } => RenderedValue {
                key: key.clone(),
                single: None,
                multiple: Some(values.clone()),
                has_single: false,
                has_multiple: true,
            },
        })
        .collect()
}

/// The collection followed by every collection nested beneath it, each named by its path,
/// such as `GOALS/1/STEPS`.
fn flatten(collection: &Collection, path: &[String]) -> Vec<RenderedCollection> {
    let collection_path: Vec<String> = path.iter().cloned().chain([collection.name.clone()]).collect();
    let item_path = |index: usize| -> Vec<String> {
        collection_path
            .iter()
            .cloned()
            .chain([(index + 1).to_string()])
            .collect()
    };

    let current = RenderedCollection {
        name: collection_path.join("/"),
        items: collection
            .items
            .iter()
            .enumerate()
            .map(|(index, item)| RenderedCollectionItem {
                name: item_path(index).join("/"),
                answers: rendered_values(&item.answers),
                properties: rendered_values(&item.properties),
            })
            .collect(),
    };

    let nested = collection.items.iter().enumerate().flat_map(|(index, item)| {
        let path = item_path(index);
        item.collections
            .iter()
            .flat_map(move |child| flatten(child, &path))
            .collect::<Vec<_>>()
    });

    std::iter::once(current).chain(nested).collect()
}

#[derive(Clone, Debug, Serialize, ToSchema)]
pub struct QueryResponse {
    pub request: Query,
    pub result: QueryResult,
}

#[derive(Clone, Debug, Serialize, ToSchema)]
pub struct QueriesResponse {
    pub queries: Vec<QueryResponse>,
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::*;
    use crate::model::collection::tests::{collection, item};

    #[test]
    fn flattens_nested_collections_by_path() {
        let goals = collection(
            Uuid::new_v4(),
            "GOALS",
            vec![
                item(Uuid::new_v4(), vec![]),
                item(
                    Uuid::new_v4(),
                    vec![collection(Uuid::new_v4(), "STEPS", vec![item(Uuid::new_v4(), vec![])])],
                ),
            ],
        );

        let flattened = flatten(&goals, &[]);

        let names: Vec<_> = flattened.iter().map(|collection| collection.name.as_str()).collect();
        assert_eq!(names, vec!["GOALS", "GOALS/2/STEPS"]);
        assert_eq!(flattened[0].items[1].name, "GOALS/2");
        assert_eq!(flattened[1].items[0].name, "GOALS/2/STEPS/1");
        assert_eq!(
            serde_json::to_value(&flattened[1].items[0].answers).unwrap(),
            json!([{"key": "q", "single": "a", "multiple": null, "hasSingle": true, "hasMultiple": false}])
        );
    }

    #[test]
    fn results_carry_their_type_even_when_nested() {
        let result =
            QueryResult::GetAssessmentsSoftDeletedSinceQueryResult(GetAssessmentsSoftDeletedSinceQueryResult {
                assessments: vec![],
            });
        assert_eq!(
            serde_json::to_value(result).unwrap(),
            json!({"type": "GetAssessmentsSoftDeletedSinceQueryResult", "assessments": []})
        );
    }
}
