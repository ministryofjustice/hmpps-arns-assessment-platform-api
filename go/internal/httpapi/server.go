// Package httpapi is the service's HTTP API.
package httpapi

import (
	"context"
	_ "embed"
	"encoding/json"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	httpSwagger "github.com/swaggo/http-swagger/v2"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/audit"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/cache"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/command"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/config"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datadeletion"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/engine"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/plan"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/query"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/store"
)

// Request bodies can be large: commands carry whole sections of answers.
const maxBodyBytes = 10 << 20

//go:embed openapi.json
var openAPIDocument []byte

//go:embed sar_template.mustache
var sarTemplate []byte

type App struct {
	config       config.Config
	pool         *pgxpool.Pool
	audit        *audit.Service
	cache        *cache.Service
	auth         *authenticator
	commands     *command.Dispatcher
	queries      *query.Dispatcher
	dataDeletion *datadeletion.Service
	plans        *plan.Service
	http         *http.Client
}

// New wires up the API. ctx bounds background work such as refreshing HMPPS Auth's keys.
func New(ctx context.Context, cfg config.Config, pool *pgxpool.Pool, auditService *audit.Service, cacheService *cache.Service) (*App, error) {
	auth, err := newAuthenticator(ctx, cfg.HmppsAuthURL)
	if err != nil {
		return nil, err
	}
	commands := &command.Dispatcher{Pool: pool, Audit: auditService, Cache: cacheService}
	queries := &query.Dispatcher{Pool: pool, Audit: auditService, Cache: cacheService, MaxLookbackDays: cfg.MaxLookbackDays}
	return &App{
		config: cfg, pool: pool, audit: auditService, cache: cacheService, auth: auth,
		commands: commands, queries: queries,
		dataDeletion: &datadeletion.Service{Pool: pool, Audit: auditService, Cache: cacheService},
		plans:        &plan.Service{Commands: commands, Queries: queries},
		http:         &http.Client{Timeout: 2 * time.Second},
	}, nil
}

// handlerFunc is an http.HandlerFunc that returns its error to be written as an error response.
type handlerFunc func(w http.ResponseWriter, r *http.Request) error

func (h handlerFunc) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	if err := h(w, r); err != nil {
		writeError(w, r, err)
	}
}

func (a *App) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.Handle("POST /command", handlerFunc(a.executeCommands))
	mux.Handle("POST /query", handlerFunc(a.executeQueries))
	mux.Handle("DELETE /assessment/{assessmentUuid}", handlerFunc(a.deleteAssessment))
	mux.Handle("POST /assessment/{assessmentUuid}/rebuild", handlerFunc(a.rebuildAggregates))
	mux.Handle("GET /data-deletion/{assessmentUuid}", handlerFunc(a.dataDeletionData))
	mux.Handle("POST /data-deletion/{assessmentUuid}", handlerFunc(a.updateData))
	mux.Handle("POST /plan/start-new-period-of-supervision", handlerFunc(a.startNewPeriodOfSupervision))
	mux.Handle("GET /subject-access-request", handlerFunc(a.subjectAccessRequest))
	mux.Handle("GET /subject-access-request/template", handlerFunc(a.subjectAccessRequestTemplate))

	mux.HandleFunc("GET /health", a.health)
	mux.HandleFunc("GET /health/ping", ping)
	mux.HandleFunc("GET /health/liveness", ping)
	mux.HandleFunc("GET /health/readiness", a.readiness)
	mux.HandleFunc("GET /info", info)
	mux.HandleFunc("GET /v3/api-docs", serveOpenAPI)
	mux.Handle("GET /swagger-ui/", httpSwagger.Handler(httpSwagger.URL("/v3/api-docs")))
	mux.HandleFunc("/", notFound)

	return logRequests(recoverPanics(mux))
}

func buildNumber() string {
	if build := os.Getenv("BUILD_NUMBER"); build != "" {
		return build
	}
	return "local"
}

func serveOpenAPI(w http.ResponseWriter, _ *http.Request) {
	var document map[string]any
	if err := json.Unmarshal(openAPIDocument, &document); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	if info, ok := document["info"].(map[string]any); ok {
		info["version"] = buildNumber()
	}
	writeJSON(w, http.StatusOK, document)
}

