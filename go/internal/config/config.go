// Package config reads configuration from the environment, using the same variables the Helm
// chart already provides to the Kotlin service.
package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	ServerPort   int
	ServiceName  string
	TimeZone     string
	Database     Database
	HmppsAuthURL string
	Redis        Redis
	Audit        Audit
	// MaxLookbackDays is the oldest "since" the "modified/deleted since" queries accept; 0 disables the check.
	MaxLookbackDays int
}

type Database struct {
	// URL overrides the individual connection settings when set.
	URL      string
	Endpoint string
	Name     string
	Username string
	Password string
	Schema   string
	PoolSize int32
}

type Redis struct {
	Host           string
	Port           int
	Password       string
	SSL            bool
	Timeout        time.Duration
	ConnectTimeout time.Duration
}

type Audit struct {
	// QueueName is empty when audit events should only be logged.
	QueueName     string
	LocalstackURL string
}

func FromEnv() (Config, error) {
	r := reader{}
	cfg := Config{
		ServerPort:  r.int("SERVER_PORT", 8080),
		ServiceName: "hmpps-arns-assessment-platform-api",
		TimeZone:    r.string("TZ", "Europe/London"),
		Database: Database{
			URL:      os.Getenv("DATABASE_URL"),
			Endpoint: r.string("DATABASE_ENDPOINT", "localhost:5432"),
			Name:     r.string("DATABASE_NAME", "postgres"),
			Username: r.string("DATABASE_USERNAME", "root"),
			Password: r.string("DATABASE_PASSWORD", "dev"),
			Schema:   r.string("DATABASE_SCHEMA_NAME", "assessment-platform"),
			PoolSize: int32(r.int("DATABASE_POOL_SIZE", 20)),
		},
		HmppsAuthURL: r.string("HMPPS_AUTH_URL", "http://localhost:8090/auth"),
		// Spring Boot also read its own property names from the environment, and they won over
		// the service's variables (the UI repo's compose file sets SPRING_DATA_REDIS_HOST, for
		// example), so they're honoured too, to keep this a drop-in replacement.
		Redis: Redis{
			Host:           r.string(springOr("SPRING_DATA_REDIS_HOST", "CACHE_REDIS_HOST"), "localhost"),
			Port:           r.int(springOr("SPRING_DATA_REDIS_PORT", "CACHE_REDIS_PORT"), 6379),
			Password:       r.string(springOr("SPRING_DATA_REDIS_PASSWORD", "CACHE_REDIS_AUTH_TOKEN"), ""),
			SSL:            r.bool(springOr("SPRING_DATA_REDIS_SSL_ENABLED", "CACHE_REDIS_SSL_ENABLED"), false),
			Timeout:        r.duration(springOr("SPRING_DATA_REDIS_TIMEOUT", "CACHE_REDIS_TIMEOUT"), 5*time.Second),
			ConnectTimeout: r.duration(springOr("SPRING_DATA_REDIS_CONNECT_TIMEOUT", "CACHE_REDIS_CONNECT_TIMEOUT"), 2*time.Second),
		},
		Audit:           Audit{QueueName: os.Getenv("HMPPS_SQS_QUEUES_AUDIT_QUEUE_NAME")},
		MaxLookbackDays: r.int("QUERY_MAX_LOOKBACK_DAYS", 1),
	}
	if os.Getenv("HMPPS_SQS_PROVIDER") == "localstack" {
		cfg.Audit.LocalstackURL = r.string("HMPPS_SQS_LOCALSTACK_URL", "http://localhost:4566")
	}
	return cfg, r.err
}

func springOr(springName, name string) string {
	if os.Getenv(springName) != "" {
		return springName
	}
	return name
}

// reader keeps the first error, so every variable can be read before checking.
type reader struct{ err error }

func (r *reader) fail(name, value, reason string) {
	if r.err == nil {
		r.err = fmt.Errorf("invalid value '%s' for %s: %s", value, name, reason)
	}
}

func (r *reader) string(name, fallback string) string {
	if value := os.Getenv(name); value != "" {
		return value
	}
	return fallback
}

func (r *reader) int(name string, fallback int) int {
	value := os.Getenv(name)
	if value == "" {
		return fallback
	}
	parsed, err := strconv.Atoi(value)
	if err != nil {
		r.fail(name, value, "expected a number")
	}
	return parsed
}

func (r *reader) bool(name string, fallback bool) bool {
	value := os.Getenv(name)
	if value == "" {
		return fallback
	}
	parsed, err := strconv.ParseBool(value)
	if err != nil {
		r.fail(name, value, "expected true or false")
	}
	return parsed
}

func (r *reader) duration(name string, fallback time.Duration) time.Duration {
	value := os.Getenv(name)
	if value == "" {
		return fallback
	}
	parsed, ok := ParseDuration(value)
	if !ok {
		r.fail(name, value, "expected a duration such as 5s or 2000ms")
	}
	return parsed
}

// ParseDuration parses Spring-style durations such as 5s, 2000ms, 1m, or a bare number of seconds.
func ParseDuration(value string) (time.Duration, bool) {
	split := strings.IndexFunc(value, func(r rune) bool { return r < '0' || r > '9' })
	if split == -1 {
		split = len(value)
	}
	amount, err := strconv.Atoi(value[:split])
	if err != nil {
		return 0, false
	}
	switch value[split:] {
	case "ms":
		return time.Duration(amount) * time.Millisecond, true
	case "", "s":
		return time.Duration(amount) * time.Second, true
	case "m":
		return time.Duration(amount) * time.Minute, true
	}
	return 0, false
}
