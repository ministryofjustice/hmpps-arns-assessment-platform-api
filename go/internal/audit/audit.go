// Package audit sends audit events to the HMPPS audit SQS queue.
package audit

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"os"
	"sync"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/credentials"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
	"github.com/aws/aws-sdk-go-v2/service/sqs/types"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/command"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/config"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/query"
)

// SQS accepts at most ten messages per batch.
const batchSize = 10

const sendTimeout = 30 * time.Second

type event struct {
	Who     string `json:"who"`
	What    string `json:"what"`
	When    string `json:"when"`
	Service string `json:"service"`
	Details string `json:"details"`
}

type Service struct {
	serviceName string
	queue       *queue
}

type queue struct {
	client *sqs.Client
	name   string

	mu  sync.Mutex
	url string
}

// New sends audit events to the configured queue, or only logs them when there isn't one.
func New(ctx context.Context, cfg config.Config) (*Service, error) {
	service := &Service{serviceName: cfg.ServiceName}
	if cfg.Audit.QueueName == "" {
		slog.Warn("No audit queue configured, so audit events will only be logged")
		return service, nil
	}

	region := os.Getenv("AWS_REGION")
	if region == "" {
		region = "eu-west-2"
	}
	options := []func(*awsconfig.LoadOptions) error{awsconfig.WithRegion(region)}
	if cfg.Audit.LocalstackURL != "" {
		options = append(options, awsconfig.WithCredentialsProvider(
			credentials.NewStaticCredentialsProvider("localstack", "localstack", "")))
	}
	awsConfig, err := awsconfig.LoadDefaultConfig(ctx, options...)
	if err != nil {
		return nil, err
	}
	client := sqs.NewFromConfig(awsConfig, func(o *sqs.Options) {
		if cfg.Audit.LocalstackURL != "" {
			o.BaseEndpoint = aws.String(cfg.Audit.LocalstackURL)
		}
	})
	service.queue = &queue{client: client, name: cfg.Audit.QueueName}
	return service, nil
}

// Disabled only logs audit events.
func Disabled(serviceName string) *Service { return &Service{serviceName: serviceName} }

func now() string { return time.Now().UTC().Format(time.RFC3339Nano) }

func (s *Service) event(who, what string, details any) event {
	encoded, _ := json.Marshal(details)
	return event{Who: who, What: what, When: now(), Service: s.serviceName, Details: string(encoded)}
}

func (s *Service) AuditCommands(commands []command.Command) {
	events := make([]event, 0, len(commands))
	for _, c := range commands {
		events = append(events, s.event(c.Requester().ID, c.Type(), jsonx.ObjectOf("assessmentUuid", c.AssessmentUUID())))
	}
	s.send(events)
}

func (s *Service) AuditQuery(q query.Query) {
	var details jsonx.Object
	switch q := q.(type) {
	case *query.AssessmentVersion:
		details = jsonx.ObjectOf("assessmentIdentifier", q.AssessmentIdentifier)
	case *query.CollectionItem:
		details = jsonx.ObjectOf("assessmentIdentifier", q.AssessmentIdentifier)
	case *query.Collection:
		details = jsonx.ObjectOf("assessmentIdentifier", q.AssessmentIdentifier)
	case *query.DailyVersions:
		details = jsonx.ObjectOf("assessmentIdentifier", q.AssessmentIdentifier)
	case *query.Timeline:
		if q.AssessmentIdentifier != nil {
			details.SetAll(jsonx.ObjectOf("assessmentIdentifier", q.AssessmentIdentifier))
		}
		if q.Subject != nil {
			details.SetAll(jsonx.ObjectOf("subject", q.Subject.ID))
		}
	case *query.AssessmentsModifiedSince:
		details = jsonx.ObjectOf("assessmentType", q.AssessmentType, "since", q.Since)
	case *query.AssessmentsSoftDeletedSince:
		details = jsonx.ObjectOf("assessmentType", q.AssessmentType, "since", q.Since)
	}
	s.send([]event{s.event(q.Requester().ID, q.Type(), details)})
}

func (s *Service) AuditSubjectAccessRequest(q *query.SubjectAccessRequest) {
	var timestamp *string
	if q.Timestamp != nil {
		instant := q.Timestamp.Instant().UTC().Format(time.RFC3339Nano)
		timestamp = &instant
	}
	details := jsonx.ObjectOf("subject", q.AssessmentIdentifiers, "from", q.From, "to", q.To, "timestamp", timestamp)
	s.send([]event{s.event("SAR", "SubjectAccessRequestQuery", details)})
}

// Audit records something that isn't a command or query.
func (s *Service) Audit(who, what, details string) {
	s.send([]event{{Who: who, What: what, When: now(), Service: s.serviceName, Details: details}})
}

// send works in the background, so auditing never fails or slows down a request.
func (s *Service) send(events []event) {
	if len(events) == 0 {
		return
	}
	if s.queue == nil {
		for _, e := range events {
			slog.Info("Audit event", "who", e.Who, "what", e.What, "details", e.Details)
		}
		return
	}
	go func() {
		ctx, cancel := context.WithTimeout(context.Background(), sendTimeout)
		defer cancel()
		if err := s.queue.send(ctx, events); err != nil {
			slog.Error("Error during sending audit messages", "error", err)
		}
	}()
}

func (q *queue) queueURL(ctx context.Context) (string, error) {
	q.mu.Lock()
	defer q.mu.Unlock()
	if q.url != "" {
		return q.url, nil
	}
	out, err := q.client.GetQueueUrl(ctx, &sqs.GetQueueUrlInput{QueueName: aws.String(q.name)})
	if err != nil {
		return "", fmt.Errorf("unable to find audit queue '%s': %w", q.name, err)
	}
	q.url = aws.ToString(out.QueueUrl)
	return q.url, nil
}

func (q *queue) send(ctx context.Context, events []event) error {
	url, err := q.queueURL(ctx)
	if err != nil {
		return err
	}
	batches := (len(events) + batchSize - 1) / batchSize
	for batch := range batches {
		first := batch * batchSize
		last := min(first+batchSize, len(events))
		slog.Info(fmt.Sprintf("Sending batch %d/%d - audit events %d to %d", batch+1, batches, first, last))

		entries := make([]types.SendMessageBatchRequestEntry, 0, last-first)
		for i := first; i < last; i++ {
			body, err := json.Marshal(events[i])
			if err != nil {
				return err
			}
			entries = append(entries, types.SendMessageBatchRequestEntry{
				Id: aws.String(fmt.Sprintf("msg-%d", i)), MessageBody: aws.String(string(body)),
			})
		}
		if _, err := q.client.SendMessageBatch(ctx, &sqs.SendMessageBatchInput{QueueUrl: aws.String(url), Entries: entries}); err != nil {
			return err
		}
	}
	slog.Info(fmt.Sprintf("Successfully sent %d audit events", len(events)))
	return nil
}
