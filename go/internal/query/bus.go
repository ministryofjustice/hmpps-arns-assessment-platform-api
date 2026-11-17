package query

import (
	"context"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/store"
)

// Auditor records who read what.
type Auditor interface {
	AuditQuery(query Query)
	AuditSubjectAccessRequest(query *SubjectAccessRequest)
}

// Cache holds assessments' latest versions for other services to read.
type Cache interface {
	CacheLatest(result *AssessmentVersionResult)
}

type Dispatcher struct {
	Pool            *pgxpool.Pool
	Audit           Auditor
	Cache           Cache
	MaxLookbackDays int
}

// DispatchAll runs a batch of queries in one read-only transaction, so they see a consistent
// snapshot.
func (d *Dispatcher) DispatchAll(ctx context.Context, c clock.Clock, queries []Query) (*Responses, error) {
	if len(queries) == 0 {
		return nil, apperr.InvalidQuery("No queries received")
	}
	responses := make([]Response, 0, len(queries))
	err := pgx.BeginTxFunc(ctx, d.Pool, pgx.TxOptions{AccessMode: pgx.ReadOnly}, func(tx pgx.Tx) error {
		for _, query := range queries {
			result, err := d.Dispatch(ctx, tx, c, query)
			if err != nil {
				return err
			}
			responses = append(responses, Response{Request: query, Result: result})
		}
		return nil
	})
	if err != nil {
		return nil, err
	}
	return &Responses{Queries: responses}, nil
}

func (d *Dispatcher) Dispatch(ctx context.Context, db store.DB, c clock.Clock, query Query) (Result, error) {
	result, err := d.handle(ctx, db, c, query)
	if err != nil {
		return nil, err
	}
	d.Audit.AuditQuery(query)
	return result, nil
}

func (d *Dispatcher) DispatchSubjectAccessRequest(ctx context.Context, c clock.Clock, query *SubjectAccessRequest) (*SubjectAccessRequestResult, error) {
	result, err := subjectAccessRequest(ctx, d.Pool, c, query)
	if err != nil {
		return nil, err
	}
	d.Audit.AuditSubjectAccessRequest(query)
	return result, nil
}
