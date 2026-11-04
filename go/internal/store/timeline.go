package store

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
)

const timelineSelect = `
	SELECT t.id, t.uuid, t.position, t.created_at, t.event_type, t.data, t.custom_type, t.custom_data, t.deleted,
	       t.assessment_uuid, ` + userSelect + `
	FROM timeline t
	JOIN user_details u ON u.uuid = t.user_details_uuid `

func collectTimeline(rows pgx.Rows, err error) ([]*entity.Timeline, error) {
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var entries []*entity.Timeline
	for rows.Next() {
		entry := &entity.Timeline{}
		var pk int64
		var position int32
		var createdAt time.Time
		var data, customData []byte
		var deleted *bool
		var user userColumns
		targets := append([]any{&pk, &entry.UUID, &position, &createdAt, &entry.EventType, &data, &entry.CustomType,
			&customData, &deleted, &entry.AssessmentUUID}, user.targets()...)
		if err := rows.Scan(targets...); err != nil {
			return nil, err
		}
		entry.ID, entry.Position, entry.CreatedAt = &pk, &position, local(createdAt)
		entry.Deleted = deleted != nil && *deleted
		if err := json.Unmarshal(data, &entry.Data); err != nil {
			return nil, fmt.Errorf("timeline %s: %w", entry.UUID, err)
		}
		if customData != nil {
			entry.CustomData = &jsonx.Object{}
			if err := json.Unmarshal(customData, entry.CustomData); err != nil {
				return nil, fmt.Errorf("timeline %s: %w", entry.UUID, err)
			}
		}
		if entry.User, err = user.entity(); err != nil {
			return nil, err
		}
		entries = append(entries, entry)
	}
	return entries, rows.Err()
}

// TimelineCriteria filters timeline queries. Empty type lists don't filter.
type TimelineCriteria struct {
	AssessmentUUID     *uuid.UUID
	UserUUID           *uuid.UUID
	From, To           *datetime.LocalDateTime
	IncludeEventTypes  []string
	ExcludeEventTypes  []string
	IncludeCustomTypes []string
	ExcludeCustomTypes []string
}

const timelineCriteria = `
	WHERE t.deleted IS FALSE
	  AND ($1::uuid IS NULL OR t.assessment_uuid = $1)
	  AND ($2::uuid IS NULL OR t.user_details_uuid = $2)
	  AND ($3::timestamptz IS NULL OR t.created_at >= $3)
	  AND ($4::timestamptz IS NULL OR t.created_at <= $4)
	  AND (cardinality($5::text[]) = 0 OR t.event_type = ANY($5))
	  AND (cardinality($6::text[]) = 0 OR NOT (t.event_type = ANY($6)))
	  AND (cardinality($7::text[]) = 0 OR t.custom_type = ANY($7))
	  AND (cardinality($8::text[]) = 0 OR NOT (t.custom_type = ANY($8)))`

func orNone(values []string) []string {
	if values == nil {
		return []string{}
	}
	return values
}

// FindTimelinePage returns a page of the timeline, most recent position first, with the total
// number of matching entries.
func FindTimelinePage(ctx context.Context, db DB, criteria TimelineCriteria, pageNumber, pageSize int) ([]*entity.Timeline, int64, error) {
	if criteria.AssessmentUUID == nil && criteria.UserUUID == nil {
		return nil, 0, apperr.TimelineBadCriteria()
	}
	args := []any{
		criteria.AssessmentUUID, criteria.UserUUID, optionalInstant(criteria.From), optionalInstant(criteria.To),
		orNone(criteria.IncludeEventTypes), orNone(criteria.ExcludeEventTypes),
		orNone(criteria.IncludeCustomTypes), orNone(criteria.ExcludeCustomTypes),
	}

	entries, err := collectTimeline(db.Query(ctx, timelineSelect+timelineCriteria+`
		ORDER BY t.position DESC LIMIT $9 OFFSET $10`, append(args, pageSize, pageNumber*pageSize)...))
	if err != nil {
		return nil, 0, err
	}
	var total int64
	err = db.QueryRow(ctx, `SELECT count(*) FROM timeline t `+timelineCriteria, args...).Scan(&total)
	return entries, total, err
}

