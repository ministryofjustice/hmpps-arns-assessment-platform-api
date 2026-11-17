package httpapi_test

import (
	"context"
	"fmt"
	"net/http"
	"slices"
	"strings"
	"testing"
	"time"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
)

func expect(t *testing.T, got, want any, what string) {
	t.Helper()
	if asJSON(got) != asJSON(want) {
		t.Errorf("%s: got %s, want %s", what, asJSON(got), asJSON(want))
	}
}

func TestHealthAndUnknownRoutes(t *testing.T) {
	app := newTestApp(t)

	status, body := app.request(http.MethodGet, "/health/ping", nil, nil)
	expect(t, status, http.StatusOK, "status")
	expect(t, body, obj{"status": "UP"}, "ping")

	status, body = app.request(http.MethodGet, "/no-such-thing", nil, nil)
	expect(t, status, http.StatusNotFound, "status")
	expect(t, at(body, "status"), 404, "body status")
}

func TestRequiresAValidTokenWithAnAllowedRole(t *testing.T) {
	app := newTestApp(t)
	body := obj{"queries": []any{}}

	status, _ := app.request(http.MethodPost, "/query", nil, body)
	expect(t, status, http.StatusUnauthorized, "no token")

	status, response := app.request(http.MethodPost, "/query", []string{sar}, body)
	expect(t, status, http.StatusForbidden, "wrong role")
	expect(t, at(response, "userMessage"), "Forbidden: Access Denied", "message")

	status, response = app.request(http.MethodPost, "/query", []string{viewer}, body)
	expect(t, status, http.StatusBadRequest, "no queries")
	expect(t, at(response, "developerMessage"), "No queries received", "message")

	status, _ = app.request(http.MethodPost, "/command", []string{viewer}, obj{"commands": []any{}})
	expect(t, status, http.StatusForbidden, "viewer commands")
}

func TestInvalidPayloadsAreBadRequests(t *testing.T) {
	app := newTestApp(t)

	status, response := app.request(http.MethodPost, "/command", []string{frontend},
		obj{"commands": []any{obj{"type": "NotACommand", "user": user()}}})

	expect(t, status, http.StatusBadRequest, "status")
	if !strings.HasPrefix(at(response, "userMessage").(string), "Invalid payload") {
		t.Errorf("got %v", response)
	}
}

