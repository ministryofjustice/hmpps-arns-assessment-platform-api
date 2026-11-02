package command

import (
	"encoding/json"
	"strings"
	"testing"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
)

const user = `"user": {"id": "user-1", "name": "A User"}`

func decode(t *testing.T, data string) Command {
	t.Helper()
	command, err := Unmarshal([]byte(data))
	if err != nil {
		t.Fatal(err)
	}
	return command
}

func TestDecodesCommandsByType(t *testing.T) {
	command := decode(t, `{"type": "UpdateAssessmentAnswersCommand", `+user+`, "assessmentUuid": "@0",
		"added": {"q1": {"type": "Single", "value": "YES"}}, "removed": []}`)

	update, ok := command.(*UpdateAssessmentAnswers)
	if !ok || update.User.AuthSource != model.AuthSourceNotSpecified || update.Autosaved() || len(update.CommandHooks()) != 0 {
		t.Errorf("got %#v", command)
	}
}

func TestRejectsUnknownTypesAndMissingFields(t *testing.T) {
	for data, want := range map[string]string{
		`{"type": "LaunchRocketCommand"}`:                                                                                            "could not resolve type id 'LaunchRocketCommand'",
		`{` + user + `, "assessmentUuid": "x"}`:                                                                                      "missing type id property 'type'",
		`{"type": "UpdateFlagsCommand", ` + user + `, "assessmentUuid": "x"}`:                                                        "missing required property 'flags'",
		`{"type": "UpdateFlagsCommand", "assessmentUuid": "x", "flags": []}`:                                                         "missing required property 'user'",
		`{"type": "UpdateFlagsCommand", "user": {"id": "u"}, "assessmentUuid": "x", "flags": []}`:                                    "missing required property 'user.name'",
		`{"type": "CreateAssessmentCommand", ` + user + `, "formVersion": "1", "assessmentType": "T", "identifiers": {"NINO": "x"}}`: "unknown identifier type 'NINO'",
	} {
		_, err := Unmarshal([]byte(data))
		if err == nil || !strings.Contains(err.Error(), want) {
			t.Errorf("%s: got %v, want %s", data, err, want)
		}
	}
}

func TestGeneratedUUIDsAreNotReadFromOrWrittenToJSON(t *testing.T) {
	command := decode(t, `{"type": "CreateAssessmentCommand", `+user+`, "formVersion": "1", "assessmentType": "SENTENCE_PLAN",
		"assessmentUuid": "00000000-0000-0000-0000-000000000001", "identifiers": {"CRN": "X123456"}}`)

	if command.AssessmentUUID().String() == "00000000-0000-0000-0000-000000000001" {
		t.Error("expected a generated UUID")
	}
	written, _ := json.Marshal(command)
	if !strings.HasPrefix(string(written), `{"type":"CreateAssessmentCommand"`) || strings.Contains(string(written), "assessmentUuid") ||
		!strings.Contains(string(written), `"identifiers":{"CRN":"X123456"}`) || !strings.Contains(string(written), `"flags":[]`) {
		t.Errorf("got %s", written)
	}
}

func TestResolvesPlaceholdersFromEarlierResults(t *testing.T) {
	assessment, collection := uuid.New(), uuid.New()
	earlier := []Response{{Result: createdAssessment(assessment)}, {Result: createdCollection(collection)}}
	command := decode(t, `{"type": "AddCollectionItemCommand", `+user+`, "assessmentUuid": "@0", "collectionUuid": "@1",
		"answers": {}, "properties": {}}`)

	if err := resolveReferences(command, earlier); err != nil {
		t.Fatal(err)
	}

	add := command.(*AddCollectionItem)
	if command.AssessmentUUID() != assessment || add.CollectionUUID.UUID() != collection {
		t.Error("expected the placeholders to resolve")
	}
	if written, _ := json.Marshal(command); !strings.Contains(string(written), `"collectionUuid":"@1"`) {
		t.Errorf("expected the placeholder to be written back, got %s", written)
	}
}

func TestPlaceholdersMustPointAtAResultWithAUUID(t *testing.T) {
	command := decode(t, `{"type": "UpdateFlagsCommand", `+user+`, "assessmentUuid": "@0", "flags": []}`)

	err := resolveReferences(command, []Response{{Result: success()}})
	if err == nil || err.Error() != "Placeholder value not found @0" {
		t.Errorf("got %v", err)
	}
	if err := resolveReferences(command, nil); err == nil {
		t.Error("expected an out of range placeholder to fail")
	}
	invalid := decode(t, `{"type": "UpdateFlagsCommand", `+user+`, "assessmentUuid": "not-a-uuid", "flags": []}`)
	if err := resolveReferences(invalid, nil); err == nil || !strings.Contains(err.Error(), "Unable to process commands") {
		t.Errorf("got %v", err)
	}
}
