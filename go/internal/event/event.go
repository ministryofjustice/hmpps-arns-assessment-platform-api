// Package event has everything that can happen to an assessment. Events are stored as JSONB in
// the event table, identified by their Kotlin class name in the "type" property.
package event

import (
	"encoding/json"
	"fmt"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
)

type Event interface {
	// Type is the event's "type", which is also its timeline event type.
	Type() string
}

type AssessmentAnswersUpdated struct {
	Added   model.Answers `json:"added" required:"true"`
	Removed []string      `json:"removed" required:"true"`
}

type AssessmentCreated struct {
	FormVersion string        `json:"formVersion" required:"true"`
	Properties  model.Answers `json:"properties" required:"true"`
	Flags       []string      `json:"flags"`
}

type AssessmentFlagsUpdated struct {
	Flags []string `json:"flags" required:"true"`
}

type AssessmentPropertiesUpdated struct {
	Added   model.Answers `json:"added" required:"true"`
	Removed []string      `json:"removed" required:"true"`
}

type AssessmentRolledBack struct {
	RolledBackTo datetime.LocalDateTime `json:"rolledBackTo" required:"true"`
}

type AssignedToUser struct {
	UserUUID uuid.UUID `json:"userUuid" required:"true"`
}

type CollectionCreated struct {
	CollectionUUID           uuid.UUID  `json:"collectionUuid" required:"true"`
	Name                     string     `json:"name" required:"true"`
	ParentCollectionItemUUID *uuid.UUID `json:"parentCollectionItemUuid"`
}

type CollectionItemAdded struct {
	CollectionItemUUID uuid.UUID     `json:"collectionItemUuid" required:"true"`
	CollectionUUID     uuid.UUID     `json:"collectionUuid" required:"true"`
	Answers            model.Answers `json:"answers" required:"true"`
	Properties         model.Answers `json:"properties" required:"true"`
	Index              *int          `json:"index"`
}

type CollectionItemAnswersUpdated struct {
	CollectionItemUUID uuid.UUID     `json:"collectionItemUuid" required:"true"`
	Added              model.Answers `json:"added" required:"true"`
	Removed            []string      `json:"removed" required:"true"`
}

type CollectionItemPropertiesUpdated struct {
	CollectionItemUUID uuid.UUID     `json:"collectionItemUuid" required:"true"`
	Added              model.Answers `json:"added" required:"true"`
	Removed            []string      `json:"removed" required:"true"`
}

type CollectionItemRemoved struct {
	CollectionItemUUID uuid.UUID `json:"collectionItemUuid" required:"true"`
}

type CollectionItemReordered struct {
	CollectionItemUUID uuid.UUID `json:"collectionItemUuid" required:"true"`
	Index              int       `json:"index" required:"true"`
}

type FormVersionUpdated struct {
	Version string `json:"version" required:"true"`
}

// Redacted replaces an event removed by a data deletion request.
type Redacted struct {
	EventType    string                 `json:"eventType" required:"true"`
	DateRedacted datetime.LocalDateTime `json:"dateRedacted" required:"true"`
	RedactedBy   string                 `json:"redactedBy" required:"true"`
}

func (AssessmentAnswersUpdated) Type() string        { return "AssessmentAnswersUpdatedEvent" }
func (AssessmentCreated) Type() string               { return "AssessmentCreatedEvent" }
func (AssessmentFlagsUpdated) Type() string          { return "AssessmentFlagsUpdatedEvent" }
func (AssessmentPropertiesUpdated) Type() string     { return "AssessmentPropertiesUpdatedEvent" }
func (AssessmentRolledBack) Type() string            { return "AssessmentRolledBackEvent" }
func (AssignedToUser) Type() string                  { return "AssignedToUserEvent" }
func (CollectionCreated) Type() string               { return "CollectionCreatedEvent" }
func (CollectionItemAdded) Type() string             { return "CollectionItemAddedEvent" }
func (CollectionItemAnswersUpdated) Type() string    { return "CollectionItemAnswersUpdatedEvent" }
func (CollectionItemPropertiesUpdated) Type() string { return "CollectionItemPropertiesUpdatedEvent" }
func (CollectionItemRemoved) Type() string           { return "CollectionItemRemovedEvent" }
func (CollectionItemReordered) Type() string         { return "CollectionItemReorderedEvent" }
func (FormVersionUpdated) Type() string              { return "FormVersionUpdatedEvent" }
func (Redacted) Type() string                        { return "RedactedEvent" }

func orEmpty(values []string) []string {
	if values == nil {
		return []string{}
	}
	return values
}

func (e AssessmentAnswersUpdated) MarshalJSON() ([]byte, error) {
	type plain AssessmentAnswersUpdated
	e.Removed = orEmpty(e.Removed)
	return jsonx.Tag(e.Type(), plain(e))
}

func (e AssessmentCreated) MarshalJSON() ([]byte, error) {
	type plain AssessmentCreated
	e.Flags = orEmpty(e.Flags)
	return jsonx.Tag(e.Type(), plain(e))
}

func (e AssessmentFlagsUpdated) MarshalJSON() ([]byte, error) {
	type plain AssessmentFlagsUpdated
	e.Flags = orEmpty(e.Flags)
	return jsonx.Tag(e.Type(), plain(e))
}

func (e AssessmentPropertiesUpdated) MarshalJSON() ([]byte, error) {
	type plain AssessmentPropertiesUpdated
	e.Removed = orEmpty(e.Removed)
	return jsonx.Tag(e.Type(), plain(e))
}