func TestBuildsAnAssessmentFromCommandsUsingPlaceholders(t *testing.T) {
	app := newTestApp(t)

	response := app.commands(
		obj{
			"type": "CreateAssessmentCommand", "user": user(), "formVersion": "1.0", "assessmentType": "SENTENCE_PLAN",
			"identifiers": obj{"CRN": "X123456"}, "flags": []string{"SAN_BETA"},
			"timeline": obj{"type": "CREATED", "data": obj{"source": "test"}},
		},
		obj{"type": "CreateCollectionCommand", "user": user(), "assessmentUuid": "@0", "name": "GOALS"},
		obj{
			"type": "AddCollectionItemCommand", "user": user(), "assessmentUuid": "@0", "collectionUuid": "@1",
			"answers": obj{"title": single("Find a job")}, "properties": obj{"status": single("ACTIVE")}, "index": nil,
		},
		obj{"type": "CreateCollectionCommand", "user": user(), "assessmentUuid": "@0", "name": "STEPS", "parentCollectionItemUuid": "@2"},
		obj{
			"type": "UpdateAssessmentAnswersCommand", "user": user(), "assessmentUuid": "@0",
			"added": obj{"q1": single("YES"), "q2": obj{"type": "Multi", "values": []string{"A", "B"}}}, "removed": []string{},
		},
	)

	result := func(i int) any { return at(response, "commands", i, "result") }
	expect(t, at(result(0), "type"), "CreateAssessmentCommandResult", "result 0")
	expect(t, at(result(1), "type"), "CreateCollectionCommandResult", "result 1")
	expect(t, at(result(2), "type"), "AddCollectionItemCommandResult", "result 2")
	expect(t, result(4), obj{"type": "CommandSuccessCommandResult", "message": "Done", "success": true}, "result 4")
	expect(t, at(response, "commands", 1, "request", "assessmentUuid"), "@0", "requests are echoed as sent")

	assessment := at(result(0), "assessmentUuid").(string)
	goal := at(result(2), "collectionItemUuid").(string)

	version := app.version(assessment)
	expect(t, at(version, "type"), "AssessmentVersionQueryResult", "type")
	expect(t, at(version, "assessmentType"), "SENTENCE_PLAN", "assessmentType")
	expect(t, at(version, "formVersion"), "1.0", "formVersion")
	expect(t, at(version, "identifiers"), obj{"CRN": "X123456"}, "identifiers")
	expect(t, at(version, "flags"), []string{"SAN_BETA"}, "flags")
	expect(t, at(version, "answers", "q2"), obj{"type": "Multi", "values": []string{"A", "B"}}, "q2")
	expect(t, at(version, "collections", 0, "name"), "GOALS", "collection")
	expect(t, at(version, "collections", 0, "items", 0, "uuid"), goal, "goal")
	expect(t, at(version, "collections", 0, "items", 0, "collections", 0, "name"), "STEPS", "nested")
	expect(t, at(version, "collaborators", 0, "userId"), "test-user", "collaborator")
	expect(t, at(version, "assignedUser", "name"), "Test User", "assignee")

	byCRN := app.query(obj{
		"type": "AssessmentVersionQuery", "user": user(),
		"assessmentIdentifier": obj{"type": "EXTERNAL", "identifier": "X123456", "identifierType": "CRN", "assessmentType": "SENTENCE_PLAN"},
	})
	expect(t, at(byCRN, "assessmentUuid"), assessment, "by CRN")

	item := app.query(obj{
		"type": "CollectionItemQuery", "user": user(),
		"assessmentIdentifier": obj{"type": "UUID", "uuid": assessment}, "collectionItemUuid": goal, "depth": 0,
	})
	expect(t, at(item, "collectionItem", "answers", "title"), single("Find a job"), "item answers")
	expect(t, at(item, "collectionItem", "collections", 0, "items"), []any{}, "truncated")

	timeline := app.query(obj{
		"type": "TimelineQuery", "user": user(), "assessmentIdentifier": obj{"type": "UUID", "uuid": assessment}, "pageSize": 3,
	})
	expect(t, at(timeline, "pageInfo"), obj{"pageNumber": 0, "totalPages": 2}, "page info")
	expect(t, at(timeline, "timeline", 0, "event"), "AssessmentAnswersUpdatedEvent", "latest event")
	expect(t, at(timeline, "timeline", 0, "data", "added"), []string{"q1", "q2"}, "added, in order")
	expect(t, at(timeline, "timeline", 0, "position"), 5, "position")

	created := at(app.query(obj{
		"type": "TimelineQuery", "user": user(), "assessmentIdentifier": obj{"type": "UUID", "uuid": assessment},
		"includeCustomTypes": []string{"CREATED"},
	}), "timeline").([]any)
	expect(t, len(created), 2, "both events raised by CreateAssessmentCommand carry the custom timeline")
	expect(t, at(created[0], "event"), "AssignedToUserEvent", "assignment")
	expect(t, at(created[0], "data", "assignee", "userId"), "test-user", "assignee")
	if _, ok := at(created[0], "data", "assignee", "id").(float64); !ok {
		t.Errorf("expected the new user's id to be recorded, got %v", at(created[0], "data", "assignee"))
	}
	expect(t, at(created[1], "customData"), obj{"source": "test"}, "custom data")
}

func TestPlaceholdersMustReferToCreatedThings(t *testing.T) {
	app := newTestApp(t)
	assessment := app.createAssessment("SAN", "X1")

	status, response := app.request(http.MethodPost, "/command", []string{frontend}, obj{"commands": []any{
		obj{"type": "UpdateFormVersionCommand", "user": user(), "assessmentUuid": assessment, "version": "2"},
		obj{"type": "UpdateFlagsCommand", "user": user(), "assessmentUuid": "@0", "flags": []string{}},
	}})

	expect(t, status, http.StatusBadRequest, "status")
	expect(t, at(response, "userMessage"), "Placeholder value not found @0", "message")
	expect(t, at(app.version(assessment), "formVersion"), "1", "nothing is saved")
}

