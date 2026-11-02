// Package entity has the rows the service reads and writes, other than aggregate snapshots.
package entity

import (
	"fmt"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/event"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/omap"
)

// UserDetails is serialised as-is into the timeline when a user is assigned to an assessment.
type UserDetails struct {
	ID          *int64           `json:"id"`
	UUID        uuid.UUID        `json:"uuid"`
	UserID      string           `json:"userId"`
	DisplayName string           `json:"displayName"`
	AuthSource  model.AuthSource `json:"authSource"`
}

func (u *UserDetails) User() model.User {
	if u == nil {
		return model.UnknownUser()
	}
	userID, authSource := u.UserID, u.AuthSource
	return model.User{ID: u.UUID, Name: u.DisplayName, UserID: &userID, AuthSource: &authSource}
}

type Assessment struct {
	ID          *int64
	UUID        uuid.UUID
	CreatedAt   datetime.LocalDateTime
	Type        string
	Identifiers []AssessmentIdentifier
}

// IdentifiersMap is keyed by identifier type; a later identifier of a type replaces an earlier one.
func (a *Assessment) IdentifiersMap() omap.Map[string] {
	var identifiers omap.Map[string]
	for _, identifier := range a.Identifiers {
		identifiers.Set(string(identifier.Pair.Type), identifier.Pair.ID)
	}
	return identifiers
}

type AssessmentIdentifier struct {
	ID        *int64
	UUID      uuid.UUID
	CreatedAt datetime.LocalDateTime
	Pair      model.IdentifierPair
}

type Event struct {
	ID         *int64
	UUID       uuid.UUID
	Position   *int32
	CreatedAt  datetime.LocalDateTime
	User       UserDetails
	Assessment *Assessment
	Data       event.Event
	Autosaved  bool
	Deleted    bool
}

func NewEvent(user UserDetails, assessment *Assessment, data event.Event, createdAt datetime.LocalDateTime, autosaved bool) *Event {
	return &Event{UUID: uuid.New(), CreatedAt: createdAt, User: user, Assessment: assessment, Data: data, Autosaved: autosaved}
}

type EventDTO struct {
	UUID      uuid.UUID              `json:"uuid"`
	CreatedAt datetime.LocalDateTime `json:"createdAt"`
	Position  int32                  `json:"position"`
	Data      event.Envelope         `json:"data"`
	Autosaved bool                   `json:"autosaved"`
}

func (e *Event) DTO() (EventDTO, error) {
	if e.Position == nil {
		return EventDTO{}, apperr.UndefinedPosition(fmt.Sprintf("Event with UUID: %s does not have a valid position", e.UUID))
	}
	return EventDTO{UUID: e.UUID, CreatedAt: e.CreatedAt, Position: *e.Position, Data: event.Envelope{Event: e.Data}, Autosaved: e.Autosaved}, nil
}

type Timeline struct {
	ID             *int64
	UUID           uuid.UUID
	Position       *int32
	CreatedAt      datetime.LocalDateTime
	User           UserDetails
	AssessmentUUID uuid.UUID
	EventType      *string
	Data           jsonx.Object
	CustomType     *string
	CustomData     *jsonx.Object
	Deleted        bool
}

// CustomTimeline is a timeline entry that isn't tied to an event.
func CustomTimeline(createdAt datetime.LocalDateTime, user UserDetails, assessmentUUID uuid.UUID, timeline *model.Timeline) *Timeline {
	customType, customData := timeline.Type, timeline.Data.Clone()
	return &Timeline{
		UUID: uuid.New(), CreatedAt: createdAt, User: user, AssessmentUUID: assessmentUUID,
		CustomType: &customType, CustomData: &customData,
	}
}

func (t *Timeline) Item() (model.TimelineItem, error) {
	if t.Position == nil {
		return model.TimelineItem{}, apperr.UndefinedPosition(fmt.Sprintf("Timeline item with UUID: %s does not have a valid position", t.UUID))
	}
	return model.TimelineItem{
		UUID: t.UUID, Position: *t.Position, Timestamp: t.CreatedAt, User: t.User.User(), Assessment: t.AssessmentUUID,
		Event: t.EventType, Data: t.Data, CustomType: t.CustomType, CustomData: t.CustomData,
	}, nil
}

// TimelineDraft is the timeline entry an event produces, completed with the custom timeline (if
// any) supplied by the command that raised the event.
type TimelineDraft struct {
	CreatedAt      datetime.LocalDateTime
	User           UserDetails
	AssessmentUUID uuid.UUID
	EventType      string
	Data           jsonx.Object
}

func NewTimelineDraft(e *Event, data jsonx.Object) *TimelineDraft {
	return &TimelineDraft{CreatedAt: e.CreatedAt, User: e.User, AssessmentUUID: e.Assessment.UUID, EventType: e.Data.Type(), Data: data}
}

func (d *TimelineDraft) Complete(custom *model.Timeline) *Timeline {
	eventType := d.EventType
	entry := &Timeline{
		UUID: uuid.New(), CreatedAt: d.CreatedAt, User: d.User, AssessmentUUID: d.AssessmentUUID,
		EventType: &eventType, Data: d.Data.Clone(),
	}
	if custom != nil {
		customType, customData := custom.Type, custom.Data.Clone()
		entry.CustomType, entry.CustomData = &customType, &customData
	}
	return entry
}
