package store

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/event"
)

const eventSelect = `
	SELECT e.id, e.uuid, e.position, e.created_at, e.data, e.autosaved, e.deleted, e.assessment_uuid, ` + userSelect + `
	FROM event e
	JOIN user_details u ON u.uuid = e.user_details_uuid `

// collectEvents reads events, finding their assessments with the given function.
func collectEvents(
	rows pgx.Rows, err error, assessmentFor func(uuid.UUID) (*entity.Assessment, error),
) ([]*entity.Event, error) {
	if err != nil {
		return nil, err
	}
	type row struct {
		event          entity.Event
		data           []byte
		assessmentUUID uuid.UUID
		user           userColumns
	}
	var scanned []row
	for rows.Next() {
		var r row
		var pk int64
		var position int32
		var createdAt time.Time
		var deleted *bool
		targets := append([]any{&pk, &r.event.UUID, &position, &createdAt, &r.data, &r.event.Autosaved, &deleted, &r.assessmentUUID},
			r.user.targets()...)
		if err := rows.Scan(targets...); err != nil {
			rows.Close()
			return nil, err
		}
		r.event.ID, r.event.Position, r.event.CreatedAt = &pk, &position, local(createdAt)
		r.event.Deleted = deleted != nil && *deleted
		scanned = append(scanned, r)
	}
	rows.Close()
	if err := rows.Err(); err != nil {
		return nil, err
	}

	// Assessments are found after the rows are read, as that may query the same connection.
	events := make([]*entity.Event, 0, len(scanned))
	for _, r := range scanned {
		e := r.event
		if e.Data, err = event.Unmarshal(r.data); err != nil {
			return nil, fmt.Errorf("event %s: %w", e.UUID, err)
		}
		if e.User, err = r.user.entity(); err != nil {
			return nil, err
		}
		if e.Assessment, err = assessmentFor(r.assessmentUUID); err != nil {
			return nil, err
		}
		events = append(events, &e)
	}
	return events, nil
}

func forAssessment(assessment *entity.Assessment) func(uuid.UUID) (*entity.Assessment, error) {
	return func(uuid.UUID) (*entity.Assessment, error) { return assessment, nil }
}

// FindEventsUpTo returns the events up to and including the point in time, in order.
func FindEventsUpTo(ctx context.Context, db DB, assessment *entity.Assessment, pointInTime datetime.LocalDateTime) ([]*entity.Event, error) {
	rows, err := db.Query(ctx, eventSelect+`
		WHERE e.assessment_uuid = $1 AND e.created_at <= $2 AND e.deleted IS FALSE
		ORDER BY e.position`, assessment.UUID, pointInTime.Instant())
	return collectEvents(rows, err, forAssessment(assessment))
}

// FindEventsBetween returns the events after from, up to and including to, in order.
func FindEventsBetween(ctx context.Context, db DB, assessment *entity.Assessment, from, to datetime.LocalDateTime) ([]*entity.Event, error) {
	rows, err := db.Query(ctx, eventSelect+`
		WHERE e.assessment_uuid = $1 AND e.created_at > $2 AND e.created_at <= $3 AND e.deleted IS FALSE
		ORDER BY e.position`, assessment.UUID, from.Instant(), to.Instant())
	return collectEvents(rows, err, forAssessment(assessment))
}

func FindAllEventsIncludingDeleted(ctx context.Context, db DB, assessment *entity.Assessment) ([]*entity.Event, error) {
	rows, err := db.Query(ctx, eventSelect+`WHERE e.assessment_uuid = $1 ORDER BY e.position`, assessment.UUID)
	return collectEvents(rows, err, forAssessment(assessment))
}

func FindEventsByUUIDsIncludingDeleted(ctx context.Context, db DB, ids []uuid.UUID) ([]*entity.Event, error) {
	rows, err := db.Query(ctx, eventSelect+`WHERE e.uuid = ANY($1) ORDER BY e.position`, ids)
	assessments := map[uuid.UUID]*entity.Assessment{}
	return collectEvents(rows, err, func(id uuid.UUID) (*entity.Assessment, error) {
		if found, ok := assessments[id]; ok {
			return found, nil
		}
		found, err := FindAssessment(ctx, db, id)
		if err == nil && found == nil {
			err = fmt.Errorf("event has no assessment %s", id)
		}
		assessments[id] = found
		return found, err
	})
}

type DeletedEvent struct {
	UUID     uuid.UUID
	Position int32
}