func TestEventHandlingFailuresAreBadRequests(t *testing.T) {
	app := newTestApp(t)
	assessment := app.createAssessment("SAN", "X1")

	status, response := app.request(http.MethodPost, "/command", []string{frontend}, obj{"commands": []any{obj{
		"type": "UpdateAssessmentAnswersCommand", "user": user(), "assessmentUuid": assessment,
		"added": obj{}, "removed": []string{"never_answered"},
	}}})

	expect(t, status, http.StatusBadRequest, "status")
	if !strings.HasPrefix(at(response, "userMessage").(string),
		"AssessmentAnswersUpdatedEventHandler was unable to handle AssessmentAnswersUpdatedEvent") {
		t.Errorf("got %v", response)
	}
	if !strings.HasPrefix(at(response, "developerMessage").(string), `The answer "never_answered" could not be found`) {
		t.Errorf("got %v", response)
	}
}

func TestReadsAndRollsBackToEarlierVersions(t *testing.T) {
	app := newTestApp(t)
	assessment := app.createAssessment("SAN", "X1")

	app.answer(assessment, "q1", "first")
	firstTime := at(app.version(assessment), "updatedAt").(string)
	tick()
	app.answer(assessment, "q1", "second")

	expect(t, at(app.version(assessment), "answers", "q1"), single("second"), "latest")
	expect(t, at(app.versionAt(assessment, firstTime), "answers", "q1"), single("first"), "earlier")

	status, response := app.request(http.MethodPost, "/query", []string{viewer}, obj{"queries": []any{obj{
		"type": "AssessmentVersionQuery", "user": user(),
		"assessmentIdentifier": obj{"type": "UUID", "uuid": assessment}, "timestamp": "2000-01-01T00:00:00",
	}}})
	expect(t, status, http.StatusBadRequest, "before creation")
	expect(t, at(response, "developerMessage"), "Timestamp cannot be before the assessment created date", "message")

	tick()
	app.commands(obj{"type": "RollbackCommand", "user": user(), "assessmentUuid": assessment, "pointInTime": firstTime})
	expect(t, at(app.version(assessment), "answers", "q1"), single("first"), "rolled back")
}

func TestSnapshotsAggregatesEveryFiftyEvents(t *testing.T) {
	app := newTestApp(t)
	assessment := app.createAssessment("SAN", "X1")

	// Creating the assessment applies two events, so this brings the total to 60.
	for batch := range 2 {
		var commands []any
		for index := range 29 {
			commands = append(commands, obj{
				"type": "UpdateAssessmentAnswersCommand", "user": user(), "assessmentUuid": assessment,
				"added": obj{fmt.Sprintf("q%d_%d", batch, index): single("x")}, "removed": []string{},
			})
		}
		app.commands(commands...)
	}

	rows, err := app.pool.Query(context.Background(),
		"SELECT events_applied FROM aggregate WHERE assessment_uuid = $1::uuid ORDER BY position", assessment)
	if err != nil {
		t.Fatal(err)
	}
	var applied []int64
	for rows.Next() {
		var count int64
		_ = rows.Scan(&count)
		applied = append(applied, count)
	}
	expect(t, applied, []int64{50, 10}, "snapshots")

	version := app.version(assessment)
	expect(t, len(at(version, "answers").(map[string]any)), 58, "answers")
	expect(t, at(version, "answers", "q1_28"), single("x"), "last answer")
}

func latestEventTime(t *testing.T, app *testApp, assessment string) string {
	t.Helper()
	_, data := app.request(http.MethodGet, "/data-deletion/"+assessment, []string{dataDeletion}, nil)
	events := at(data, "events").([]any)
	return at(events[len(events)-1], "createdAt").(string)
}

func TestSoftDeletesAndUndeletesHistory(t *testing.T) {
	app := newTestApp(t)
	assessment := app.createAssessment("SAN", "X1")
	app.answer(assessment, "q1", "kept")
	tick()
	cutOff := at(app.version(assessment), "updatedAt").(string)
	tick()
	app.answer(assessment, "q2", "deleted")
	deletedFrom := latestEventTime(t, app, assessment)

	app.commands(obj{"type": "SoftDeleteCommand", "user": user(), "assessmentUuid": assessment, "pointInTime": deletedFrom})
	version := app.version(assessment)
	expect(t, at(version, "answers", "q1"), single("kept"), "kept")
	expect(t, at(version, "answers", "q2"), nil, "deleted")
	expect(t, at(version, "updatedAt"), cutOff, "updatedAt")

	softDeleted := app.query(obj{
		"type": "GetAssessmentsSoftDeletedSinceQuery", "user": user(), "assessmentType": "SAN", "since": cutOff,
	})
	expect(t, at(softDeleted, "assessments"), []string{assessment}, "soft deleted")

	app.commands(obj{"type": "UndeleteCommand", "user": user(), "assessmentUuid": assessment, "pointInTime": deletedFrom})
	expect(t, at(app.version(assessment), "answers", "q2"), single("deleted"), "undeleted")
}

