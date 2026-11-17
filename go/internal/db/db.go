// Package db connects to PostgreSQL and migrates the schema.
package db

import (
	"context"
	"crypto/sha256"
	"embed"
	"encoding/hex"
	"fmt"
	"io/fs"
	"log/slog"
	"net"
	"regexp"
	"slices"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/config"
)

//go:embed migrations/*.sql
var migrationFiles embed.FS

// Allows hyphens, because the default schema is "assessment-platform". Names are always quoted.
var schemaName = regexp.MustCompile(`^[A-Za-z_][0-9A-Za-z_-]*$`)

// Arbitrary, but must stay the same so replicas starting together take turns.
const migrationLock = 0x4141_5041_5049

func ValidateSchemaName(name string) error {
	if len(name) > 63 { // PostgreSQL's limit on identifiers
		return fmt.Errorf("invalid schema name: exceeds max length")
	}
	if !schemaName.MatchString(name) {
		return fmt.Errorf("invalid schema name: must start with a letter or underscore, and contain only letters, digits, underscores and hyphens")
	}
	return nil
}

func connString(cfg config.Database) (string, error) {
	if cfg.URL != "" {
		return cfg.URL, nil
	}
	host, port, err := net.SplitHostPort(cfg.Endpoint)
	if err != nil {
		host, port = cfg.Endpoint, "5432"
	}
	return fmt.Sprintf("host=%s port=%s dbname=%s user=%s password=%s",
		quoteValue(host), quoteValue(port), quoteValue(cfg.Name), quoteValue(cfg.Username), quoteValue(cfg.Password)), nil
}

