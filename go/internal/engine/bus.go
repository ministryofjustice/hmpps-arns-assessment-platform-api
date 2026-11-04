package engine

import (
	"context"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/aggregate"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/event"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/store"
)

// HandleEvent applies an event to the aggregate and records it in the unit of work. It returns
// the timeline entries the event produced, for the caller to complete with any custom timeline.
func HandleEvent(ctx context.Context, db store.DB, c clock.Clock, u *UnitOfWork, e *entity.Event) ([]*entity.TimelineDraft, error) {
	var drafts []*entity.TimelineDraft
	if event.UpdatesAggregate(e.Data) {
		state, err := u.State(ctx, db, c, e.Assessment, &e.CreatedAt)
		if err != nil {
			return nil, err
		}
		draft, err := apply(ctx, db, c, e, state)
		if err != nil {
			return nil, apperr.EventHandling(e.UUID, e.Data.Type(), err)
		}
		drafts = append(drafts, draft)
	}
	u.Events = append(u.Events, e)
	return drafts, nil
}

func HandleEvents(ctx context.Context, db store.DB, c clock.Clock, u *UnitOfWork, events []*entity.Event) error {
	for _, e := range events {
		if _, err := HandleEvent(ctx, db, c, u, e); err != nil {
			return err
		}
	}
	return nil
}

func apply(ctx context.Context, db store.DB, c clock.Clock, e *entity.Event, state *aggregate.State) (*entity.TimelineDraft, error) {
	rolledBack, ok := e.Data.(event.AssessmentRolledBack)
	if !ok {
		return aggregate.Apply(e, state, c)
	}

	// Restoring the earlier state replays events, which may include rollbacks.
	state.ForWrite(c)
	previous, err := FetchOrCreateState(ctx, db, c, e.Assessment, &rolledBack.RolledBackTo)
	if err != nil {
		return nil, err
	}
	restored := previous.ForWrite(c).Data.Clone()

	snapshot := state.ForWrite(c)
	snapshot.Data = restored
	snapshot.Data.AddCollaborator(e.User.UUID)
	snapshot.Applied(e.CreatedAt, c)
	return entity.NewTimelineDraft(e, jsonx.ObjectOf("rolledBackTo", rolledBack.RolledBackTo)), nil
}

// SaveEvents inserts new events, giving those without a position the next positions for their
// assessment.
func SaveEvents(ctx context.Context, db store.DB, events []*entity.Event) error {
	type counter struct{ max, seen int32 }
	counters := map[uuid.UUID]*counter{}
	for _, e := range events {
		count, ok := counters[e.Assessment.UUID]
		if !ok {
			maxPosition, err := store.FindMaxEventPosition(ctx, db, e.Assessment.UUID)
			if err != nil {
				return err
			}
			count = &counter{max: -1}
			if maxPosition != nil {
				count.max = *maxPosition
			}
			counters[e.Assessment.UUID] = count
		}
		if e.Position == nil {
			position := count.max + 1 + count.seen
			e.Position = &position
		}
		count.seen++
	}
	for _, e := range events {
		if err := store.InsertEvent(ctx, db, e); err != nil {
			return err
		}
	}
	return nil
}

// SaveTimeline inserts timeline entries after the existing entries for their assessment. Unlike
// events, any position an entry already had is replaced.
func SaveTimeline(ctx context.Context, db store.DB, entries []*entity.Timeline) error {
	next := map[uuid.UUID]int32{}
	for _, entry := range entries {
		position, ok := next[entry.AssessmentUUID]
		if !ok {
			maxPosition, err := store.FindMaxTimelinePosition(ctx, db, entry.AssessmentUUID)
			if err != nil {
				return err
			}
			if maxPosition != nil {
				position = *maxPosition + 1
			}
		}
		entry.Position = &position
		next[entry.AssessmentUUID] = position + 1
	}
	for _, entry := range entries {
		if err := store.InsertTimeline(ctx, db, entry); err != nil {
			return err
		}
	}
	return nil
}

// Undelete restores events deleted from the point in time onwards. It's only allowed when no
// events have been written since, because those were built without the deleted ones.
func Undelete(ctx context.Context, db store.DB, assessmentUUID uuid.UUID, from datetime.LocalDateTime) error {
	deleted, err := store.FindDeletedEventsFrom(ctx, db, assessmentUUID, from)
	if err != nil || len(deleted) == 0 {
		return err
	}
	first := deleted[0].Position
	ids := make([]uuid.UUID, 0, len(deleted))
	for _, d := range deleted {
		first = min(first, d.Position)
		ids = append(ids, d.UUID)
	}
	newer, err := store.EventsExistAfterPosition(ctx, db, assessmentUUID, first)
	if err != nil {
		return err
	}
	if newer {
		return apperr.UndeleteNotAtTail(assessmentUUID, from)
	}
	return store.UndeleteEvents(ctx, db, ids)
}