func TestCannotUndeleteOnceNewerEventsExist(t *testing.T) {
	app := newTestApp(t)
	assessment := app.createAssessment("SAN", "X1")
	tick()
	app.answer(assessment, "q1", "deleted")
	deletedFrom := latestEventTime(t, app, assessment)

	app.commands(obj{"type": "SoftDeleteCommand", "user": user(), "assessmentUuid": assessment, "pointInTime": deletedFrom})
	tick()
	app.answer(assessment, "q1", "newer")

	status, response := app.request(http.MethodPost, "/command", []string{frontend}, obj{"commands": []any{obj{
		"type": "UndeleteCommand", "user": user(), "assessmentUuid": assessment, "pointInTime": deletedFrom,
	}}})
	expect(t, status, http.StatusConflict, "status")
	expect(t, at(response, "userMessage"), "Unable to undelete", "message")
}

func TestBackdatedCommandsUseTheGivenTime(t *testing.T) {
	app := newTestApp(t)

	response := app.commandsAt("/command?backdateTo=2025-07-01T09:30:00",
		obj{"type": "CreateAssessmentCommand", "user": user(), "formVersion": "1", "assessmentType": "SAN"})
	assessment := at(response, "commands", 0, "result", "assessmentUuid").(string)

	// The clock runs on from the given time, so allow for the time the request took, as the
	// Kotlin service's tests did.
	createdAt := datetime.MustParse(at(app.version(assessment), "createdAt").(string))
	if difference := createdAt.Sub(datetime.MustParse("2025-07-01T09:30:00")); difference < 0 || difference > 2*time.Second {
		t.Errorf("created at %s", createdAt)
	}
	var stored string
	_ = app.pool.QueryRow(context.Background(),
		"SELECT to_char(created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD HH24:MI') FROM assessment WHERE uuid = $1::uuid", assessment).Scan(&stored)
	expect(t, stored, "2025-07-01 08:30", "stored as the BST instant")

	status, _ := app.request(http.MethodPost, "/command?backdateTo=2999-01-01T00:00:00", []string{frontend}, obj{"commands": []any{}})
	expect(t, status, http.StatusBadRequest, "future backdate")
}

func TestPagesThroughRecentlyModifiedAssessments(t *testing.T) {
	app := newTestApp(t)
	first := app.createAssessment("SAN", "X1")
	second := app.createAssessment("SAN", "X2")
	app.createAssessment("OTHER", "X3")

	status, response := app.request(http.MethodPost, "/query", []string{viewer}, obj{"queries": []any{obj{
		"type": "GetAssessmentsModifiedSinceQuery", "user": user(), "assessmentType": "SAN", "since": "2000-01-01T00:00:00",
	}}})
	expect(t, status, http.StatusBadRequest, "too old")
	expect(t, at(response, "developerMessage"), "The 'since' parameter cannot be older than 1 day(s)", "message")

	since := clock.System().Now().Add(-time.Hour).String()
	page := app.query(obj{
		"type": "GetAssessmentsModifiedSinceQuery", "user": user(), "assessmentType": "SAN", "since": since, "limit": 1,
	})
	expected := []string{first, second}
	slices.Sort(expected)
	expect(t, at(page, "assessments", 0, "type"), "AssessmentVersionQueryResult", "nested results are typed")
	expect(t, at(page, "assessments", 0, "assessmentUuid"), expected[0], "first page")
	expect(t, at(page, "nextCursor"), expected[0], "cursor")

	next := app.query(obj{
		"type": "GetAssessmentsModifiedSinceQuery", "user": user(), "assessmentType": "SAN", "since": since,
		"limit": 1, "after": at(page, "nextCursor"),
	})
	expect(t, at(next, "assessments", 0, "assessmentUuid"), expected[1], "second page")
}

