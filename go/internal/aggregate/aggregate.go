// Package aggregate has the current state of an assessment, derived by replaying its events, and
// the snapshots it's persisted as so reads don't need to replay the whole event stream.
package aggregate

import (
	"encoding/json"
	"fmt"
	"slices"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
)

// TypeName is the aggregate's "type", which the data_type column is generated from.
const TypeName = "AssessmentAggregate"

// SnapshotThreshold is how many events a snapshot takes before the next goes in a new one.
const SnapshotThreshold = 50

type Assessment struct {
	FormVersion   string              `json:"formVersion"`
	AssignedUser  *uuid.UUID          `json:"assignedUser"`
	Properties    model.Answers       `json:"properties"`
	Answers       model.Answers       `json:"answers"`
	Collections   []*model.Collection `json:"collections"`
	Collaborators []uuid.UUID         `json:"collaborators"`
	Flags         []string            `json:"flags"`
}

func (a Assessment) MarshalJSON() ([]byte, error) {
	type plain Assessment
	if a.Collections == nil {
		a.Collections = []*model.Collection{}
	}
	if a.Collaborators == nil {
		a.Collaborators = []uuid.UUID{}
	}
	if a.Flags == nil {
		a.Flags = []string{}
	}
	return jsonx.Tag(TypeName, plain(a))
}

func (a *Assessment) UnmarshalJSON(data []byte) error {
	typeName, err := jsonx.TypeOf(data)
	if err != nil {
		return err
	}
	if typeName != TypeName {
		return fmt.Errorf("unknown aggregate type '%s'", typeName)
	}
	type plain Assessment
	return json.Unmarshal(data, (*plain)(a))
}

func (a *Assessment) Clone() *Assessment {
	clone := *a
	clone.Properties = a.Properties.Clone()
	clone.Answers = a.Answers.Clone()
	clone.Collections = make([]*model.Collection, len(a.Collections))
	for i, collection := range a.Collections {
		clone.Collections[i] = collection.Clone()
	}
	clone.Collaborators = slices.Clone(a.Collaborators)
	clone.Flags = slices.Clone(a.Flags)
	return &clone
}

func (a *Assessment) AddCollaborator(user uuid.UUID) {
	if !slices.Contains(a.Collaborators, user) {
		a.Collaborators = append(a.Collaborators, user)
	}
}

func (a *Assessment) GetCollection(id uuid.UUID) *model.Collection {
	for _, collection := range a.Collections {
		if collection.UUID == id {
			return collection
		}
	}
	for _, collection := range a.Collections {
		for _, item := range collection.Items {
			if found := item.FindCollection(id); found != nil {
				return found
			}
		}
	}
	return nil
}

// GetCollectionWithItem finds the collection, at any depth, that directly contains the item.
func (a *Assessment) GetCollectionWithItem(itemID uuid.UUID) *model.Collection {
	for _, collection := range a.Collections {
		if collection.IndexOf(itemID) != -1 {
			return collection
		}
		for _, item := range collection.Items {
			if found := item.FindCollectionWithItem(itemID); found != nil {
				return found
			}
		}
	}
	return nil
}

func (a *Assessment) GetCollectionItem(itemID uuid.UUID) *model.CollectionItem {
	for _, collection := range a.Collections {
		if found := collection.FindItem(itemID); found != nil {
			return found
		}
	}
	return nil
}

// Entity is a snapshot of the aggregate.
type Entity struct {
	ID             *int64
	UUID           uuid.UUID
	Position       *int32
	Version        int64 // for optimistic locking
	UpdatedAt      datetime.LocalDateTime
	EventsFrom     datetime.LocalDateTime
	EventsTo       datetime.LocalDateTime
	EventsApplied  int64
	AssessmentUUID uuid.UUID
	Data           *Assessment
}

func NewEntity(assessmentUUID uuid.UUID, data *Assessment, eventsFrom, eventsTo, updatedAt datetime.LocalDateTime) *Entity {
	return &Entity{
		UUID: uuid.New(), UpdatedAt: updatedAt, EventsFrom: eventsFrom, EventsTo: eventsTo,
		AssessmentUUID: assessmentUUID, Data: data,
	}
}

// Snapshot starts a new, unsaved snapshot from this one's data.
func (e *Entity) Snapshot(c clock.Clock) *Entity {
	return NewEntity(e.AssessmentUUID, e.Data.Clone(), e.EventsFrom, e.EventsTo, c.Now())
}

// Applied records that an event has been applied.
func (e *Entity) Applied(eventsTo datetime.LocalDateTime, c clock.Clock) {
	e.EventsTo = eventsTo
	e.UpdatedAt = c.Now()
	e.EventsApplied++
}

type DTO struct {
	UUID                  uuid.UUID              `json:"uuid"`
	Position              int32                  `json:"position"`
	UpdatedAt             datetime.LocalDateTime `json:"updatedAt"`
	EventsFrom            datetime.LocalDateTime `json:"eventsFrom"`
	EventsTo              datetime.LocalDateTime `json:"eventsTo"`
	NumberOfEventsApplied int64                  `json:"numberOfEventsApplied"`
	AssessmentUUID        uuid.UUID              `json:"assessmentUuid"`
	Data                  *Assessment            `json:"data"`
}

func (e *Entity) DTO() (DTO, error) {
	if e.Position == nil {
		return DTO{}, apperr.UndefinedPosition(fmt.Sprintf("Aggregate with UUID: %s does not have a valid position", e.UUID))
	}
	return DTO{
		UUID: e.UUID, Position: *e.Position, UpdatedAt: e.UpdatedAt, EventsFrom: e.EventsFrom, EventsTo: e.EventsTo,
		NumberOfEventsApplied: e.EventsApplied, AssessmentUUID: e.AssessmentUUID, Data: e.Data,
	}, nil
}

// State is the snapshots of one assessment loaded or created in a unit of work. The last one is
// the latest.
type State struct {
	Aggregates []*Entity
}

func NewState(latest *Entity) *State { return &State{Aggregates: []*Entity{latest}} }

func (s *State) Latest() *Entity { return s.Aggregates[len(s.Aggregates)-1] }

// ForWrite is the snapshot to apply the next event to, starting a new one if the latest is full.
func (s *State) ForWrite(c clock.Clock) *Entity {
	if s.Latest().EventsApplied >= SnapshotThreshold {
		s.Aggregates = append(s.Aggregates, s.Latest().Snapshot(c))
	}
	return s.Latest()
}
