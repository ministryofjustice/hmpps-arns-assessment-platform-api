// Package datadeletion rewrites an assessment's history, to remove data that should never have
// been recorded.
package datadeletion

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strings"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/aggregate"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/engine"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/event"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/store"
)

type Operation string

const (
	OperationUpdate Operation = "UPDATE"
	OperationDelete Operation = "DELETE"
)

func (o *Operation) UnmarshalJSON(data []byte) error {
	var raw string
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	if raw != string(OperationUpdate) && raw != string(OperationDelete) {
		return fmt.Errorf("unknown operation '%s'", raw)
	}
	*o = Operation(raw)
	return nil
}

type Request struct {
	Events   []EventUpdate    `json:"events"`
	Timeline []TimelineUpdate `json:"timeline"`
	// DryRun defaults to true: nothing is saved unless it's explicitly false.
	DryRun bool `json:"dryRun"`
}

func (r *Request) UnmarshalJSON(data []byte) error {
	type plain Request
	decoded := plain{DryRun: true}
	if err := jsonx.Unmarshal(data, &decoded); err != nil {
		return err
	}
	*r = Request(decoded)
	return nil
}

type EventUpdate struct {
	UUID      uuid.UUID `json:"uuid" required:"true"`
	Operation Operation `json:"operation" required:"true"`
	// Event is the replacement for an update. For a delete, only its type is kept.
	Event event.Envelope `json:"event" required:"true"`
}

type TimelineUpdate struct {
	UUID      uuid.UUID          `json:"uuid" required:"true"`
	Operation Operation          `json:"operation" required:"true"`
	Timeline  model.TimelineItem `json:"timeline" required:"true"`
}

type DataResponse struct {
	Events   []entity.EventDTO    `json:"events"`
	Timeline []model.TimelineItem `json:"timeline"`
}

type Response struct {
	Success bool `json:"success"`
	DryRun  bool `json:"dryRun"`
	// Exception is why the rebuilt history couldn't be replayed, when Success is false.
	Exception *EventHandlingFailure `json:"exception"`
	// RebuiltState is the aggregates rebuilt from the updated events, by aggregate type.
	RebuiltState *jsonx.Object `json:"rebuiltState"`
}

type EventHandlingFailure struct {
	EventUUID        uuid.UUID `json:"eventUuid"`
	EventName        string    `json:"eventName"`
	HandlerName      string    `json:"handlerName"`
	Message          string    `json:"message"`
	DeveloperMessage string    `json:"developerMessage"`
	StatusCode       string    `json:"statusCode"`
}

// Auditor records who did what.
type Auditor interface {
	Audit(who, what, details string)
}

// Evictor removes assessments' cached latest versions.
type Evictor interface {
	EvictLatest(assessmentUUID uuid.UUID)
}

type Service struct {
	Pool  *pgxpool.Pool
	Audit Auditor
	Cache Evictor
}

// Data is every event and timeline entry for the assessment, including deleted ones.
func (s *Service) Data(ctx context.Context, assessmentUUID uuid.UUID) (*DataResponse, error) {
	response := &DataResponse{Events: []entity.EventDTO{}, Timeline: []model.TimelineItem{}}
	assessment, err := store.FindAssessment(ctx, s.Pool, assessmentUUID)
	if err != nil || assessment == nil {
		return response, err
	}

	events, err := store.FindAllEventsIncludingDeleted(ctx, s.Pool, assessment)
	if err != nil {
		return nil, err
	}
	for _, e := range events {
		dto, err := e.DTO()
		if err != nil {
			return nil, err
		}
		response.Events = append(response.Events, dto)
	}
	entries, err := store.FindAllTimelineIncludingDeleted(ctx, s.Pool, assessmentUUID)
	if err != nil {
		return nil, err
	}
	for _, entry := range entries {
		item, err := entry.Item()
		if err != nil {
			return nil, err
		}
		response.Timeline = append(response.Timeline, item)
	}
	return response, nil
}

// errRollback ends a transaction without saving, keeping the response built inside it.
var errRollback = errors.New("rolled back")