func TestRedactsEventsOnADryRunWithoutSaving(t *testing.T) {
	app := newTestApp(t)
	assessment := app.createAssessment("SAN", "X1")
	app.answer(assessment, "q1", "sensitive")

	status, data := app.request(http.MethodGet, "/data-deletion/"+assessment, []string{dataDeletion}, nil)
	expect(t, status, http.StatusOK, "status")
	var answerEvent any
	for _, e := range at(data, "events").([]any) {
		if at(e, "data", "type") == "AssessmentAnswersUpdatedEvent" {
			answerEvent = e
		}
	}

	request := obj{"events": []any{obj{"uuid": at(answerEvent, "uuid"), "operation": "DELETE", "event": at(answerEvent, "data")}}}
	status, response := app.request(http.MethodPost, "/data-deletion/"+assessment, []string{dataDeletion}, request)
	expect(t, status, http.StatusOK, "dry run")
	expect(t, at(response, "success"), true, "success")
	expect(t, at(response, "dryRun"), true, "dryRun")
	expect(t, at(response, "rebuiltState", "AssessmentAggregate", 0, "data", "answers", "q1"), nil, "rebuilt without q1")
	expect(t, at(app.version(assessment), "answers", "q1"), single("sensitive"), "dry runs save nothing")

	request["dryRun"] = false
	status, _ = app.request(http.MethodPost, "/data-deletion/"+assessment, []string{dataDeletion}, request)
	expect(t, status, http.StatusOK, "for real")
	expect(t, at(app.version(assessment), "answers", "q1"), nil, "redacted")

	_, data = app.request(http.MethodGet, "/data-deletion/"+assessment, []string{dataDeletion}, nil)
	for _, e := range at(data, "events").([]any) {
		if at(e, "uuid") == at(answerEvent, "uuid") {
			expect(t, at(e, "data", "type"), "RedactedEvent", "type")
			expect(t, at(e, "data", "eventType"), "AssessmentAnswersUpdatedEvent", "eventType")
			expect(t, at(e, "data", "redactedBy"), "test-client", "redactedBy")
			expect(t, at(e, "position"), at(answerEvent, "position"), "positions are kept")
		}
	}
}

func TestSubjectAccessRequests(t *testing.T) {
	app := newTestApp(t)
	assessment := app.createAssessment("SAN", "X999")
	app.answer(assessment, "q1", "YES")

	status, body := app.request(http.MethodGet, "/subject-access-request?crn=X999", []string{sar}, nil)
	expect(t, status, http.StatusOK, "status")
	expect(t, at(body, "content", "type"), "SubjectAccessRequestQueryResult", "type")
	result := at(body, "content", "results", 0)
	expect(t, at(result, "assessmentType"), "SAN", "assessmentType")
	expect(t, at(result, "hasAnswersOrProperties"), true, "hasAnswersOrProperties")
	expect(t, at(result, "answers", 0, "key"), "q1", "answer")
	expect(t, at(result, "identifiers"), obj{"CRN": "X999"}, "identifiers")

	status, _ = app.request(http.MethodGet, "/subject-access-request?crn=UNKNOWN", []string{sar}, nil)
	expect(t, status, http.StatusNoContent, "unknown")

	status, _ = app.request(http.MethodGet, "/subject-access-request", []string{sar}, nil)
	expect(t, status, 209, "no identifier")

	status, template := app.request(http.MethodGet, "/subject-access-request/template", []string{sar}, nil)
	expect(t, status, http.StatusOK, "template")
	if !strings.Contains(template.(string), "Assess Risks and Needs") {
		t.Error("expected the template")
	}
}

