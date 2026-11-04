// Package cache keeps the latest version of each assessment in Redis, for other services to read.
package cache

import (
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"log/slog"
	"strconv"
	"time"

	"github.com/google/uuid"
	"github.com/redis/go-redis/v9"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/config"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/query"
)

const (
	ttl = 10 * time.Minute
	// Operations beyond this many waiting are dropped rather than slowing requests down.
	queueSize = 1000
)

func Key(assessmentUUID uuid.UUID) string { return fmt.Sprintf("assessment:%s:latest", assessmentUUID) }

type operation struct {
	key     string
	payload []byte // nil to evict
}

// Service writes to Redis in the background, one operation at a time in the order they were
// asked for, so an eviction can't overtake a later write of the same assessment.
type Service struct {
	client     *redis.Client
	operations chan operation
}

func New(cfg config.Redis) *Service {
	options := &redis.Options{
		Addr:         cfg.Host + ":" + strconv.Itoa(cfg.Port),
		Password:     cfg.Password,
		DialTimeout:  cfg.ConnectTimeout,
		ReadTimeout:  cfg.Timeout,
		WriteTimeout: cfg.Timeout,
	}
	if cfg.SSL {
		options.TLSConfig = &tls.Config{MinVersion: tls.VersionTLS12, ServerName: cfg.Host}
	}
	service := &Service{client: redis.NewClient(options), operations: make(chan operation, queueSize)}
	go service.work()
	return service
}

// Disabled caches nothing.
func Disabled() *Service { return &Service{} }

func (s *Service) work() {
	for op := range s.operations {
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		var err error
		if op.payload == nil {
			err = s.client.Del(ctx, op.key).Err()
		} else {
			err = s.client.Set(ctx, op.key, op.payload, ttl).Err()
		}
		cancel()
		if err != nil {
			slog.Error("Failed to update the assessment cache", "key", op.key, "error", err)
		}
	}
}

func (s *Service) enqueue(op operation) {
	if s.client == nil {
		return
	}
	select {
	case s.operations <- op:
	default:
		slog.Error("Assessment cache is falling behind, so an update was dropped", "key", op.key)
	}
}

func (s *Service) CacheLatest(result *query.AssessmentVersionResult) {
	payload, err := json.Marshal(result)
	if err != nil {
		slog.Error("Failed to cache latest assessment", "assessment", result.AssessmentUUID, "error", err)
		return
	}
	s.enqueue(operation{key: Key(result.AssessmentUUID), payload: payload})
}

func (s *Service) EvictLatest(assessmentUUID uuid.UUID) {
	s.enqueue(operation{key: Key(assessmentUUID)})
}

// Ping is for the health check. Enabled is false when caching is disabled.
func (s *Service) Ping(ctx context.Context) (enabled bool, err error) {
	if s.client == nil {
		return false, nil
	}
	return true, s.client.Ping(ctx).Err()
}