// Update replaces or redacts events and timeline entries, then rebuilds the aggregates from the
// result. A dry run makes the same changes but rolls them back, to preview the rebuilt aggregates.
func (s *Service) Update(ctx context.Context, c clock.Clock, principal string, assessmentUUID uuid.UUID, request Request) (*Response, error) {
	response := &Response{Success: true, DryRun: request.DryRun}
	var evicted []uuid.UUID
	var updatedEvents, updatedTimeline []uuid.UUID

	err := pgx.BeginFunc(ctx, s.Pool, func(tx pgx.Tx) error {
		assessment, err := engine.FindAssessment(ctx, tx, assessmentUUID)
		if err != nil {
			return err
		}

		replacementTimeline, existingTimeline, err := replaceTimeline(ctx, tx, request.Timeline)
		if err != nil {
			return err
		}
		replacementEvents, existingEvents, err := replaceEvents(ctx, tx, c, principal, request.Events)
		if err != nil {
			return err
		}
		updatedEvents, updatedTimeline = existingEvents, existingTimeline

		if err := store.DeleteTimeline(ctx, tx, existingTimeline); err != nil {
			return err
		}
		if err := engine.SaveTimeline(ctx, tx, replacementTimeline); err != nil {
			return err
		}
		if err := store.DeleteEvents(ctx, tx, existingEvents); err != nil {
			return err
		}
		if err := engine.SaveEvents(ctx, tx, replacementEvents); err != nil {
			return err
		}

		if len(replacementEvents) > 0 {
			var rebuilt *aggregate.State
			rebuilt, evicted, err = engine.RebuildState(ctx, tx, c, assessment, nil)
			var failure *apperr.EventHandlingError
			if errors.As(err, &failure) {
				response.Success, response.Exception = false, &EventHandlingFailure{
					EventUUID: failure.EventUUID, EventName: failure.EventName, HandlerName: failure.HandlerName,
					Message: failure.Error(), DeveloperMessage: apperr.DeveloperMessage(failure.Cause), StatusCode: "BAD_REQUEST",
				}
				return errRollback
			}
			if err != nil {
				return err
			}
			dtos := make([]aggregate.DTO, 0, len(rebuilt.Aggregates))
			for _, snapshot := range rebuilt.Aggregates {
				dto, err := snapshot.DTO()
				if err != nil {
					return err
				}
				dtos = append(dtos, dto)
			}
			state := jsonx.ObjectOf(aggregate.TypeName, dtos)
			response.RebuiltState = &state
		}

		if request.DryRun {
			return errRollback
		}
		return nil
	})
	if errors.Is(err, errRollback) {
		return response, nil
	}
	if err != nil {
		return nil, err
	}

	for _, id := range evicted {
		s.Cache.EvictLatest(id)
	}
	s.Audit.Audit(principal, "RewroteHistory", fmt.Sprintf(
		"Updated %d events and %d timelines.Event UUIDs: [%s] ; Timeline UUIDs: [%s]",
		len(updatedEvents), len(updatedTimeline), join(updatedEvents), join(updatedTimeline)))
	return response, nil
}

func replaceTimeline(ctx context.Context, db store.DB, updates []TimelineUpdate) ([]*entity.Timeline, []uuid.UUID, error) {
	existing, err := store.FindTimelineByUUIDsIncludingDeleted(ctx, db, distinct(updates, func(u TimelineUpdate) uuid.UUID { return u.UUID }))
	if err != nil {
		return nil, nil, err
	}
	byUUID := map[uuid.UUID]*entity.Timeline{}
	var ids []uuid.UUID
	for _, entry := range existing {
		byUUID[entry.UUID] = entry
		ids = append(ids, entry.UUID)
	}

	var replacements []*entity.Timeline
	for _, update := range updates {
		if update.Operation != OperationUpdate {
			continue
		}
		original, ok := byUUID[update.UUID]
		if !ok {
			return nil, nil, apperr.TimelineNotFound(update.UUID)
		}
		replacement := *original
		replacement.ID = nil
		replacement.EventType = update.Timeline.Event
		replacement.Data = update.Timeline.Data
		replacement.CustomType = update.Timeline.CustomType
		replacement.CustomData = update.Timeline.CustomData
		replacements = append(replacements, &replacement)
	}
	return replacements, ids, nil
}

func replaceEvents(
	ctx context.Context, db store.DB, c clock.Clock, principal string, updates []EventUpdate,
) ([]*entity.Event, []uuid.UUID, error) {
	existing, err := store.FindEventsByUUIDsIncludingDeleted(ctx, db, distinct(updates, func(u EventUpdate) uuid.UUID { return u.UUID }))
	if err != nil {
		return nil, nil, err
	}
	byUUID := map[uuid.UUID]*entity.Event{}
	var ids []uuid.UUID
	for _, e := range existing {
		byUUID[e.UUID] = e
		ids = append(ids, e.UUID)
	}

	replacements := make([]*entity.Event, 0, len(updates))
	for _, update := range updates {
		original, ok := byUUID[update.UUID]
		if !ok {
			return nil, nil, apperr.EventNotFound(update.UUID)
		}
		replacement := *original
		replacement.ID = nil
		replacement.Data = update.Event.Event
		if update.Operation == OperationDelete {
			replacement.Data = event.Redacted{EventType: update.Event.Type(), DateRedacted: c.Now(), RedactedBy: principal}
		}
		replacements = append(replacements, &replacement)
	}
	return replacements, ids, nil
}

func distinct[T any](items []T, key func(T) uuid.UUID) []uuid.UUID {
	seen := map[uuid.UUID]bool{}
	ids := []uuid.UUID{}
	for _, item := range items {
		if id := key(item); !seen[id] {
			seen[id] = true
			ids = append(ids, id)
		}
	}
	return ids
}

func join(ids []uuid.UUID) string {
	parts := make([]string, len(ids))
	for i, id := range ids {
		parts[i] = id.String()
	}
	return strings.Join(parts, ", ")
}