func TestStartsANewPeriodOfSupervision(t *testing.T) {
	app := newTestApp(t)
	response := app.commands(
		obj{"type": "CreateAssessmentCommand", "user": user(), "formVersion": "1", "assessmentType": "SENTENCE_PLAN"},
		obj{"type": "CreateCollectionCommand", "user": user(), "assessmentUuid": "@0", "name": "GOALS"},
		obj{"type": "AddCollectionItemCommand", "user": user(), "assessmentUuid": "@0", "collectionUuid": "@1",
			"answers": obj{"target_date": single("2030-01-01")}, "properties": obj{"status": single("ACTIVE")}},
		obj{"type": "AddCollectionItemCommand", "user": user(), "assessmentUuid": "@0", "collectionUuid": "@1",
			"answers": obj{}, "properties": obj{"status": single("ACHIEVED")}},
		obj{"type": "CreateCollectionCommand", "user": user(), "assessmentUuid": "@0", "name": "PLAN_AGREEMENTS"},
		obj{"type": "AddCollectionItemCommand", "user": user(), "assessmentUuid": "@0", "collectionUuid": "@4",
			"answers": obj{}, "properties": obj{}},
	)
	plan := at(response, "commands", 0, "result", "assessmentUuid").(string)

	status, body := app.request(http.MethodPost, "/plan/start-new-period-of-supervision", []string{coordinator},
		obj{"user": user(), "assessmentUuid": plan})
	expect(t, status, http.StatusOK, fmt.Sprint(body))

	version := app.version(plan)
	goals := at(version, "collections", 0, "items")
	expect(t, at(goals, 0, "properties", "status"), single("REMOVED"), "removed")
	expect(t, at(goals, 0, "answers", "target_date"), nil, "target date removed")
	expect(t, at(goals, 0, "collections", 0, "name"), "NOTES", "notes")
	expect(t, at(goals, 0, "collections", 0, "items", 0, "answers", "note"),
		single("Automatically removed as the previous supervision period has ended."), "note")
	expect(t, at(goals, 1, "properties", "status"), single("ACHIEVED"), "achieved goal kept")
	expect(t, at(version, "collections", 1, "items"), []any{}, "agreements removed")

	notAPlan := app.createAssessment("SAN", "X1")
	status, _ = app.request(http.MethodPost, "/plan/start-new-period-of-supervision", []string{coordinator},
		obj{"user": user(), "assessmentUuid": notAPlan})
	expect(t, status, http.StatusBadRequest, "not a plan")
}

func TestUnknownFormVersionsFailTheOasysMapping(t *testing.T) {
	app := newTestApp(t)
	assessment := app.createAssessment("SAN", "X1")

	status, response := app.request(http.MethodPost, "/command", []string{frontend}, obj{"commands": []any{obj{
		"type": "UpdateAssessmentAnswersCommand", "user": user(), "assessmentUuid": assessment,
		"added": obj{"q1": single("YES")}, "removed": []string{},
		"hooks": []any{obj{"type": "UpdateOasysDataMapping", "formConfig": obj{"version": "v9.9", "fields": obj{}}}},
	}}})

	expect(t, status, http.StatusInternalServerError, "status")
	expect(t, at(response, "developerMessage"), "No data mapping found for form version v9.9", "message")
}

func TestDeletesAndRebuildsAssessments(t *testing.T) {
	app := newTestApp(t)
	assessment := app.createAssessment("SAN", "X1")
	app.answer(assessment, "q1", "YES")

	status, _ := app.request(http.MethodPost, "/assessment/"+assessment+"/rebuild", []string{dataDeletion}, nil)
	expect(t, status, http.StatusOK, "rebuild")
	expect(t, at(app.version(assessment), "answers", "q1"), single("YES"), "rebuilt")

	status, _ = app.request(http.MethodDelete, "/assessment/"+assessment, []string{coordinator}, nil)
	expect(t, status, http.StatusOK, "delete")

	status, response := app.request(http.MethodPost, "/query", []string{viewer}, obj{"queries": []any{obj{
		"type": "DailyVersionsQuery", "user": user(), "assessmentIdentifier": obj{"type": "UUID", "uuid": assessment},
	}}})
	expect(t, status, http.StatusNotFound, "deleted")
	expect(t, at(response, "developerMessage"), "No assessment found for UUID: "+assessment, "message")
}

func TestServesTheOpenAPIDocument(t *testing.T) {
	app := newTestApp(t)

	status, document := app.request(http.MethodGet, "/v3/api-docs", nil, nil)
	expect(t, status, http.StatusOK, "status")
	if at(document, "paths", "/command") == nil || at(document, "components", "schemas", "CreateAssessmentCommand") == nil {
		t.Error("expected the API's paths and schemas")
	}
	status, _ = app.request(http.MethodGet, "/swagger-ui/index.html", nil, nil)
	expect(t, status, http.StatusOK, "swagger ui")
}
