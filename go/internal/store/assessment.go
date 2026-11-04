package store

import (
	"context"
	"errors"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
)

// FindAssessment returns nil when there's no such assessment.
func FindAssessment(ctx context.Context, db DB, id uuid.UUID) (*entity.Assessment, error) {
	assessment := &entity.Assessment{}
	var pk int64
	var createdAt time.Time
	err := db.QueryRow(ctx, `SELECT id, uuid, created_at, type FROM assessment WHERE uuid = $1`, id).
		Scan(&pk, &assessment.UUID, &createdAt, &assessment.Type)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	assessment.ID, assessment.CreatedAt = &pk, local(createdAt)

	rows, err := db.Query(ctx, `
		SELECT id, uuid, created_at, identifier_type, identifier
		FROM assessment_identifier
		WHERE assessment_uuid = $1
		ORDER BY id`, id)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	for rows.Next() {
		var identifier entity.AssessmentIdentifier
		var identifierPK int64
		var identifierCreatedAt time.Time
		var identifierType string
		if err := rows.Scan(&identifierPK, &identifier.UUID, &identifierCreatedAt, &identifierType, &identifier.Pair.ID); err != nil {
			return nil, err
		}
		if identifier.Pair.Type, err = model.ParseIdentifierType(identifierType); err != nil {
			return nil, err
		}
		identifier.ID, identifier.CreatedAt = &identifierPK, local(identifierCreatedAt)
		assessment.Identifiers = append(assessment.Identifiers, identifier)
	}
	return assessment, rows.Err()
}

// FindAssessmentUUIDByExternalIdentifier finds the assessment most recently given this
// identifier before the point in time.
func FindAssessmentUUIDByExternalIdentifier(
	ctx context.Context, db DB, identifierType model.IdentifierType, identifier, assessmentType string, pointInTime datetime.LocalDateTime,
) (*uuid.UUID, error) {
	var found uuid.UUID
	err := db.QueryRow(ctx, `
		SELECT ai.assessment_uuid
		FROM assessment_identifier ai
		JOIN assessment a ON a.uuid = ai.assessment_uuid
		WHERE ai.identifier_type = $1 AND ai.identifier = $2 AND a.type = $3 AND ai.created_at < $4
		ORDER BY ai.created_at DESC
		LIMIT 1`, string(identifierType), identifier, assessmentType, pointInTime.Instant()).Scan(&found)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	return &found, err
}

// FindAssessmentUUIDsByExternalIdentifiers finds assessments with any of the identifiers (or all
// assessments when there are none) created within the optional range.
func FindAssessmentUUIDsByExternalIdentifiers(
	ctx context.Context, db DB, identifiers []model.IdentifierPair, from, to *datetime.LocalDateTime,
) ([]uuid.UUID, error) {
	types, ids := []string{}, []string{}
	for _, pair := range identifiers {
		types, ids = append(types, string(pair.Type)), append(ids, pair.ID)
	}
	rows, err := db.Query(ctx, `
		SELECT a.uuid
		FROM assessment a
		WHERE (
			cardinality($1::text[]) = 0
			OR EXISTS (
				SELECT 1 FROM assessment_identifier ai
				WHERE ai.assessment_uuid = a.uuid
				  AND (ai.identifier_type, ai.identifier) IN (SELECT * FROM UNNEST($1::text[], $2::text[]))
			)
		)
		  AND ($3::timestamptz IS NULL OR a.created_at >= $3)
		  AND ($4::timestamptz IS NULL OR a.created_at <= $4)
		ORDER BY a.id`, types, ids, optionalInstant(from), optionalInstant(to))
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, pgx.RowTo[uuid.UUID])
}

func InsertAssessment(ctx context.Context, db DB, assessment *entity.Assessment) error {
	if _, err := db.Exec(ctx, `INSERT INTO assessment (uuid, created_at, type) VALUES ($1, $2, $3)`,
		assessment.UUID, assessment.CreatedAt.Instant(), assessment.Type); err != nil {
		return err
	}
	for _, identifier := range assessment.Identifiers {
		if _, err := db.Exec(ctx, `
			INSERT INTO assessment_identifier (uuid, created_at, identifier_type, identifier, assessment_uuid)
			VALUES ($1, $2, $3, $4, $5)`,
			identifier.UUID, identifier.CreatedAt.Instant(), string(identifier.Pair.Type), identifier.Pair.ID, assessment.UUID,
		); err != nil {
			return err
		}
	}
	return nil
}

// DeleteAssessment also removes its events, aggregates, timeline and identifiers, through
// cascading foreign keys.
func DeleteAssessment(ctx context.Context, db DB, id uuid.UUID) error {
	_, err := db.Exec(ctx, `DELETE FROM assessment WHERE uuid = $1`, id)
	return err
}
