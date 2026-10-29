use std::sync::Arc;

use sqlx::PgConnection;
use uuid::Uuid;

use super::*;
use crate::{
    AppState,
    aggregate::AggregateType,
    clock::Clock,
    error::{Error, Result},
    model::{Collection, CollectionItem, TimelineItem, User},
    persistence::{
        entity::AssessmentEntity,
        repository::{event, timeline, timeline::TimelineCriteria, user_details},
    },
    service::{assessment, state},
};

pub async fn handle(query: &Query, app: &AppState, conn: &mut PgConnection, clock: &Clock) -> Result<QueryResult> {
    Ok(match query {
        Query::AssessmentVersionQuery(query) => {
            let assessment = assessment::find_by(conn, &query.assessment_identifier, clock.now()).await?;
            let result = assessment_version(conn, clock, &assessment, query.timestamp).await?;
            if query.timestamp.is_none() {
                app.cache.cache_latest(&result);
            }
            QueryResult::AssessmentVersionQueryResult(Box::new(result))
        }
        Query::CollectionQuery(query) => {
            let assessment = assessment::find_by(conn, &query.assessment_identifier, clock.now()).await?;
            let state = assessment_state(conn, clock, &assessment, query.timestamp).await?;
            let aggregate = state.latest();
            let collection = aggregate
                .data
                .assessment()
                .get_collection(query.collection_uuid)
                .ok_or_else(|| Error::collection_not_found(query.collection_uuid, aggregate.uuid))?;

            let collection = match query.depth {
                -1 => collection.clone(),
                depth if depth >= 0 => truncate_collection(collection, depth, 0),
                depth => return Err(Error::collection_depth_out_of_bounds(depth, query.collection_uuid)),
            };
            QueryResult::CollectionQueryResult(CollectionQueryResult { collection })
        }
        Query::CollectionItemQuery(query) => {
            let assessment = assessment::find_by(conn, &query.assessment_identifier, clock.now()).await?;
            let state = assessment_state(conn, clock, &assessment, query.timestamp).await?;
            let aggregate = state.latest();
            let item = aggregate
                .data
                .assessment()
                .get_collection_item(query.collection_item_uuid)
                .ok_or_else(|| Error::collection_item_not_found(query.collection_item_uuid, aggregate.uuid))?;

            let collection_item = match query.depth {
                -1 => item.clone(),
                depth if depth >= 0 => truncate_collection_item(item, depth, 0),
                depth => return Err(Error::collection_depth_out_of_bounds(depth, query.collection_item_uuid)),
            };
            QueryResult::CollectionItemQueryResult(CollectionItemQueryResult { collection_item })
        }
        Query::DailyVersionsQuery(query) => {
            let assessment = assessment::find_by(conn, &query.assessment_identifier, clock.now()).await?;
            QueryResult::DailyVersionsQueryResult(DailyVersionsQueryResult {
                versions: timeline::find_daily_versions(conn, assessment.uuid).await?,
            })
        }
        Query::TimelineQuery(query) => QueryResult::TimelineQueryResult(timeline_page(conn, clock, query).await?),
        Query::GetAssessmentsModifiedSinceQuery(query) => {
            validate_max_lookback(app, clock, query.since)?;
            if query.limit < 1 {
                return Err(Error::invalid_query("The 'limit' parameter must be at least 1"));
            }

            let uuids = event::find_assessment_uuids_modified_since(
                conn,
                &query.assessment_type,
                query.since,
                query.after,
                query.limit.into(),
            )
            .await?;

            let mut assessments = Vec::with_capacity(uuids.len());
            for uuid in &uuids {
                let assessment = Arc::new(assessment::find_by_uuid(conn, *uuid).await?);
                assessments.push(assessment_version(conn, clock, &assessment, None).await?);
            }

            QueryResult::GetAssessmentsModifiedSinceQueryResult(GetAssessmentsModifiedSinceQueryResult {
                assessments,
                next_cursor: (uuids.len() == query.limit as usize)
                    .then(|| uuids.last().copied())
                    .flatten(),
            })
        }
        Query::GetAssessmentsSoftDeletedSinceQuery(query) => {
            validate_max_lookback(app, clock, query.since)?;
            QueryResult::GetAssessmentsSoftDeletedSinceQueryResult(GetAssessmentsSoftDeletedSinceQueryResult {
                assessments: event::find_assessment_uuids_soft_deleted_since(conn, &query.assessment_type, query.since)
                    .await?,
            })
        }
    })
}

