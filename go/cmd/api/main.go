// Command api runs the ARNS Assessment Platform API.
package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/audit"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/cache"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/config"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/db"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/httpapi"
)

func main() {
	// The image has no shell or curl, so container healthchecks run "/app/app healthcheck"
	if len(os.Args) > 1 && os.Args[1] == "healthcheck" {
		os.Exit(healthcheck())
	}
	slog.SetDefault(slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: logLevel()})))
	if err := run(); err != nil {
		slog.Error("Stopped", "error", err)
		os.Exit(1)
	}
}

// healthcheck pings the running server, exiting 0 if it's up and 1 if not.
func healthcheck() int {
	port := os.Getenv("SERVER_PORT")
	if port == "" {
		port = "8080"
	}
	client := http.Client{Timeout: 5 * time.Second}
	response, err := client.Get("http://localhost:" + port + "/health/ping")
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		return 1
	}
	response.Body.Close()
	if response.StatusCode != http.StatusOK {
		fmt.Fprintln(os.Stderr, response.Status)
		return 1
	}
	return 0
}

func logLevel() slog.Level {
	var level slog.Level
	if err := level.UnmarshalText([]byte(strings.ToUpper(os.Getenv("LOG_LEVEL")))); err != nil {
		return slog.LevelInfo
	}
	return level
}

func run() error {
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	cfg, err := config.FromEnv()
	if err != nil {
		return err
	}
	zone, err := time.LoadLocation(cfg.TimeZone)
	if err != nil {
		return fmt.Errorf("invalid TZ: %w", err)
	}
	datetime.SetZone(zone)

	pool, err := db.Connect(ctx, cfg.Database, cfg.TimeZone)
	if err != nil {
		return fmt.Errorf("unable to connect to the database: %w", err)
	}
	defer pool.Close()
	if err := db.Migrate(ctx, pool); err != nil {
		return fmt.Errorf("unable to migrate the database: %w", err)
	}
	slog.Info("Database ready", "schema", cfg.Database.Schema)

	auditService, err := audit.New(ctx, cfg)
	if err != nil {
		return err
	}
	app, err := httpapi.New(ctx, cfg, pool, auditService, cache.New(cfg.Redis))
	if err != nil {
		return err
	}

	server := &http.Server{
		Addr:              fmt.Sprintf(":%d", cfg.ServerPort),
		Handler:           app.Handler(),
		ReadHeaderTimeout: 10 * time.Second,
	}
	serverErrors := make(chan error, 1)
	go func() {
		slog.Info("Server listening", "address", server.Addr)
		serverErrors <- server.ListenAndServe()
	}()

	select {
	case err := <-serverErrors:
		return err
	case <-ctx.Done():
	}
	slog.Info("Shutting down")
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	if err := server.Shutdown(shutdownCtx); err != nil && !errors.Is(err, http.ErrServerClosed) {
		return err
	}
	return nil
}
