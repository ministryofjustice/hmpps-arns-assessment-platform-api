package engine

import (
	"context"
	"fmt"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/aggregate"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/store"
)

// FetchOrCreateState returns the state of the aggregate at the point in time, or the latest
// state when there isn't one.
func FetchOrCreateState(
	ctx context.Context, db store.DB, c clock.Clock, assessment *entity.Assessment, pointInTime *datetime.LocalDateTime,
) (*aggregate.State, error) {
	if pointInTime == nil {
		latest, err := store.FindLatestAggregateUpTo(ctx, db, assessment.UUID, c.Now())
		if err != nil || latest != nil {
			return stateOf(latest), err
		}
		return stateFromEvents(ctx, db, c, assessment, c.Now())
	}

	normalised := pointInTime.ToDatabasePrecision()
	if normalised.Before(assessment.CreatedAt) {
		return nil, apperr.InvalidTimestamp(normalised, "Timestamp cannot be before the assessment created date")
	}
	return stateFromSnapshot(ctx, db, c, assessment, normalised)
}

func stateOf(snapshot *aggregate.Entity) *aggregate.State {
	if snapshot == nil {
		return nil
	}
	return aggregate.NewState(snapshot)
}

// stateFromSnapshot starts from the latest snapshot before the point in time and replays any
// events since.
func stateFromSnapshot(
	ctx context.Context, db store.DB, c clock.Clock, assessment *entity.Assessment, pointInTime datetime.LocalDateTime,
) (*aggregate.State, error) {
	snapshot, err := store.FindLatestAggregateUpTo(ctx, db, assessment.UUID, pointInTime)
	if err != nil {
		return nil, err
	}
	if snapshot == nil {
		return stateFromEvents(ctx, db, c, assessment, pointInTime)
	}
	state := aggregate.NewState(snapshot)
	if snapshot.EventsTo == pointInTime {
		return state, nil
	}

	events, err := store.FindEventsBetween(ctx, db, assessment, snapshot.EventsTo, pointInTime)
	if err != nil || len(events) == 0 {
		return state, err
	}

	replay := NewUnitOfWork()
	replay.setState(assessment.UUID, state)
	if err := HandleEvents(ctx, db, c, replay, events); err != nil {
		return nil, err
	}
	return replay.state[assessment.UUID], nil
}

func stateFromEvents(
	ctx context.Context, db store.DB, c clock.Clock, assessment *entity.Assessment, pointInTime datetime.LocalDateTime,
) (*aggregate.State, error) {
	return RebuildFromEvents(ctx, db, c, assessment, &pointInTime)
}

// RebuildFromEvents replays every event up to the point in time (or now) onto a blank aggregate.
func RebuildFromEvents(
	ctx context.Context, db store.DB, c clock.Clock, assessment *entity.Assessment, pointInTime *datetime.LocalDateTime,
) (*aggregate.State, error) {
	upTo := c.Now()
	if pointInTime != nil {
		upTo = *pointInTime
	}
	events, err := store.FindEventsUpTo(ctx, db, assessment, upTo)
	if err != nil {
		return nil, err
	}

	replay := NewUnitOfWork()
	replay.setState(assessment.UUID, blankState(c, assessment))
	if err := HandleEvents(ctx, db, c, replay, events); err != nil {
		return nil, err
	}
	return replay.state[assessment.UUID], nil
}

func blankState(c clock.Clock, assessment *entity.Assessment) *aggregate.State {
	return aggregate.NewState(aggregate.NewEntity(
		assessment.UUID, &aggregate.Assessment{}, assessment.CreatedAt, assessment.CreatedAt, c.Now()))
}

// AssessmentState is the state to persist for one assessment.
type AssessmentState struct {
	AssessmentUUID uuid.UUID
	State          *aggregate.State
}

// PersistState saves new snapshots and updates changed ones. It returns the assessments whose
// cached latest version is now stale.
func PersistState(ctx context.Context, db store.DB, states []AssessmentState) ([]uuid.UUID, error) {
	evicted := make([]uuid.UUID, 0, len(states))
	for _, assessmentState := range states {
		maxPosition, err := store.FindMaxAggregatePosition(ctx, db, assessmentState.AssessmentUUID)
		if err != nil {
			return nil, err
		}
		next := int32(0)
		if maxPosition != nil {
			next = *maxPosition + 1
		}
		for index, snapshot := range assessmentState.State.Aggregates {
			if snapshot.ID != nil {
				if err := store.UpdateAggregate(ctx, db, snapshot); err != nil {
					return nil, err
				}
				continue
			}
			position := next + int32(index)
			snapshot.Position = &position
			id, err := store.InsertAggregate(ctx, db, snapshot)
			if err != nil {
				return nil, fmt.Errorf("saving aggregate: %w", err)
			}
			snapshot.ID = &id
		}
		evicted = append(evicted, assessmentState.AssessmentUUID)
	}
	return evicted, nil
}

// RebuildState replaces the assessment's snapshots with ones rebuilt from its remaining events.
func RebuildState(
	ctx context.Context, db store.DB, c clock.Clock, assessment *entity.Assessment, pointInTime *datetime.LocalDateTime,
) (*aggregate.State, []uuid.UUID, error) {
	if err := store.DeleteAggregates(ctx, db, assessment.UUID); err != nil {
		return nil, nil, err
	}
	rebuilt, err := RebuildFromEvents(ctx, db, c, assessment, pointInTime)
	if err != nil {
		return nil, nil, err
	}
	evicted, err := PersistState(ctx, db, []AssessmentState{{assessment.UUID, rebuilt}})
	return rebuilt, evicted, err
}