pub async fn subject_access_request(
    conn: &mut PgConnection,
    clock: &Clock,
    query: &SubjectAccessRequestQuery,
) -> Result<SubjectAccessRequestQueryResult> {
    let uuids = crate::persistence::repository::assessment::find_uuids_by_external_identifiers(
        conn,
        &query.assessment_identifiers,
        query.from.map(LocalDateTime::start_of_day),
        query.to.map(|to| LocalDateTime::end_of_day(to).to_database_precision()),
    )
    .await?;

    if uuids.is_empty() {
        return Err(Error::subject_access_request_no_assessments(
            &query.assessment_identifiers,
        ));
    }

    let point_in_time = query
        .to
        .map(LocalDateTime::end_of_day)
        .or(query.timestamp)
        .unwrap_or_else(|| clock.now());

    let mut results = Vec::with_capacity(uuids.len());
    for uuid in uuids {
        let assessment = Arc::new(assessment::find_by_uuid(conn, uuid).await?);
        let state = assessment_state(conn, clock, &assessment, Some(point_in_time)).await?;
        results.push(SubjectAccessRequestAssessmentVersion::from_aggregate(
            state.latest(),
            &assessment,
        ));
    }

    Ok(SubjectAccessRequestQueryResult { results })
}

async fn assessment_state(
    conn: &mut PgConnection,
    clock: &Clock,
    assessment: &Arc<AssessmentEntity>,
    timestamp: Option<LocalDateTime>,
) -> Result<crate::aggregate::AggregateState> {
    state::fetch_or_create_state(conn, clock, assessment, AggregateType::AssessmentAggregate, timestamp).await
}

async fn assessment_version(
    conn: &mut PgConnection,
    clock: &Clock,
    assessment: &Arc<AssessmentEntity>,
    timestamp: Option<LocalDateTime>,
) -> Result<AssessmentVersionQueryResult> {
    let state = assessment_state(conn, clock, assessment, timestamp).await?;
    let aggregate = state.latest();
    let data = aggregate.data.assessment();

    let collaborator_uuids: Vec<Uuid> = data.collaborators.iter().copied().collect();
    let collaborators: Vec<User> = user_details::find_all_by_uuids(conn, &collaborator_uuids)
        .await?
        .iter()
        .map(|user| User::from_entity(Some(user)))
        .collect();

    let assigned_user = match data.assigned_user {
        None => None,
        Some(uuid) => Some(
            match collaborators.iter().find(|collaborator| collaborator.id == uuid) {
                Some(collaborator) => collaborator.clone(),
                None => User::from_entity(user_details::find_by_uuid(conn, uuid).await?.as_ref()),
            },
        ),
    };

    Ok(AssessmentVersionQueryResult {
        assessment_uuid: assessment.uuid,
        aggregate_uuid: aggregate.uuid,
        assessment_type: assessment.assessment_type.clone(),
        form_version: data.form_version.clone(),
        created_at: aggregate.events_from,
        updated_at: aggregate.events_to,
        answers: data.answers.clone(),
        properties: data.properties.clone(),
        collections: data.collections.clone(),
        collaborators,
        identifiers: assessment.identifiers_map(),
        assigned_user,
        flags: data.flags.clone(),
    })
}