// decode reads a JSON body, reporting anything unreadable as an invalid payload.
func decode(w http.ResponseWriter, r *http.Request, into any) error {
	r.Body = http.MaxBytesReader(w, r.Body, maxBodyBytes)
	if err := json.NewDecoder(r.Body).Decode(into); err != nil {
		return apperr.InvalidPayload(err.Error())
	}
	return nil
}

func pathUUID(r *http.Request, name string) (uuid.UUID, error) {
	id, err := uuid.Parse(r.PathValue(name))
	if err != nil {
		return uuid.Nil, apperr.InvalidPayload(fmt.Sprintf("invalid UUID '%s' for %s", r.PathValue(name), name))
	}
	return id, nil
}

// requestClock is the request's clock, moved back to the backdateTo query parameter when one is given.
func requestClock(r *http.Request) (clock.Clock, error) {
	raw := r.URL.Query().Get("backdateTo")
	if raw == "" {
		return clock.System(), nil
	}
	backdateTo, err := datetime.Parse(raw)
	if err != nil {
		return clock.Clock{}, apperr.InvalidBackdate(err.Error())
	}
	return clock.BackdatedTo(backdateTo)
}

type commandsRequest struct {
	Commands command.List `json:"commands"`
}

func (a *App) executeCommands(w http.ResponseWriter, r *http.Request) error {
	claims, err := a.auth.authorise(r, commandRoles)
	if err != nil {
		return err
	}
	c, err := requestClock(r)
	if err != nil {
		return err
	}
	var request commandsRequest
	if err := decode(w, r, &request); err != nil {
		return err
	}
	response, err := a.commands.Dispatch(r.Context(), c, request.Commands)
	if err != nil {
		return err
	}
	slog.InfoContext(r.Context(), fmt.Sprintf("Client '%s' executed %d command(s)", claims.Principal(), len(request.Commands)))
	writeJSON(w, http.StatusOK, response)
	return nil
}

type queriesRequest struct {
	Queries query.List `json:"queries"`
}

func (a *App) executeQueries(w http.ResponseWriter, r *http.Request) error {
	if _, err := a.auth.authorise(r, queryRoles); err != nil {
		return err
	}
	c, err := requestClock(r)
	if err != nil {
		return err
	}
	var request queriesRequest
	if err := decode(w, r, &request); err != nil {
		return err
	}
	response, err := a.queries.DispatchAll(r.Context(), c, request.Queries)
	if err != nil {
		return err
	}
	writeJSON(w, http.StatusOK, response)
	return nil
}

func (a *App) deleteAssessment(w http.ResponseWriter, r *http.Request) error {
	claims, err := a.auth.authorise(r, deleteRoles)
	if err != nil {
		return err
	}
	id, err := pathUUID(r, "assessmentUuid")
	if err != nil {
		return err
	}
	if _, err := engine.FindAssessment(r.Context(), a.pool, id); err != nil {
		return err
	}
	if err := store.DeleteAssessment(r.Context(), a.pool, id); err != nil {
		return err
	}
	slog.InfoContext(r.Context(), fmt.Sprintf("Client '%s' deleted assessment '%s'", claims.Principal(), id))
	w.WriteHeader(http.StatusOK)
	return nil
}

// rebuildAggregates replaces all of the assessment's snapshots with ones rebuilt from its events.
func (a *App) rebuildAggregates(w http.ResponseWriter, r *http.Request) error {
	claims, err := a.auth.authorise(r, rebuildRoles)
	if err != nil {
		return err
	}
	c, err := requestClock(r)
	if err != nil {
		return err
	}
	id, err := pathUUID(r, "assessmentUuid")
	if err != nil {
		return err
	}

	var rebuiltCount int
	var evicted []uuid.UUID
	err = pgxBeginFunc(r.Context(), a.pool, func(db store.DB) error {
		assessment, err := engine.FindAssessment(r.Context(), db, id)
		if err != nil {
			return err
		}
		now := clock.System().Now()
		rebuilt, evictions, err := engine.RebuildState(r.Context(), db, c, assessment, &now)
		if err != nil {
			return err
		}
		rebuiltCount, evicted = len(rebuilt.Aggregates), evictions
		return nil
	})
	if err != nil {
		return err
	}
	for _, assessmentUUID := range evicted {
		a.cache.EvictLatest(assessmentUUID)
	}
	a.audit.Audit(claims.Principal(), "RebuiltAggregates", fmt.Sprintf("Rebuilt all (%d) aggregates for assessment %s", rebuiltCount, id))
	w.WriteHeader(http.StatusOK)
	return nil
}