func (e AssessmentRolledBack) MarshalJSON() ([]byte, error) {
	type plain AssessmentRolledBack
	return jsonx.Tag(e.Type(), plain(e))
}

func (e AssignedToUser) MarshalJSON() ([]byte, error) {
	type plain AssignedToUser
	return jsonx.Tag(e.Type(), plain(e))
}

func (e CollectionCreated) MarshalJSON() ([]byte, error) {
	type plain CollectionCreated
	return jsonx.Tag(e.Type(), plain(e))
}

func (e CollectionItemAdded) MarshalJSON() ([]byte, error) {
	type plain CollectionItemAdded
	return jsonx.Tag(e.Type(), plain(e))
}

func (e CollectionItemAnswersUpdated) MarshalJSON() ([]byte, error) {
	type plain CollectionItemAnswersUpdated
	e.Removed = orEmpty(e.Removed)
	return jsonx.Tag(e.Type(), plain(e))
}

func (e CollectionItemPropertiesUpdated) MarshalJSON() ([]byte, error) {
	type plain CollectionItemPropertiesUpdated
	e.Removed = orEmpty(e.Removed)
	return jsonx.Tag(e.Type(), plain(e))
}

func (e CollectionItemRemoved) MarshalJSON() ([]byte, error) {
	type plain CollectionItemRemoved
	return jsonx.Tag(e.Type(), plain(e))
}

func (e CollectionItemReordered) MarshalJSON() ([]byte, error) {
	type plain CollectionItemReordered
	return jsonx.Tag(e.Type(), plain(e))
}

func (e FormVersionUpdated) MarshalJSON() ([]byte, error) {
	type plain FormVersionUpdated
	return jsonx.Tag(e.Type(), plain(e))
}

func (e Redacted) MarshalJSON() ([]byte, error) {
	type plain Redacted
	return jsonx.Tag(e.Type(), plain(e))
}

// Unmarshal decodes an event by its "type".
func Unmarshal(data []byte) (Event, error) {
	typeName, err := jsonx.TypeOf(data)
	if err != nil {
		return nil, err
	}
	decode := func(target Event) (Event, error) {
		return target, jsonx.Unmarshal(data, target)
	}
	var decoded Event
	switch typeName {
	case "AssessmentAnswersUpdatedEvent":
		decoded, err = decode(&AssessmentAnswersUpdated{})
	case "AssessmentCreatedEvent":
		decoded, err = decode(&AssessmentCreated{})
	case "AssessmentFlagsUpdatedEvent":
		decoded, err = decode(&AssessmentFlagsUpdated{})
	case "AssessmentPropertiesUpdatedEvent":
		decoded, err = decode(&AssessmentPropertiesUpdated{})
	case "AssessmentRolledBackEvent":
		decoded, err = decode(&AssessmentRolledBack{})
	case "AssignedToUserEvent":
		decoded, err = decode(&AssignedToUser{})
	case "CollectionCreatedEvent":
		decoded, err = decode(&CollectionCreated{})
	case "CollectionItemAddedEvent":
		decoded, err = decode(&CollectionItemAdded{})
	case "CollectionItemAnswersUpdatedEvent":
		decoded, err = decode(&CollectionItemAnswersUpdated{})
	case "CollectionItemPropertiesUpdatedEvent":
		decoded, err = decode(&CollectionItemPropertiesUpdated{})
	case "CollectionItemRemovedEvent":
		decoded, err = decode(&CollectionItemRemoved{})
	case "CollectionItemReorderedEvent":
		decoded, err = decode(&CollectionItemReordered{})
	case "FormVersionUpdatedEvent":
		decoded, err = decode(&FormVersionUpdated{})
	case "RedactedEvent":
		decoded, err = decode(&Redacted{})
	default:
		return nil, fmt.Errorf("unknown event type '%s'", typeName)
	}
	if err != nil {
		return nil, err
	}
	return deref(decoded), nil
}

// deref turns the decoded pointer back into the value type that handlers switch on.
func deref(e Event) Event {
	switch e := e.(type) {
	case *AssessmentAnswersUpdated:
		return *e
	case *AssessmentCreated:
		return *e
	case *AssessmentFlagsUpdated:
		return *e
	case *AssessmentPropertiesUpdated:
		return *e
	case *AssessmentRolledBack:
		return *e
	case *AssignedToUser:
		return *e
	case *CollectionCreated:
		return *e
	case *CollectionItemAdded:
		return *e
	case *CollectionItemAnswersUpdated:
		return *e
	case *CollectionItemPropertiesUpdated:
		return *e
	case *CollectionItemRemoved:
		return *e
	case *CollectionItemReordered:
		return *e
	case *FormVersionUpdated:
		return *e
	case *Redacted:
		return *e
	}
	return e
}

// Envelope holds an event in a struct that's (un)marshalled as JSON.
type Envelope struct{ Event }

func (e Envelope) MarshalJSON() ([]byte, error) { return json.Marshal(e.Event) }

func (e *Envelope) UnmarshalJSON(data []byte) error {
	decoded, err := Unmarshal(data)
	e.Event = decoded
	return err
}

// UpdatesAggregate reports whether the assessment aggregate is derived from this event.
func UpdatesAggregate(e Event) bool {
	_, redacted := e.(Redacted)
	return !redacted
}
