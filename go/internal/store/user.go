package store

import (
	"context"
	"errors"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
)

const userDetailsSelect = `SELECT id, uuid, user_id, display_name, auth_source FROM user_details u `

func collectUsers(rows pgx.Rows, err error) ([]entity.UserDetails, error) {
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var users []entity.UserDetails
	for rows.Next() {
		var columns userColumns
		if err := rows.Scan(columns.targets()...); err != nil {
			return nil, err
		}
		user, err := columns.entity()
		if err != nil {
			return nil, err
		}
		users = append(users, user)
	}
	return users, rows.Err()
}

func first(users []entity.UserDetails, err error) (*entity.UserDetails, error) {
	if err != nil || len(users) == 0 {
		return nil, err
	}
	return &users[0], nil
}

// FindUserByUserIDAndAuthSource returns nil when there's no such user.
func FindUserByUserIDAndAuthSource(ctx context.Context, db DB, userID string, authSource model.AuthSource) (*entity.UserDetails, error) {
	return first(collectUsers(db.Query(ctx, userDetailsSelect+`WHERE user_id = $1 AND auth_source = $2`, userID, string(authSource))))
}

// FindUserByUUID returns nil when there's no such user.
func FindUserByUUID(ctx context.Context, db DB, id uuid.UUID) (*entity.UserDetails, error) {
	return first(collectUsers(db.Query(ctx, userDetailsSelect+`WHERE uuid = $1`, id)))
}

func FindUsersByUUIDs(ctx context.Context, db DB, ids []uuid.UUID) ([]entity.UserDetails, error) {
	return collectUsers(db.Query(ctx, userDetailsSelect+`WHERE uuid = ANY($1) ORDER BY id`, ids))
}

// InsertUser returns the generated id.
func InsertUser(ctx context.Context, db DB, user *entity.UserDetails) (int64, error) {
	var id int64
	err := db.QueryRow(ctx, `
		INSERT INTO user_details (uuid, user_id, display_name, auth_source) VALUES ($1, $2, $3, $4)
		RETURNING id`, user.UUID, user.UserID, user.DisplayName, string(user.AuthSource)).Scan(&id)
	return id, err
}

var errNoRows = pgx.ErrNoRows

func isNoRows(err error) bool { return errors.Is(err, errNoRows) }
