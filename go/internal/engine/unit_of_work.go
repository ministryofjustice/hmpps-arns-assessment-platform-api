// Package engine applies events to aggregates and saves the results: the event-sourcing core
// that commands, queries and data deletion share.
package engine

import (
	"context"
	"encoding/json"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/aggregate"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/store"
)

// UnitOfWork is everything loaded or created while handling a batch of commands, written to the
// database in one go by Persist.
type UnitOfWork struct {
	state       map[uuid.UUID]*aggregate.State
	stateOrder  []uuid.UUID
	Events      []*entity.Event
	Timeline    []*entity.Timeline
	users       []*entity.UserDetails
	assessments []*entity.Assessment
	// Evictions are the assessments whose cached latest version is stale once the transaction commits.
	Evictions []uuid.UUID
}

func NewUnitOfWork() *UnitOfWork {
	return &UnitOfWork{state: map[uuid.UUID]*aggregate.State{}}
}

// AddAssessment records a new assessment, to be saved by Persist.
func (u *UnitOfWork) AddAssessment(assessment *entity.Assessment) {
	u.assessments = append(u.assessments, assessment)
}

func (u *UnitOfWork) FindAssessment(ctx context.Context, db store.DB, id uuid.UUID) (*entity.Assessment, error) {
	for _, assessment := range u.assessments {
		if assessment.UUID == id {
			return assessment, nil
		}
	}
	found, err := FindAssessment(ctx, db, id)
	if err != nil {
		return nil, err
	}
	u.assessments = append(u.assessments, found)
	return found, nil
}

// FindUser returns the user's existing details, or new details that Persist will save.
func (u *UnitOfWork) FindUser(ctx context.Context, db store.DB, user model.UserDetails) (entity.UserDetails, error) {
	for _, existing := range u.users {
		if existing.UserID == user.ID && existing.AuthSource == user.AuthSource {
			return *existing, nil
		}
	}
	found, err := store.FindUserByUserIDAndAuthSource(ctx, db, user.ID, user.AuthSource)
	if err != nil {
		return entity.UserDetails{}, err
	}
	if found == nil {
		found = &entity.UserDetails{UUID: uuid.New(), UserID: user.ID, DisplayName: user.Name, AuthSource: user.AuthSource}
	}
	u.users = append(u.users, found)
	return *found, nil
}

// State returns the aggregate state for the assessment, loading (or building) it the first time.
func (u *UnitOfWork) State(
	ctx context.Context, db store.DB, c clock.Clock, assessment *entity.Assessment, pointInTime *datetime.LocalDateTime,
) (*aggregate.State, error) {
	if state, ok := u.state[assessment.UUID]; ok {
		return state, nil
	}
	state, err := FetchOrCreateState(ctx, db, c, assessment, pointInTime)
	if err != nil {
		return nil, err
	}
	u.setState(assessment.UUID, state)
	return state, nil
}

func (u *UnitOfWork) setState(assessmentUUID uuid.UUID, state *aggregate.State) {
	if _, exists := u.state[assessmentUUID]; !exists {
		u.stateOrder = append(u.stateOrder, assessmentUUID)
	}
	u.state[assessmentUUID] = state
}

// AddTimeline completes the timeline entries events produced with the command's custom timeline.
func (u *UnitOfWork) AddTimeline(drafts []*entity.TimelineDraft, custom *model.Timeline) {
	for _, draft := range drafts {
		u.Timeline = append(u.Timeline, draft.Complete(custom))
	}
}

func (u *UnitOfWork) Persist(ctx context.Context, db store.DB) error {
	newUserIDs := map[uuid.UUID]int64{}
	for _, user := range u.users {
		if user.ID != nil {
			continue
		}
		id, err := store.InsertUser(ctx, db, user)
		if err != nil {
			return err
		}
		user.ID = &id
		newUserIDs[user.UUID] = id
	}
	u.fillInAssigneeIDs(newUserIDs)

	for _, assessment := range u.assessments {
		if assessment.ID == nil {
			if err := store.InsertAssessment(ctx, db, assessment); err != nil {
				return err
			}
		}
	}
	if err := SaveEvents(ctx, db, u.Events); err != nil {
		return err
	}
	evicted, err := PersistState(ctx, db, u.orderedState())
	if err != nil {
		return err
	}
	u.Evictions = append(u.Evictions, evicted...)
	if err := SaveTimeline(ctx, db, u.Timeline); err != nil {
		return err
	}

	u.users, u.assessments, u.Events, u.Timeline = nil, nil, nil, nil
	u.state, u.stateOrder = map[uuid.UUID]*aggregate.State{}, nil
	return nil
}

func (u *UnitOfWork) orderedState() []AssessmentState {
	states := make([]AssessmentState, 0, len(u.stateOrder))
	for _, id := range u.stateOrder {
		states = append(states, AssessmentState{AssessmentUUID: id, State: u.state[id]})
	}
	return states
}

// fillInAssigneeIDs: assignment timeline entries embed the assigned user's details, and a user
// created in this unit of work only has an id once saved, so fill it in to record the same
// details as the Kotlin service did.
func (u *UnitOfWork) fillInAssigneeIDs(newUserIDs map[uuid.UUID]int64) {
	if len(newUserIDs) == 0 {
		return
	}
	for _, entry := range u.Timeline {
		raw, ok := entry.Data.Get("assignee")
		if !ok {
			continue
		}
		var assignee jsonx.Object
		var identified struct {
			UUID uuid.UUID `json:"uuid"`
		}
		if json.Unmarshal(raw, &assignee) != nil || json.Unmarshal(raw, &identified) != nil {
			continue
		}
		if id, ok := newUserIDs[identified.UUID]; ok {
			encodedID, _ := json.Marshal(id)
			assignee.Set("id", encodedID)
			updated, _ := json.Marshal(assignee)
			entry.Data.Set("assignee", updated)
		}
	}
}

// FindAssessment fails with a not found error when there's no such assessment.
func FindAssessment(ctx context.Context, db store.DB, id uuid.UUID) (*entity.Assessment, error) {
	found, err := store.FindAssessment(ctx, db, id)
	if err == nil && found == nil {
		err = apperr.AssessmentNotFoundByUUID(id)
	}
	return found, err
}
