package aggregate

import (
	"encoding/json"
	"slices"
	"strings"
	"testing"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/event"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/omap"
)

var created = datetime.MustParse("2025-01-01T09:00:00")

func newState(data *Assessment) *State {
	return NewState(NewEntity(uuid.New(), data, created, created, created))
}

func newEvent(data event.Event) *entity.Event {
	id := int64(1)
	user := entity.UserDetails{ID: &id, UUID: uuid.New(), UserID: "user", DisplayName: "A User", AuthSource: model.AuthSourceHmppsAuth}
	assessment := &entity.Assessment{UUID: uuid.New(), CreatedAt: created, Type: "TEST"}
	return entity.NewEvent(user, assessment, data, datetime.MustParse("2025-01-02T10:00:00"), false)
}

func timelineJSON(t *testing.T, draft *entity.TimelineDraft) string {
	t.Helper()
	out, err := json.Marshal(draft.Data)
	if err != nil {
		t.Fatal(err)
	}
	return string(out)
}

func TestAssessmentCreatedSetsInitialState(t *testing.T) {
	state := newState(&Assessment{})
	e := newEvent(event.AssessmentCreated{
		FormVersion: "1.0", Properties: omap.Of[model.Value]("status", model.Single("NEW")), Flags: []string{"FLAG"},
	})

	draft, err := Apply(e, state, clock.System())
	if err != nil {
		t.Fatal(err)
	}

	data := state.Latest().Data
	if data.FormVersion != "1.0" || !slices.Equal(data.Flags, []string{"FLAG"}) || !slices.Contains(data.Collaborators, e.User.UUID) {
		t.Errorf("unexpected state %+v", data)
	}
	if state.Latest().EventsApplied != 1 || state.Latest().EventsTo != e.CreatedAt {
		t.Error("expected the event to be recorded as applied")
	}
	if draft.EventType != "AssessmentCreatedEvent" || timelineJSON(t, draft) != `{"formVersion":"1.0","properties":["status"]}` {
		t.Errorf("got %s %s", draft.EventType, timelineJSON(t, draft))
	}
}

func TestRemovingAnUnknownAnswerFails(t *testing.T) {
	_, err := Apply(newEvent(event.AssessmentAnswersUpdated{Removed: []string{"missing"}}), newState(&Assessment{}), clock.System())

	if err == nil || !strings.Contains(apperr.DeveloperMessage(err), `The answer "missing" could not be found`) {
		t.Errorf("got %v", err)
	}
}

func TestAFullSnapshotStartsANewOne(t *testing.T) {
	state := newState(&Assessment{})
	state.Aggregates[0].EventsApplied = SnapshotThreshold

	if _, err := Apply(newEvent(event.FormVersionUpdated{Version: "2.0"}), state, clock.System()); err != nil {
		t.Fatal(err)
	}

	if len(state.Aggregates) != 2 || state.Aggregates[0].Data.FormVersion != "" || state.Latest().Data.FormVersion != "2.0" {
		t.Error("expected the change in a new snapshot only")
	}
	if state.Latest().EventsApplied != 1 || state.Latest().EventsFrom != state.Aggregates[0].EventsFrom {
		t.Error("expected the new snapshot to continue from the old one")
	}
}

func TestCollectionItemsAreAddedAtAnIndexIntoNestedCollections(t *testing.T) {
	goal, notes, newNote := uuid.New(), uuid.New(), uuid.New()
	data := &Assessment{Collections: []*model.Collection{{
		UUID: uuid.New(), Name: "GOALS",
		Items: []*model.CollectionItem{{UUID: goal, Collections: []*model.Collection{{
			UUID: notes, Name: "NOTES", Items: []*model.CollectionItem{{UUID: uuid.New()}},
		}}}},
	}}}
	state := newState(data)
	index := 0

	draft, err := Apply(newEvent(event.CollectionItemAdded{CollectionItemUUID: newNote, CollectionUUID: notes, Index: &index}), state, clock.System())
	if err != nil {
		t.Fatal(err)
	}

	added := state.Latest().Data.GetCollection(notes)
	if len(added.Items) != 2 || added.Items[0].UUID != newNote {
		t.Error("expected the note first")
	}
	if !strings.HasPrefix(timelineJSON(t, draft), `{"index":0,"collection":"NOTES"`) {
		t.Errorf("got %s", timelineJSON(t, draft))
	}
}

func TestAssigningAUserRecordsThemOnTheTimeline(t *testing.T) {
	state := newState(&Assessment{})
	assignee := uuid.New()

	draft, err := Apply(newEvent(event.AssignedToUser{UserUUID: assignee}), state, clock.System())
	if err != nil {
		t.Fatal(err)
	}

	if *state.Latest().Data.AssignedUser != assignee || len(state.Latest().Data.Collaborators) != 0 {
		t.Error("expected only the assignee to change")
	}
	var assigned jsonx.Object
	_ = json.Unmarshal([]byte(timelineJSON(t, draft)), &assigned)
	raw, _ := assigned.Get("assignee")
	if !strings.Contains(string(raw), `"displayName":"A User"`) {
		t.Errorf("got %s", raw)
	}
}

func TestAggregatesRoundTripThroughJSONWithTheirType(t *testing.T) {
	out, _ := json.Marshal(&Assessment{FormVersion: "1"})
	if !strings.HasPrefix(string(out), `{"type":"AssessmentAggregate","formVersion":"1"`) || !strings.Contains(string(out), `"flags":[]`) {
		t.Errorf("got %s", out)
	}
	var decoded Assessment
	if err := json.Unmarshal(out, &decoded); err != nil || decoded.FormVersion != "1" {
		t.Errorf("got %+v, %v", decoded, err)
	}
}