func quoteValue(value string) string {
	return "'" + strings.NewReplacer(`\`, `\\`, `'`, `\'`).Replace(value) + "'"
}

// Connect opens a pool whose sessions use the schema and the service's time zone, as the Kotlin
// service's did, so date functions such as DATE(created_at) give the same results.
func Connect(ctx context.Context, cfg config.Database, timeZone string) (*pgxpool.Pool, error) {
	if err := ValidateSchemaName(cfg.Schema); err != nil {
		return nil, err
	}
	conn, err := connString(cfg)
	if err != nil {
		return nil, err
	}
	poolConfig, err := pgxpool.ParseConfig(conn)
	if err != nil {
		return nil, err
	}
	poolConfig.ConnConfig.RuntimeParams["search_path"] = quoteIdentifier(cfg.Schema)
	poolConfig.ConnConfig.RuntimeParams["TimeZone"] = timeZone
	poolConfig.MaxConns = cfg.PoolSize
	poolConfig.MinConns = 2
	poolConfig.MaxConnLifetime = 30 * time.Minute
	poolConfig.MaxConnIdleTime = 10 * time.Minute

	pool, err := pgxpool.NewWithConfig(ctx, poolConfig)
	if err != nil {
		return nil, err
	}
	if _, err := pool.Exec(ctx, "CREATE SCHEMA IF NOT EXISTS "+quoteIdentifier(cfg.Schema)); err != nil {
		pool.Close()
		return nil, err
	}
	return pool, nil
}

func quoteIdentifier(name string) string { return `"` + name + `"` }

type migration struct {
	version     int64
	description string
	sql         string
	checksum    string
}

func migrations() ([]migration, error) {
	entries, err := fs.ReadDir(migrationFiles, "migrations")
	if err != nil {
		return nil, err
	}
	var found []migration
	for _, entry := range entries {
		number, description, ok := strings.Cut(strings.TrimSuffix(entry.Name(), ".sql"), "_")
		version, err := strconv.ParseInt(number, 10, 64)
		if !ok || err != nil {
			return nil, fmt.Errorf("migration %s isn't named NNNN_description.sql", entry.Name())
		}
		content, err := fs.ReadFile(migrationFiles, "migrations/"+entry.Name())
		if err != nil {
			return nil, err
		}
		sum := sha256.Sum256(content)
		found = append(found, migration{version, strings.ReplaceAll(description, "_", " "), string(content), hex.EncodeToString(sum[:])})
	}
	sort.Slice(found, func(i, j int) bool { return found[i].version < found[j].version })
	return found, nil
}

// Migrate applies migrations that haven't been, each in its own transaction, recording them in
// schema_migrations.
//
// The first time it runs against a database the Kotlin service (Flyway) or the Rust service
// (sqlx) migrated, it records the migrations they applied as already applied, so they aren't run
// again.
func Migrate(ctx context.Context, pool *pgxpool.Pool) error {
	all, err := migrations()
	if err != nil {
		return err
	}

	conn, err := pool.Acquire(ctx)
	if err != nil {
		return err
	}
	defer conn.Release()
	if _, err := conn.Exec(ctx, "SELECT pg_advisory_lock($1)", migrationLock); err != nil {
		return err
	}
	defer conn.Exec(context.WithoutCancel(ctx), "SELECT pg_advisory_unlock($1)", migrationLock) //nolint:errcheck

	if _, err := conn.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS schema_migrations (
			version BIGINT PRIMARY KEY,
			description TEXT NOT NULL,
			checksum TEXT NOT NULL,
			installed_on TIMESTAMPTZ NOT NULL DEFAULT now()
		)`); err != nil {
		return err
	}

	applied := map[int64]string{}
	rows, err := conn.Query(ctx, "SELECT version, checksum FROM schema_migrations")
	if err != nil {
		return err
	}
	for rows.Next() {
		var version int64
		var checksum string
		if err := rows.Scan(&version, &checksum); err != nil {
			return err
		}
		applied[version] = checksum
	}
	if err := rows.Err(); err != nil {
		return err
	}

	if len(applied) == 0 {
		if err := adoptHistory(ctx, conn.Conn(), all, applied); err != nil {
			return err
		}
	}

	for _, m := range all {
		if checksum, ok := applied[m.version]; ok {
			if checksum != m.checksum {
				return fmt.Errorf("migration %d (%s) has changed since it was applied", m.version, m.description)
			}
			continue
		}
		if err := pgx.BeginFunc(ctx, conn.Conn(), func(tx pgx.Tx) error {
			if _, err := tx.Exec(ctx, m.sql); err != nil {
				return fmt.Errorf("migration %d (%s): %w", m.version, m.description, err)
			}
			_, err := tx.Exec(ctx, "INSERT INTO schema_migrations (version, description, checksum) VALUES ($1, $2, $3)",
				m.version, m.description, m.checksum)
			return err
		}); err != nil {
			return err
		}
		slog.Info("Applied migration", "version", m.version, "description", m.description)
	}
	return nil
}

func adoptHistory(ctx context.Context, conn *pgx.Conn, all []migration, applied map[int64]string) error {
	var versions []int64
	for table, query := range map[string]string{
		"flyway_schema_history": "SELECT version::bigint FROM flyway_schema_history WHERE success AND version IS NOT NULL",
		"_sqlx_migrations":      "SELECT version FROM _sqlx_migrations WHERE success",
	} {
		var exists bool
		if err := conn.QueryRow(ctx, "SELECT to_regclass($1) IS NOT NULL", table).Scan(&exists); err != nil {
			return err
		}
		if !exists {
			continue
		}
		rows, err := conn.Query(ctx, query)
		if err != nil {
			return err
		}
		found, err := pgx.CollectRows(rows, pgx.RowTo[int64])
		if err != nil {
			return err
		}
		versions = append(versions, found...)
	}

	adopted := 0
	for _, m := range all {
		if !slices.Contains(versions, m.version) {
			continue
		}
		if _, err := conn.Exec(ctx, "INSERT INTO schema_migrations (version, description, checksum) VALUES ($1, $2, $3)",
			m.version, m.description, m.checksum); err != nil {
			return err
		}
		applied[m.version] = m.checksum
		adopted++
	}
	if adopted > 0 {
		slog.Info("Adopted migrations already applied by the Kotlin or Rust service", "count", adopted)
	}
	return nil
}
