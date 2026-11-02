// Package store is the database access, one file per table. Every function takes the connection
// to run on, so the caller decides the transaction.
package store

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
)

// DB is a pool, a connection or a transaction.
type DB interface {
	Exec(ctx context.Context, sql string, args ...any) (pgconn.CommandTag, error)
	Query(ctx context.Context, sql string, args ...any) (pgx.Rows, error)
	QueryRow(ctx context.Context, sql string, args ...any) pgx.Row
}

func local(instant time.Time) datetime.LocalDateTime { return datetime.FromInstant(instant) }

func optionalInstant(value *datetime.LocalDateTime) *time.Time {
	if value == nil {
		return nil
	}
	instant := value.Instant()
	return &instant
}

// userColumns scans the user_details columns that queries select as user_pk, user_uuid,
// user_id, display_name and auth_source.
type userColumns struct {
	pk          int64
	uuid        uuid.UUID
	userID      string
	displayName string
	authSource  string
}

func (u *userColumns) targets() []any {
	return []any{&u.pk, &u.uuid, &u.userID, &u.displayName, &u.authSource}
}

func (u *userColumns) entity() (entity.UserDetails, error) {
	authSource, err := model.ParseAuthSource(u.authSource)
	pk := u.pk
	return entity.UserDetails{ID: &pk, UUID: u.uuid, UserID: u.userID, DisplayName: u.displayName, AuthSource: authSource}, err
}

const userSelect = `u.id AS user_pk, u.uuid AS user_uuid, u.user_id, u.display_name, u.auth_source`
