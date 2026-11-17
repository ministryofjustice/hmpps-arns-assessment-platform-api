package command

import (
	"context"
	"log/slog"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/engine"
)

const (
	maxAttempts  = 3
	retryBackoff = 50 * time.Millisecond
)

// Auditor records who did what.
type Auditor interface {
	AuditCommands(commands []Command)
}

// Evictor removes assessments' cached latest versions.
type Evictor interface {
	EvictLatest(assessmentUUID uuid.UUID)
}

type Dispatcher struct {
	Pool  *pgxpool.Pool
	Audit Auditor
	Cache Evictor
}

// Dispatch handles a batch of commands in one transaction, retrying when a concurrent request
// wrote the same assessment first. Audit events are sent once the transaction has committed.
func (d *Dispatcher) Dispatch(ctx context.Context, c clock.Clock, commands []Command) (*Responses, error) {
	if len(commands) == 0 {
		return nil, apperr.InvalidCommand("No commands received")
	}

	for attempt := 1; ; attempt++ {
		responses, evictions, err := d.dispatchInTransaction(ctx, c, commands)
		if err == nil {
			for _, assessmentUUID := range evictions {
				d.Cache.EvictLatest(assessmentUUID)
			}
			d.Audit.AuditCommands(commands)
			return &Responses{Commands: responses}, nil
		}
		if !apperr.IsConcurrencyConflict(err) || attempt == maxAttempts {
			return nil, err
		}
		slog.DebugContext(ctx, "Retrying commands after a concurrent update", "attempt", attempt, "error", err)
		select {
		case <-ctx.Done():
			return nil, ctx.Err()
		case <-time.After(retryBackoff):
		}
	}
}

func (d *Dispatcher) dispatchInTransaction(ctx context.Context, c clock.Clock, commands []Command) ([]Response, []uuid.UUID, error) {
	var responses []Response
	u := engine.NewUnitOfWork()
	err := pgx.BeginFunc(ctx, d.Pool, func(tx pgx.Tx) error {
		responses = make([]Response, 0, len(commands))
		// Each command is resolved against the earlier results, then handled, then its hooks run.
		for _, command := range commands {
			if err := resolveReferences(command, responses); err != nil {
				return err
			}
			result, err := handle(ctx, tx, c, u, command)
			if err != nil {
				return err
			}
			if err := runHooks(ctx, tx, c, u, command); err != nil {
				return err
			}
			responses = append(responses, Response{Request: command, Result: result})
		}
		return u.Persist(ctx, tx)
	})
	return responses, u.Evictions, err
}
