package store

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/aggregate"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
)

// FindLatestAggregateUpTo finds the latest snapshot covering events up to the point in time, or nil.
func FindLatestAggregateUpTo(ctx context.Context, db DB, assessmentUUID uuid.UUID, pointInTime datetime.LocalDateTime) (*aggregate.Entity, error) {
	snapshot := &aggregate.Entity{Data: &aggregate.Assessment{}}
	var pk int64
	var position int32
	var updatedAt, eventsFrom, eventsTo time.Time
	var data []byte
	err := db.QueryRow(ctx, `
		SELECT id, uuid, position, version, updated_at, events_from, events_to, events_applied, assessment_uuid, data
		FROM aggregate
		WHERE assessment_uuid = $1 AND data_type = $2 AND events_to <= $3
		ORDER BY position DESC
		LIMIT 1`, assessmentUUID, aggregate.TypeName, pointInTime.Instant(),
	).Scan(&pk, &snapshot.UUID, &position, &snapshot.Version, &updatedAt, &eventsFrom, &eventsTo,
		&snapshot.EventsApplied, &snapshot.AssessmentUUID, &data)
	if isNoRows(err) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	if err := json.Unmarshal(data, snapshot.Data); err != nil {
		return nil, fmt.Errorf("aggregate %s: %w", snapshot.UUID, err)
	}
	snapshot.ID, snapshot.Position = &pk, &position
	snapshot.UpdatedAt, snapshot.EventsFrom, snapshot.EventsTo = local(updatedAt), local(eventsFrom), local(eventsTo)
	return snapshot, nil
}

// FindMaxAggregatePosition returns nil when the assessment has no snapshots.
func FindMaxAggregatePosition(ctx context.Context, db DB, assessmentUUID uuid.UUID) (*int32, error) {
	var position int32
	err := db.QueryRow(ctx, `
		SELECT position FROM aggregate WHERE assessment_uuid = $1 AND data_type = $2
		ORDER BY position DESC LIMIT 1`, assessmentUUID, aggregate.TypeName).Scan(&position)
	if isNoRows(err) {
		return nil, nil
	}
	return &position, err
}

// InsertAggregate returns the generated id.
func InsertAggregate(ctx context.Context, db DB, snapshot *aggregate.Entity) (int64, error) {
	if snapshot.Position == nil {
		return 0, fmt.Errorf("aggregate %s has no position", snapshot.UUID)
	}
	data, err := json.Marshal(snapshot.Data)
	if err != nil {
		return 0, err
	}
	var id int64
	err = db.QueryRow(ctx, `
		INSERT INTO aggregate (uuid, updated_at, events_from, events_to, events_applied, data, assessment_uuid, version, position)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
		RETURNING id`,
		snapshot.UUID, snapshot.UpdatedAt.Instant(), snapshot.EventsFrom.Instant(), snapshot.EventsTo.Instant(),
		snapshot.EventsApplied, data, snapshot.AssessmentUUID, snapshot.Version, *snapshot.Position,
	).Scan(&id)
	return id, err
}

// UpdateAggregate fails with apperr.ErrOptimisticLock if the snapshot changed since it was read.
func UpdateAggregate(ctx context.Context, db DB, snapshot *aggregate.Entity) error {
	data, err := json.Marshal(snapshot.Data)
	if err != nil {
		return err
	}
	result, err := db.Exec(ctx, `
		UPDATE aggregate
		SET updated_at = $1, events_to = $2, events_applied = $3, data = $4, version = version + 1
		WHERE id = $5 AND version = $6`,
		snapshot.UpdatedAt.Instant(), snapshot.EventsTo.Instant(), snapshot.EventsApplied, data, *snapshot.ID, snapshot.Version)
	if err != nil {
		return err
	}
	if result.RowsAffected() == 0 {
		return fmt.Errorf("aggregate %s was updated or deleted by another transaction: %w", snapshot.UUID, apperr.ErrOptimisticLock)
	}
	return nil
}

func DeleteAggregates(ctx context.Context, db DB, assessmentUUID uuid.UUID) error {
	_, err := db.Exec(ctx, `DELETE FROM aggregate WHERE assessment_uuid = $1`, assessmentUUID)
	return err
}
