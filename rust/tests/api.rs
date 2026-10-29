mod common;

use axum::http::{Method, StatusCode};
use common::*;
use hmpps_arns_assessment_platform_api::{clock::Clock, datetime::LocalDateTime, db::MIGRATOR};
use serde_json::{Value, json};
use sqlx::PgPool;

#[sqlx::test(migrator = "MIGRATOR")]
async fn health_and_unknown_routes(pool: PgPool) {
    let app = TestApp::new(pool).await;

    let (status, body) = app.request(Method::GET, "/health/ping", &[], None).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(body, json!({"status": "UP"}));

    let (status, body) = app.request(Method::GET, "/no-such-thing", &[], None).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    assert_eq!(body["status"], 404);
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn requires_a_valid_token_with_an_allowed_role(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let body = Some(json!({"queries": []}));

    let (status, _) = app.request(Method::POST, "/query", &[], body.clone()).await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);

    let (status, error) = app.request(Method::POST, "/query", &[SAR], body.clone()).await;
    assert_eq!(status, StatusCode::FORBIDDEN);
    assert_eq!(error["userMessage"], "Forbidden: Access Denied");

    let (status, error) = app.request(Method::POST, "/query", &[VIEWER], body).await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    assert_eq!(error["developerMessage"], "No queries received");

    let (status, _) = app
        .request(Method::POST, "/command", &[VIEWER], Some(json!({"commands": []})))
        .await;
    assert_eq!(status, StatusCode::FORBIDDEN);
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn invalid_payloads_are_bad_requests(pool: PgPool) {
    let app = TestApp::new(pool).await;

    let (status, error) = app
        .request(
            Method::POST,
            "/command",
            &[FRONTEND],
            Some(json!({"commands": [{"type": "NotACommand", "user": user()}]})),
        )
        .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    assert!(error["userMessage"].as_str().unwrap().starts_with("Invalid payload"));
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn builds_an_assessment_from_commands_using_placeholders(pool: PgPool) {
    let app = TestApp::new(pool).await;

    let response = app
        .commands(json!([
            {
                "type": "CreateAssessmentCommand",
                "user": user(),
                "formVersion": "1.0",
                "assessmentType": "SENTENCE_PLAN",
                "identifiers": {"CRN": "X123456"},
                "flags": ["SAN_BETA"],
                "timeline": {"type": "CREATED", "data": {"source": "test"}},
            },
            {"type": "CreateCollectionCommand", "user": user(), "assessmentUuid": "@0", "name": "GOALS"},
            {
                "type": "AddCollectionItemCommand",
                "user": user(),
                "assessmentUuid": "@0",
                "collectionUuid": "@1",
                "answers": {"title": single("Find a job")},
                "properties": {"status": single("ACTIVE")},
                "index": null,
            },
            {"type": "CreateCollectionCommand", "user": user(), "assessmentUuid": "@0", "name": "STEPS", "parentCollectionItemUuid": "@2"},
            {
                "type": "UpdateAssessmentAnswersCommand",
                "user": user(),
                "assessmentUuid": "@0",
                "added": {"q1": single("YES"), "q2": {"type": "Multi", "values": ["A", "B"]}},
                "removed": [],
            },
        ]))
        .await;

    let results: Vec<&Value> = response["commands"]
        .as_array()
        .unwrap()
        .iter()
        .map(|c| &c["result"])
        .collect();
    assert_eq!(results[0]["type"], "CreateAssessmentCommandResult");
    assert_eq!(results[1]["type"], "CreateCollectionCommandResult");
    assert_eq!(results[2]["type"], "AddCollectionItemCommandResult");
    assert_eq!(
        *results[4],
        json!({"type": "CommandSuccessCommandResult", "message": "Done", "success": true})
    );
    assert_eq!(
        response["commands"][1]["request"]["assessmentUuid"], "@0",
        "requests are echoed as sent"
    );

    let assessment = results[0]["assessmentUuid"].as_str().unwrap();
    let goal = results[2]["collectionItemUuid"].as_str().unwrap();

    let version = app.version(assessment).await;
    assert_eq!(version["type"], "AssessmentVersionQueryResult");
    assert_eq!(version["assessmentType"], "SENTENCE_PLAN");
    assert_eq!(version["formVersion"], "1.0");
    assert_eq!(version["identifiers"], json!({"CRN": "X123456"}));
    assert_eq!(version["flags"], json!(["SAN_BETA"]));
    assert_eq!(version["answers"]["q2"], json!({"type": "Multi", "values": ["A", "B"]}));
    assert_eq!(version["collections"][0]["name"], "GOALS");
    assert_eq!(version["collections"][0]["items"][0]["uuid"], goal);
    assert_eq!(version["collections"][0]["items"][0]["collections"][0]["name"], "STEPS");
    assert_eq!(version["collaborators"][0]["userId"], "test-user");
    assert_eq!(version["assignedUser"]["name"], "Test User");

    let by_crn = app
        .query(json!({
            "type": "AssessmentVersionQuery",
            "user": user(),
            "assessmentIdentifier": {"type": "EXTERNAL", "identifier": "X123456", "identifierType": "CRN", "assessmentType": "SENTENCE_PLAN"},
        }))
        .await;
    assert_eq!(by_crn["assessmentUuid"], assessment);

    let item = app
        .query(json!({
            "type": "CollectionItemQuery",
            "user": user(),
            "assessmentIdentifier": {"type": "UUID", "uuid": assessment},
            "collectionItemUuid": goal,
            "depth": 0,
        }))
        .await;
    assert_eq!(item["collectionItem"]["answers"]["title"], single("Find a job"));
    assert_eq!(item["collectionItem"]["collections"][0]["items"], json!([]));

    let timeline = app
        .query(json!({
            "type": "TimelineQuery",
            "user": user(),
            "assessmentIdentifier": {"type": "UUID", "uuid": assessment},
            "pageSize": 3,
        }))
        .await;
    assert_eq!(timeline["pageInfo"], json!({"pageNumber": 0, "totalPages": 2}));
    let entries = timeline["timeline"].as_array().unwrap();
    assert_eq!(entries[0]["event"], "AssessmentAnswersUpdatedEvent");
    assert_eq!(entries[0]["data"], json!({"added": ["q1", "q2"], "removed": []}));
    assert_eq!(entries[0]["position"], 5);

    let created = app
        .query(json!({
            "type": "TimelineQuery",
            "user": user(),
            "assessmentIdentifier": {"type": "UUID", "uuid": assessment},
            "includeCustomTypes": ["CREATED"],
        }))
        .await;
    let created = created["timeline"].as_array().unwrap();
    assert_eq!(
        created.len(),
        2,
        "both events raised by CreateAssessmentCommand carry the custom timeline"
    );
    assert_eq!(created[0]["event"], "AssignedToUserEvent");
    assert_eq!(created[0]["data"]["assignee"]["userId"], "test-user");
    assert!(
        created[0]["data"]["assignee"]["id"].is_i64(),
        "the new user's id is recorded"
    );
    assert_eq!(created[1]["customData"], json!({"source": "test"}));
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn placeholders_must_refer_to_created_things(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let assessment = app.create_assessment("SAN", "X1").await;

    let (status, error) = app
        .request(
            Method::POST,
            "/command",
            &[FRONTEND],
            Some(json!({"commands": [
                {"type": "UpdateFormVersionCommand", "user": user(), "assessmentUuid": assessment, "version": "2"},
                {"type": "UpdateFlagsCommand", "user": user(), "assessmentUuid": "@0", "flags": []},
            ]})),
        )
        .await;

    assert_eq!(status, StatusCode::BAD_REQUEST);
    assert_eq!(error["userMessage"], "Placeholder value not found @0");
    assert_eq!(app.version(&assessment).await["formVersion"], "1", "nothing is saved");
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn event_handling_failures_are_bad_requests(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let assessment = app.create_assessment("SAN", "X1").await;

    let (status, error) = app
        .request(
            Method::POST,
            "/command",
            &[FRONTEND],
            Some(json!({"commands": [{
                "type": "UpdateAssessmentAnswersCommand", "user": user(), "assessmentUuid": assessment,
                "added": {}, "removed": ["never_answered"],
            }]})),
        )
        .await;

    assert_eq!(status, StatusCode::BAD_REQUEST);
    assert!(
        error["userMessage"]
            .as_str()
            .unwrap()
            .starts_with("AssessmentAnswersUpdatedEventHandler was unable to handle AssessmentAnswersUpdatedEvent")
    );
    assert!(
        error["developerMessage"]
            .as_str()
            .unwrap()
            .starts_with("The answer \"never_answered\" could not be found")
    );
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn reads_and_rolls_back_to_earlier_versions(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let assessment = app.create_assessment("SAN", "X1").await;

    app.answer(&assessment, "q1", "first").await;
    let first_version = app.version(&assessment).await;
    let first_time = first_version["updatedAt"].as_str().unwrap().to_owned();
    tick().await;
    app.answer(&assessment, "q1", "second").await;

    assert_eq!(app.version(&assessment).await["answers"]["q1"], single("second"));
    assert_eq!(
        app.version_at(&assessment, Some(&first_time)).await["answers"]["q1"],
        single("first")
    );

    let (status, error) = app
        .request(
            Method::POST,
            "/query",
            &[VIEWER],
            Some(json!({"queries": [{
                "type": "AssessmentVersionQuery", "user": user(),
                "assessmentIdentifier": {"type": "UUID", "uuid": assessment},
                "timestamp": "2000-01-01T00:00:00",
            }]})),
        )
        .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    assert_eq!(
        error["developerMessage"],
        "Timestamp cannot be before the assessment created date"
    );

    tick().await;
    app.commands(json!([{
        "type": "RollbackCommand", "user": user(), "assessmentUuid": assessment, "pointInTime": first_time,
    }]))
    .await;
    assert_eq!(app.version(&assessment).await["answers"]["q1"], single("first"));
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn snapshots_aggregates_every_fifty_events(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let assessment = app.create_assessment("SAN", "X1").await;

    // Creating the assessment applies two events, so this brings the total to 60.
    for batch in 0..2 {
        let commands: Vec<Value> = (0..29)
            .map(|index| {
                json!({
                    "type": "UpdateAssessmentAnswersCommand", "user": user(), "assessmentUuid": assessment,
                    "added": {format!("q{batch}_{index}"): single("x")}, "removed": [],
                })
            })
            .collect();
        app.commands(json!(commands)).await;
    }

    let snapshots: Vec<(i32, i64)> = sqlx::query_as(
        "SELECT position, events_applied FROM aggregate WHERE assessment_uuid = $1::uuid ORDER BY position",
    )
    .bind(&assessment)
    .fetch_all(&app.pool)
    .await
    .unwrap();
    assert_eq!(
        snapshots.iter().map(|(_, applied)| *applied).collect::<Vec<_>>(),
        vec![50, 10]
    );

    let version = app.version(&assessment).await;
    assert_eq!(version["answers"].as_object().unwrap().len(), 58);
    assert_eq!(version["answers"]["q1_28"], single("x"));
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn soft_deletes_and_undeletes_history(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let assessment = app.create_assessment("SAN", "X1").await;
    app.answer(&assessment, "q1", "kept").await;
    tick().await;
    let cut_off = app.version(&assessment).await["updatedAt"].as_str().unwrap().to_owned();
    tick().await;
    app.answer(&assessment, "q2", "deleted").await;
    let deleted_from = latest_event_time(&app, &assessment).await;

    app.commands(json!([{
        "type": "SoftDeleteCommand", "user": user(), "assessmentUuid": assessment, "pointInTime": deleted_from,
    }]))
    .await;
    let version = app.version(&assessment).await;
    assert_eq!(version["answers"]["q1"], single("kept"));
    assert!(version["answers"].get("q2").is_none());
    assert_eq!(version["updatedAt"], cut_off);

    let soft_deleted = app
        .query(json!({
            "type": "GetAssessmentsSoftDeletedSinceQuery", "user": user(), "assessmentType": "SAN", "since": cut_off,
        }))
        .await;
    assert_eq!(soft_deleted["assessments"], json!([assessment]));

    app.commands(json!([{
        "type": "UndeleteCommand", "user": user(), "assessmentUuid": assessment, "pointInTime": deleted_from,
    }]))
    .await;
    assert_eq!(app.version(&assessment).await["answers"]["q2"], single("deleted"));
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn cannot_undelete_once_newer_events_exist(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let assessment = app.create_assessment("SAN", "X1").await;
    tick().await;
    app.answer(&assessment, "q1", "deleted").await;
    let deleted_from = latest_event_time(&app, &assessment).await;

    app.commands(json!([{
        "type": "SoftDeleteCommand", "user": user(), "assessmentUuid": assessment, "pointInTime": deleted_from,
    }]))
    .await;
    tick().await;
    app.answer(&assessment, "q1", "newer").await;

    let (status, error) = app
        .request(
            Method::POST,
            "/command",
            &[FRONTEND],
            Some(json!({"commands": [{
                "type": "UndeleteCommand", "user": user(), "assessmentUuid": assessment, "pointInTime": deleted_from,
            }]})),
        )
        .await;
    assert_eq!(status, StatusCode::CONFLICT);
    assert_eq!(error["userMessage"], "Unable to undelete");
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn backdated_commands_use_the_given_time(pool: PgPool) {
    let app = TestApp::new(pool).await;

    let response = app
        .commands_at(
            "/command?backdateTo=2025-07-01T09:30:00",
            json!([{
                "type": "CreateAssessmentCommand", "user": user(), "formVersion": "1", "assessmentType": "SAN",
            }]),
        )
        .await;
    let assessment = response["commands"][0]["result"]["assessmentUuid"].as_str().unwrap();

    // The clock runs on from the given time, so allow for the time the request took, as the
    // Kotlin service's tests did.
    let created_at: LocalDateTime = app.version(assessment).await["createdAt"]
        .as_str()
        .unwrap()
        .parse()
        .unwrap();
    let backdated_to: LocalDateTime = "2025-07-01T09:30:00".parse().unwrap();
    assert!(
        (created_at.0 - backdated_to.0).num_seconds().abs() < 2,
        "created at {created_at}"
    );
    let stored: String = sqlx::query_scalar("SELECT created_at::text FROM assessment WHERE uuid = $1::uuid")
        .bind(assessment)
        .fetch_one(&app.pool)
        .await
        .unwrap();
    assert!(
        stored.starts_with("2025-07-01 08:30:00"),
        "stored as the BST instant, got {stored}"
    );

    let (status, _) = app
        .request(
            Method::POST,
            "/command?backdateTo=2999-01-01T00:00:00",
            &[FRONTEND],
            Some(json!({"commands": []})),
        )
        .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn pages_through_recently_modified_assessments(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let first = app.create_assessment("SAN", "X1").await;
    let second = app.create_assessment("SAN", "X2").await;
    app.create_assessment("OTHER", "X3").await;
    let since = "2000-01-01T00:00:00";

    let (status, error) = app
        .request(
            Method::POST,
            "/query",
            &[VIEWER],
            Some(json!({"queries": [{
                "type": "GetAssessmentsModifiedSinceQuery", "user": user(), "assessmentType": "SAN", "since": since,
            }]})),
        )
        .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    assert_eq!(
        error["developerMessage"],
        "The 'since' parameter cannot be older than 1 day(s)"
    );

    let an_hour_ago = Clock::system().now().0 - chrono::Duration::hours(1);
    let since = LocalDateTime(an_hour_ago).to_string();

    let page = app
        .query(json!({
            "type": "GetAssessmentsModifiedSinceQuery", "user": user(), "assessmentType": "SAN", "since": since, "limit": 1,
        }))
        .await;
    let mut expected = [first.clone(), second.clone()];
    expected.sort();
    assert_eq!(page["assessments"][0]["type"], "AssessmentVersionQueryResult");
    assert_eq!(page["assessments"][0]["assessmentUuid"], expected[0]);
    assert_eq!(page["nextCursor"], expected[0]);

    let next = app
        .query(json!({
            "type": "GetAssessmentsModifiedSinceQuery", "user": user(), "assessmentType": "SAN", "since": since,
            "limit": 1, "after": page["nextCursor"],
        }))
        .await;
    assert_eq!(next["assessments"][0]["assessmentUuid"], expected[1]);
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn redacts_events_on_a_dry_run_without_saving(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let assessment = app.create_assessment("SAN", "X1").await;
    app.answer(&assessment, "q1", "sensitive").await;

    let (status, data) = app
        .request(
            Method::GET,
            &format!("/data-deletion/{assessment}"),
            &[DATA_DELETION],
            None,
        )
        .await;
    assert_eq!(status, StatusCode::OK);
    let answer_event = data["events"]
        .as_array()
        .unwrap()
        .iter()
        .find(|event| event["data"]["type"] == "AssessmentAnswersUpdatedEvent")
        .unwrap()
        .clone();

    let request = json!({
        "events": [{"uuid": answer_event["uuid"], "operation": "DELETE", "event": answer_event["data"]}],
    });
    let (status, response) = app
        .request(
            Method::POST,
            &format!("/data-deletion/{assessment}"),
            &[DATA_DELETION],
            Some(request.clone()),
        )
        .await;
    assert_eq!(status, StatusCode::OK, "{response:#}");
    assert_eq!(response["success"], true);
    assert_eq!(response["dryRun"], true);
    let rebuilt = &response["rebuiltState"]["AssessmentAggregate"][0]["data"];
    assert!(rebuilt["answers"].get("q1").is_none());
    assert_eq!(
        app.version(&assessment).await["answers"]["q1"],
        single("sensitive"),
        "dry runs save nothing"
    );

    let mut for_real = request;
    for_real["dryRun"] = json!(false);
    let (status, _) = app
        .request(
            Method::POST,
            &format!("/data-deletion/{assessment}"),
            &[DATA_DELETION],
            Some(for_real),
        )
        .await;
    assert_eq!(status, StatusCode::OK);
    assert!(app.version(&assessment).await["answers"].get("q1").is_none());

    let (_, data) = app
        .request(
            Method::GET,
            &format!("/data-deletion/{assessment}"),
            &[DATA_DELETION],
            None,
        )
        .await;
    let redacted = data["events"]
        .as_array()
        .unwrap()
        .iter()
        .find(|event| event["uuid"] == answer_event["uuid"])
        .unwrap();
    assert_eq!(redacted["data"]["type"], "RedactedEvent");
    assert_eq!(redacted["data"]["eventType"], "AssessmentAnswersUpdatedEvent");
    assert_eq!(redacted["data"]["redactedBy"], "test-client");
    assert_eq!(redacted["position"], answer_event["position"], "positions are kept");
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn subject_access_requests(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let assessment = app.create_assessment("SAN", "X999").await;
    app.answer(&assessment, "q1", "YES").await;

    let (status, body) = app
        .request(Method::GET, "/subject-access-request?crn=X999", &[SAR], None)
        .await;
    assert_eq!(status, StatusCode::OK, "{body:#}");
    assert_eq!(body["content"]["type"], "SubjectAccessRequestQueryResult");
    let result = &body["content"]["results"][0];
    assert_eq!(result["assessmentType"], "SAN");
    assert_eq!(result["hasAnswersOrProperties"], true);
    assert_eq!(result["answers"][0]["key"], "q1");
    assert_eq!(result["identifiers"], json!({"CRN": "X999"}));

    let (status, _) = app
        .request(Method::GET, "/subject-access-request?crn=UNKNOWN", &[SAR], None)
        .await;
    assert_eq!(status, StatusCode::NO_CONTENT);

    let (status, _) = app.request(Method::GET, "/subject-access-request", &[SAR], None).await;
    assert_eq!(status.as_u16(), 209);

    let (status, template) = app
        .request(Method::GET, "/subject-access-request/template", &[SAR], None)
        .await;
    assert_eq!(status, StatusCode::OK);
    assert!(template.as_str().unwrap().contains("Assess Risks and Needs"));
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn starts_a_new_period_of_supervision(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let response = app
        .commands(json!([
            {"type": "CreateAssessmentCommand", "user": user(), "formVersion": "1", "assessmentType": "SENTENCE_PLAN"},
            {"type": "CreateCollectionCommand", "user": user(), "assessmentUuid": "@0", "name": "GOALS"},
            {"type": "AddCollectionItemCommand", "user": user(), "assessmentUuid": "@0", "collectionUuid": "@1",
             "answers": {"target_date": single("2030-01-01")}, "properties": {"status": single("ACTIVE")}},
            {"type": "AddCollectionItemCommand", "user": user(), "assessmentUuid": "@0", "collectionUuid": "@1",
             "answers": {}, "properties": {"status": single("ACHIEVED")}},
            {"type": "CreateCollectionCommand", "user": user(), "assessmentUuid": "@0", "name": "PLAN_AGREEMENTS"},
            {"type": "AddCollectionItemCommand", "user": user(), "assessmentUuid": "@0", "collectionUuid": "@4",
             "answers": {}, "properties": {}},
        ]))
        .await;
    let plan = response["commands"][0]["result"]["assessmentUuid"].as_str().unwrap();

    let (status, body) = app
        .request(
            Method::POST,
            "/plan/start-new-period-of-supervision",
            &[COORDINATOR],
            Some(json!({"user": user(), "assessmentUuid": plan})),
        )
        .await;
    assert_eq!(status, StatusCode::OK, "{body:#}");

    let version = app.version(plan).await;
    let goals = &version["collections"][0]["items"];
    assert_eq!(goals[0]["properties"]["status"], single("REMOVED"));
    assert!(goals[0]["answers"].get("target_date").is_none());
    assert_eq!(goals[0]["collections"][0]["name"], "NOTES");
    assert_eq!(
        goals[0]["collections"][0]["items"][0]["answers"]["note"],
        single("Automatically removed as the previous supervision period has ended.")
    );
    assert_eq!(goals[1]["properties"]["status"], single("ACHIEVED"));
    assert_eq!(version["collections"][1]["items"], json!([]));

    let not_a_plan = app.create_assessment("SAN", "X1").await;
    let (status, _) = app
        .request(
            Method::POST,
            "/plan/start-new-period-of-supervision",
            &[COORDINATOR],
            Some(json!({"user": user(), "assessmentUuid": not_a_plan})),
        )
        .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn unknown_form_versions_fail_the_oasys_mapping(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let assessment = app.create_assessment("SAN", "X1").await;

    let (status, error) = app
        .request(
            Method::POST,
            "/command",
            &[FRONTEND],
            Some(json!({"commands": [{
                "type": "UpdateAssessmentAnswersCommand", "user": user(), "assessmentUuid": assessment,
                "added": {"q1": single("YES")}, "removed": [],
                "hooks": [{"type": "UpdateOasysDataMapping", "formConfig": {"version": "v9.9", "fields": {}}}],
            }]})),
        )
        .await;

    assert_eq!(status, StatusCode::INTERNAL_SERVER_ERROR);
    assert_eq!(error["developerMessage"], "No data mapping found for form version v9.9");
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn deletes_and_rebuilds_assessments(pool: PgPool) {
    let app = TestApp::new(pool).await;
    let assessment = app.create_assessment("SAN", "X1").await;
    app.answer(&assessment, "q1", "YES").await;

    let (status, _) = app
        .request(
            Method::POST,
            &format!("/assessment/{assessment}/rebuild"),
            &[DATA_DELETION],
            None,
        )
        .await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(app.version(&assessment).await["answers"]["q1"], single("YES"));

    let (status, _) = app
        .request(
            Method::DELETE,
            &format!("/assessment/{assessment}"),
            &[COORDINATOR],
            None,
        )
        .await;
    assert_eq!(status, StatusCode::OK);

    let (status, error) = app
        .request(
            Method::POST,
            "/query",
            &[VIEWER],
            Some(json!({"queries": [{
                "type": "DailyVersionsQuery", "user": user(),
                "assessmentIdentifier": {"type": "UUID", "uuid": assessment},
            }]})),
        )
        .await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    assert_eq!(
        error["developerMessage"],
        format!("No assessment found for UUID: {assessment}")
    );
}

#[sqlx::test(migrator = "MIGRATOR")]
async fn serves_the_openapi_document(pool: PgPool) {
    let app = TestApp::new(pool).await;

    let (status, document) = app.request(Method::GET, "/v3/api-docs", &[], None).await;
    assert_eq!(status, StatusCode::OK);
    assert!(document["paths"].get("/command").is_some());
    assert!(
        document["components"]["schemas"]
            .get("CreateAssessmentCommand")
            .is_some()
    );
}

async fn latest_event_time(app: &TestApp, assessment: &str) -> String {
    let (_, data) = app
        .request(
            Method::GET,
            &format!("/data-deletion/{assessment}"),
            &[DATA_DELETION],
            None,
        )
        .await;
    data["events"].as_array().unwrap().last().unwrap()["createdAt"]
        .as_str()
        .unwrap()
        .to_owned()
}
