package httpapi

import (
	"context"
	"net/http"
	"strings"
	"sync"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/query"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/store"
)

// noIdentifier is returned when neither identifier is given, as the HMPPS SAR library does.
const noIdentifier = 209

// subjectAccessRequest returns everything held about a person, by prison or case reference number.
func (a *App) subjectAccessRequest(w http.ResponseWriter, r *http.Request) error {
	if _, err := a.auth.authorise(r, sarRoles); err != nil {
		return err
	}
	params := r.URL.Query()
	prn, crn := params.Get("prn"), params.Get("crn")
	if prn == "" && crn == "" {
		message := "One of prn or crn must be supplied."
		writeErrorResponse(w, noIdentifier, message, message)
		return nil
	}

	c := clock.System()
	now := c.Now()
	request := &query.SubjectAccessRequest{Timestamp: &now}
	if crn != "" {
		request.AssessmentIdentifiers = append(request.AssessmentIdentifiers, model.IdentifierPair{Type: model.IdentifierCRN, ID: crn})
	}
	if prn != "" {
		request.AssessmentIdentifiers = append(request.AssessmentIdentifiers, model.IdentifierPair{Type: model.IdentifierPRN, ID: prn})
	}
	var err error
	if request.From, err = dateParam(params.Get("fromDate")); err != nil {
		return err
	}
	if request.To, err = dateParam(params.Get("toDate")); err != nil {
		return err
	}

	result, err := a.queries.DispatchSubjectAccessRequest(r.Context(), c, request)
	if err != nil {
		return err
	}
	writeJSON(w, http.StatusOK, map[string]any{"content": result})
	return nil
}

func dateParam(raw string) (*datetime.Date, error) {
	if raw == "" {
		return nil, nil
	}
	date, err := datetime.ParseDate(raw)
	if err != nil {
		return nil, apperr.InvalidPayload(err.Error())
	}
	return &date, nil
}

// subjectAccessRequestTemplate is the template the SAR service renders the content with.
func (a *App) subjectAccessRequestTemplate(w http.ResponseWriter, r *http.Request) error {
	if _, err := a.auth.authorise(r, sarRoles); err != nil {
		return err
	}
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	_, err := w.Write(sarTemplate)
	return err
}

func up() map[string]any { return map[string]any{"status": "UP"} }

func down(err error) map[string]any {
	return map[string]any{"status": "DOWN", "details": map[string]string{"error": err.Error()}}
}

// health is overall health in the shape Spring Boot Actuator gave it, which is down when any
// component is.
func (a *App) health(w http.ResponseWriter, r *http.Request) {
	components := map[string]any{"ping": up()}
	var mu sync.Mutex
	var wg sync.WaitGroup
	set := func(name string, value map[string]any) {
		mu.Lock()
		defer mu.Unlock()
		components[name] = value
	}

	wg.Go(func() { set("db", a.database(r.Context())) })
	wg.Go(func() { set("hmppsAuth", a.hmppsAuth(r.Context())) })
	wg.Go(func() {
		if enabled, err := a.cache.Ping(r.Context()); enabled && err != nil {
			set("redis", down(err))
		} else if enabled {
			set("redis", up())
		}
	})
	wg.Wait()

	status, overall := http.StatusOK, "UP"
	for _, component := range components {
		if component.(map[string]any)["status"] != "UP" {
			status, overall = http.StatusServiceUnavailable, "DOWN"
		}
	}
	writeJSON(w, status, map[string]any{"status": overall, "components": components})
}

// readiness is ready once the database can be reached.
func (a *App) readiness(w http.ResponseWriter, r *http.Request) {
	db := a.database(r.Context())
	status := http.StatusOK
	if db["status"] != "UP" {
		status = http.StatusServiceUnavailable
	}
	writeJSON(w, status, map[string]any{"status": db["status"]})
}

func (a *App) database(ctx context.Context) map[string]any {
	if _, err := a.pool.Exec(ctx, "SELECT 1"); err != nil {
		return down(err)
	}
	return map[string]any{"status": "UP", "details": map[string]string{"database": "PostgreSQL"}}
}

func (a *App) hmppsAuth(ctx context.Context) map[string]any {
	request, err := http.NewRequestWithContext(ctx, http.MethodGet, strings.TrimRight(a.config.HmppsAuthURL, "/")+"/health/ping", nil)
	if err != nil {
		return down(err)
	}
	response, err := a.http.Do(request)
	if err != nil {
		return down(err)
	}
	response.Body.Close()
	status := "DOWN"
	if response.StatusCode < 300 {
		status = "UP"
	}
	return map[string]any{"status": status, "details": map[string]int{"HttpStatus": response.StatusCode}}
}

// pgxBeginFunc runs fn in a transaction.
func pgxBeginFunc(ctx context.Context, pool *pgxpool.Pool, fn func(store.DB) error) error {
	return pgx.BeginFunc(ctx, pool, func(tx pgx.Tx) error { return fn(tx) })
}