// FindDailyVersions returns one entry per day with timeline activity, pointing at the last
// timeline entry of that day.
func FindDailyVersions(ctx context.Context, db DB, assessmentUUID uuid.UUID) ([]model.DailyVersionDetails, error) {
	rows, err := db.Query(ctx, `
		SELECT min_created_at, max_created_at, uuid
		FROM (
			SELECT
				DATE(t.created_at) AS day,
				t.created_at,
				t.uuid,
				MIN(t.created_at) OVER (PARTITION BY DATE(t.created_at)) AS min_created_at,
				MAX(t.created_at) OVER (PARTITION BY DATE(t.created_at)) AS max_created_at,
				ROW_NUMBER() OVER (PARTITION BY DATE(t.created_at) ORDER BY t.created_at DESC, t.position DESC) AS rn
			FROM timeline t
			WHERE t.assessment_uuid = $1 AND t.deleted IS FALSE
		) ranked
		WHERE rn = 1
		ORDER BY day`, assessmentUUID)
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, func(row pgx.CollectableRow) (model.DailyVersionDetails, error) {
		var createdAt, updatedAt time.Time
		var version model.DailyVersionDetails
		err := row.Scan(&createdAt, &updatedAt, &version.LastTimelineItemUUID)
		version.CreatedAt, version.UpdatedAt = local(createdAt), local(updatedAt)
		return version, err
	})
}

func FindAllTimelineIncludingDeleted(ctx context.Context, db DB, assessmentUUID uuid.UUID) ([]*entity.Timeline, error) {
	return collectTimeline(db.Query(ctx, timelineSelect+`WHERE t.assessment_uuid = $1 ORDER BY t.position`, assessmentUUID))
}

func FindTimelineByUUIDsIncludingDeleted(ctx context.Context, db DB, ids []uuid.UUID) ([]*entity.Timeline, error) {
	return collectTimeline(db.Query(ctx, timelineSelect+`WHERE t.uuid = ANY($1) ORDER BY t.position`, ids))
}

// FindMaxTimelinePosition returns nil when the assessment has no timeline, including deleted entries.
func FindMaxTimelinePosition(ctx context.Context, db DB, assessmentUUID uuid.UUID) (*int32, error) {
	var position int32
	err := db.QueryRow(ctx, `SELECT position FROM timeline WHERE assessment_uuid = $1 ORDER BY position DESC LIMIT 1`,
		assessmentUUID).Scan(&position)
	if isNoRows(err) {
		return nil, nil
	}
	return &position, err
}

func SoftDeleteTimelineFrom(ctx context.Context, db DB, assessmentUUID uuid.UUID, from datetime.LocalDateTime) error {
	_, err := db.Exec(ctx, `
		UPDATE timeline SET deleted = TRUE
		WHERE assessment_uuid = $1 AND created_at >= $2 AND deleted IS FALSE`, assessmentUUID, from.Instant())
	return err
}

func UndeleteTimelineFrom(ctx context.Context, db DB, assessmentUUID uuid.UUID, from datetime.LocalDateTime) error {
	_, err := db.Exec(ctx, `
		UPDATE timeline SET deleted = FALSE
		WHERE assessment_uuid = $1 AND created_at >= $2 AND deleted IS TRUE`, assessmentUUID, from.Instant())
	return err
}

func InsertTimeline(ctx context.Context, db DB, entry *entity.Timeline) error {
	if entry.Position == nil {
		return fmt.Errorf("timeline %s has no position", entry.UUID)
	}
	data, err := json.Marshal(entry.Data)
	if err != nil {
		return err
	}
	var customData []byte
	if entry.CustomData != nil {
		if customData, err = json.Marshal(entry.CustomData); err != nil {
			return err
		}
	}
	_, err = db.Exec(ctx, `
		INSERT INTO timeline (uuid, created_at, event_type, data, custom_type, custom_data, user_details_uuid, assessment_uuid, position, deleted)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
		entry.UUID, entry.CreatedAt.Instant(), entry.EventType, data, entry.CustomType, customData,
		entry.User.UUID, entry.AssessmentUUID, *entry.Position, entry.Deleted)
	return err
}

func DeleteTimeline(ctx context.Context, db DB, ids []uuid.UUID) error {
	_, err := db.Exec(ctx, `DELETE FROM timeline WHERE uuid = ANY($1)`, ids)
	return err
}