async fn timeline_page(conn: &mut PgConnection, clock: &Clock, query: &TimelineQuery) -> Result<TimelineQueryResult> {
    if query.page_number < 0 {
        return Err(Error::invalid_query("Page index must not be less than zero"));
    }
    if query.page_size < 1 {
        return Err(Error::invalid_query("Page size must not be less than one"));
    }

    let assessment_uuid = match &query.assessment_identifier {
        Some(identifier) => Some(assessment::find_by(conn, identifier, clock.now()).await?.uuid),
        None => None,
    };
    let user_uuid = match &query.subject {
        Some(subject) => Some(
            user_details::find_by_user_id_and_auth_source(conn, &subject.id, subject.auth_source)
                .await?
                .ok_or_else(Error::user_details_not_found)?
                .uuid,
        ),
        None => None,
    };

    let criteria = TimelineCriteria {
        assessment_uuid,
        user_uuid,
        from: query.from,
        to: query.to,
        include_event_types: query.include_event_types.clone().unwrap_or_default(),
        exclude_event_types: query.exclude_event_types.clone().unwrap_or_default(),
        include_custom_types: query.include_custom_types.clone().unwrap_or_default(),
        exclude_custom_types: query.exclude_custom_types.clone().unwrap_or_default(),
    };
    let page_size = i64::from(query.page_size);
    let (entries, total) = timeline::find_page(conn, &criteria, query.page_number.into(), page_size).await?;

    Ok(TimelineQueryResult {
        timeline: entries.iter().map(TimelineItem::from_entity).collect::<Result<_>>()?,
        page_info: PageInfo {
            page_number: query.page_number,
            total_pages: ((total + page_size - 1) / page_size) as i32,
        },
    })
}

fn validate_max_lookback(app: &AppState, clock: &Clock, since: LocalDateTime) -> Result<()> {
    let max_days = app.config.query_max_lookback_days;
    if max_days > 0 && since < clock.now().minus_days(max_days) {
        return Err(Error::invalid_query(format!(
            "The 'since' parameter cannot be older than {max_days} day(s)"
        )));
    }
    Ok(())
}

/// The collection with items nested `depth` collections deep, and no deeper.
fn truncate_collection(collection: &Collection, depth: i32, current_depth: i32) -> Collection {
    Collection {
        items: collection
            .items
            .iter()
            .map(|item| CollectionItem {
                collections: if current_depth < depth {
                    item.collections
                        .iter()
                        .map(|nested| truncate_collection(nested, depth, current_depth + 1))
                        .collect()
                } else {
                    Vec::new()
                },
                ..item.clone()
            })
            .collect(),
        ..collection.clone()
    }
}

/// The item with collections nested `depth` items deep, and no deeper.
fn truncate_collection_item(item: &CollectionItem, depth: i32, current_depth: i32) -> CollectionItem {
    CollectionItem {
        collections: item
            .collections
            .iter()
            .map(|collection| Collection {
                items: if current_depth < depth {
                    collection
                        .items
                        .iter()
                        .map(|nested| truncate_collection_item(nested, depth, current_depth + 1))
                        .collect()
                } else {
                    Vec::new()
                },
                ..collection.clone()
            })
            .collect(),
        ..item.clone()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::model::collection::tests::{collection, item};

    fn nested() -> Collection {
        collection(
            Uuid::new_v4(),
            "GOALS",
            vec![item(
                Uuid::new_v4(),
                vec![collection(
                    Uuid::new_v4(),
                    "STEPS",
                    vec![item(
                        Uuid::new_v4(),
                        vec![collection(Uuid::new_v4(), "NOTES", vec![item(Uuid::new_v4(), vec![])])],
                    )],
                )],
            )],
        )
    }

    #[test]
    fn truncates_collections_to_the_requested_depth() {
        let goals = nested();

        let depth_0 = truncate_collection(&goals, 0, 0);
        assert!(depth_0.items[0].collections.is_empty());

        let depth_1 = truncate_collection(&goals, 1, 0);
        let steps = &depth_1.items[0].collections[0];
        assert_eq!(steps.name, "STEPS");
        assert!(steps.items[0].collections.is_empty());
    }

    #[test]
    fn truncates_collection_items_to_the_requested_depth() {
        let goal = nested().items[0].clone();

        let depth_0 = truncate_collection_item(&goal, 0, 0);
        assert_eq!(depth_0.collections[0].name, "STEPS");
        assert!(depth_0.collections[0].items.is_empty());

        let depth_1 = truncate_collection_item(&goal, 1, 0);
        assert_eq!(depth_1.collections[0].items[0].collections[0].name, "NOTES");
        assert!(depth_1.collections[0].items[0].collections[0].items.is_empty());
    }
}