func (a *App) dataDeletionData(w http.ResponseWriter, r *http.Request) error {
	if _, err := a.auth.authorise(r, dataDeletionRoles); err != nil {
		return err
	}
	id, err := pathUUID(r, "assessmentUuid")
	if err != nil {
		return err
	}
	response, err := a.dataDeletion.Data(r.Context(), id)
	if err != nil {
		return err
	}
	writeJSON(w, http.StatusOK, response)
	return nil
}

func (a *App) updateData(w http.ResponseWriter, r *http.Request) error {
	claims, err := a.auth.authorise(r, dataDeletionRoles)
	if err != nil {
		return err
	}
	c, err := requestClock(r)
	if err != nil {
		return err
	}
	id, err := pathUUID(r, "assessmentUuid")
	if err != nil {
		return err
	}
	var request datadeletion.Request
	if err := decode(w, r, &request); err != nil {
		return err
	}
	response, err := a.dataDeletion.Update(r.Context(), c, claims.Principal(), id, request)
	if err != nil {
		return err
	}
	slog.InfoContext(r.Context(), fmt.Sprintf("Client '%s' updated data for assessment '%s' (dry run: %t, success: %t)",
		claims.Principal(), id, response.DryRun, response.Success))
	writeJSON(w, http.StatusOK, response)
	return nil
}

func (a *App) startNewPeriodOfSupervision(w http.ResponseWriter, r *http.Request) error {
	claims, err := a.auth.authorise(r, planRoles)
	if err != nil {
		return err
	}
	c, err := requestClock(r)
	if err != nil {
		return err
	}
	var request plan.NewPeriodOfSupervisionRequest
	if err := decode(w, r, &request); err != nil {
		return err
	}
	if request.AssessmentUUID == uuid.Nil {
		return apperr.InvalidPayload("missing required property 'assessmentUuid'")
	}
	if err := a.plans.StartNewPeriodOfSupervision(r.Context(), c, request); err != nil {
		return err
	}
	slog.InfoContext(r.Context(), fmt.Sprintf("Client '%s' started a new period of supervision for plan '%s'",
		claims.Principal(), request.AssessmentUUID))
	w.WriteHeader(http.StatusOK)
	return nil
}

func ping(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"status": "UP"})
}

func info(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]any{
		"app":   map[string]string{"name": "HMPPS Arns Assessment Platform Api", "version": "1.0"},
		"build": map[string]string{"name": "hmpps-arns-assessment-platform-api", "version": buildNumber()},
	})
}

func logRequests(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		started := time.Now()
		recorder := &statusRecorder{ResponseWriter: w, status: http.StatusOK}
		next.ServeHTTP(recorder, r)
		level := slog.LevelDebug
		if recorder.status >= http.StatusInternalServerError {
			level = slog.LevelError
		}
		slog.Log(r.Context(), level, "Request", "method", r.Method, "path", r.URL.Path,
			"status", recorder.status, "latency", time.Since(started))
	})
}

type statusRecorder struct {
	http.ResponseWriter
	status int
}

func (s *statusRecorder) WriteHeader(status int) {
	s.status = status
	s.ResponseWriter.WriteHeader(status)
}

func recoverPanics(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if recovered := recover(); recovered != nil {
				if recovered == http.ErrAbortHandler {
					panic(recovered)
				}
				writeError(w, r, errors.New(fmt.Sprint(recovered)))
			}
		}()
		next.ServeHTTP(w, r)
	})
}