// FindDeletedEventsFrom returns the deleted events created at or after from.
func FindDeletedEventsFrom(ctx context.Context, db DB, assessmentUUID uuid.UUID, from datetime.LocalDateTime) ([]DeletedEvent, error) {
	rows, err := db.Query(ctx, `
		SELECT uuid, position FROM event
		WHERE assessment_uuid = $1 AND created_at >= $2 AND deleted IS TRUE
		ORDER BY position`, assessmentUUID, from.Instant())
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, func(row pgx.CollectableRow) (DeletedEvent, error) {
		var deleted DeletedEvent
		err := row.Scan(&deleted.UUID, &deleted.Position)
		return deleted, err
	})
}

// EventsExistAfterPosition reports whether any events that haven't been deleted come after the position.
func EventsExistAfterPosition(ctx context.Context, db DB, assessmentUUID uuid.UUID, position int32) (bool, error) {
	var exists bool
	err := db.QueryRow(ctx, `
		SELECT EXISTS (SELECT 1 FROM event WHERE assessment_uuid = $1 AND position > $2 AND deleted IS FALSE)`,
		assessmentUUID, position).Scan(&exists)
	return exists, err
}

// FindMaxEventPosition returns nil when the assessment has no events, including deleted ones.
func FindMaxEventPosition(ctx context.Context, db DB, assessmentUUID uuid.UUID) (*int32, error) {
	var position int32
	err := db.QueryRow(ctx, `SELECT position FROM event WHERE assessment_uuid = $1 ORDER BY position DESC LIMIT 1`,
		assessmentUUID).Scan(&position)
	if isNoRows(err) {
		return nil, nil
	}
	return &position, err
}

func SoftDeleteEventsFrom(ctx context.Context, db DB, assessmentUUID uuid.UUID, from datetime.LocalDateTime) error {
	_, err := db.Exec(ctx, `
		UPDATE event SET deleted = TRUE
		WHERE assessment_uuid = $1 AND created_at >= $2 AND deleted IS FALSE`, assessmentUUID, from.Instant())
	return err
}

func UndeleteEvents(ctx context.Context, db DB, ids []uuid.UUID) error {
	_, err := db.Exec(ctx, `UPDATE event SET deleted = FALSE WHERE uuid = ANY($1)`, ids)
	return err
}

func InsertEvent(ctx context.Context, db DB, e *entity.Event) error {
	if e.Position == nil {
		return fmt.Errorf("event %s has no position", e.UUID)
	}
	data, err := json.Marshal(e.Data)
	if err != nil {
		return err
	}
	_, err = db.Exec(ctx, `
		INSERT INTO event (uuid, created_at, data, assessment_uuid, user_details_uuid, position, autosaved, deleted)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
		e.UUID, e.CreatedAt.Instant(), data, e.Assessment.UUID, e.User.UUID, *e.Position, e.Autosaved, e.Deleted)
	return err
}

func DeleteEvents(ctx context.Context, db DB, ids []uuid.UUID) error {
	_, err := db.Exec(ctx, `DELETE FROM event WHERE uuid = ANY($1)`, ids)
	return err
}

// FindAssessmentUUIDsModifiedSince pages through assessments of the type with events created
// after since, in UUID order, from the after cursor. A cursor is used rather than offsets
// because a large result set can change while it is being paged through, and offsets would then
// drop rows.
func FindAssessmentUUIDsModifiedSince(
	ctx context.Context, db DB, assessmentType string, since datetime.LocalDateTime, after *uuid.UUID, limit int,
) ([]uuid.UUID, error) {
	rows, err := db.Query(ctx, `
		SELECT DISTINCT a.uuid
		FROM event e
		JOIN assessment a ON a.uuid = e.assessment_uuid
		WHERE a.type = $1 AND e.created_at > $2 AND e.deleted IS FALSE AND ($3::uuid IS NULL OR a.uuid > $3)
		ORDER BY a.uuid
		LIMIT $4`, assessmentType, since.Instant(), after, limit)
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, pgx.RowTo[uuid.UUID])
}

// FindAssessmentUUIDsSoftDeletedSince finds assessments of the type that have deleted events and
// had their aggregates rebuilt since.
func FindAssessmentUUIDsSoftDeletedSince(ctx context.Context, db DB, assessmentType string, since datetime.LocalDateTime) ([]uuid.UUID, error) {
	rows, err := db.Query(ctx, `
		SELECT DISTINCT a.uuid
		FROM event e
		JOIN assessment a ON a.uuid = e.assessment_uuid
		JOIN aggregate ag ON ag.assessment_uuid = a.uuid
		WHERE a.type = $1 AND ag.updated_at > $2 AND e.deleted = TRUE
		ORDER BY a.uuid`, assessmentType, since.Instant())
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, pgx.RowTo[uuid.UUID])
}
