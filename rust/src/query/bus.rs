use sqlx::PgConnection;

use super::{
    QueriesResponse, Query, QueryResponse, QueryResult, SubjectAccessRequestQuery, SubjectAccessRequestQueryResult,
    handler,
};
use crate::{
    AppState,
    clock::Clock,
    error::{Error, Result},
};

/// Run a batch of queries in one read-only transaction, so they see a consistent snapshot.
pub async fn dispatch_all(app: &AppState, clock: &Clock, queries: Vec<Query>) -> Result<QueriesResponse> {
    if queries.is_empty() {
        return Err(Error::invalid_query("No queries received"));
    }

    let mut tx = app.pool.begin().await?;
    sqlx::query("SET TRANSACTION READ ONLY").execute(&mut *tx).await?;

    let mut responses = Vec::with_capacity(queries.len());
    for query in queries {
        let result = dispatch(app, &mut tx, clock, &query).await?;
        responses.push(QueryResponse { request: query, result });
    }

    tx.commit().await?;
    Ok(QueriesResponse { queries: responses })
}

pub async fn dispatch(app: &AppState, conn: &mut PgConnection, clock: &Clock, query: &Query) -> Result<QueryResult> {
    let result = handler::handle(query, app, conn, clock).await?;
    app.audit.audit_query(query);
    Ok(result)
}

pub async fn dispatch_subject_access_request(
    app: &AppState,
    clock: &Clock,
    query: &SubjectAccessRequestQuery,
) -> Result<SubjectAccessRequestQueryResult> {
    let mut conn = app.pool.acquire().await?;
    let result = handler::subject_access_request(&mut conn, clock, query).await?;
    app.audit.audit_subject_access_request(query);
    Ok(result)
}
