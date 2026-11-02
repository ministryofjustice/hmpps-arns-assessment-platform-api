package command

import (
	"context"
	"fmt"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/engine"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/event"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/store"
)

func handle(ctx context.Context, db store.DB, c clock.Clock, u *engine.UnitOfWork, command Command) (Result, error) {
	// Most commands raise a single event; the rest are handled here and return early.
	var data event.Event
	result := success()
	switch cmd := command.(type) {
	case *CreateAssessment:
		return createAssessment(ctx, db, c, u, cmd)
	case *CreateTimelineItem:
		assessment, err := u.FindAssessment(ctx, db, cmd.AssessmentUUID())
		if err != nil {
			return nil, err
		}
		user, err := u.FindUser(ctx, db, cmd.User)
		if err != nil {
			return nil, err
		}
		u.Timeline = append(u.Timeline, entity.CustomTimeline(cmd.Timestamp, user, assessment.UUID, &cmd.Timeline))
		return result, nil
	case *SoftDelete:
		return result, rewriteHistory(ctx, db, c, u, command, func(assessment uuid.UUID) error {
			if err := store.SoftDeleteEventsFrom(ctx, db, assessment, cmd.PointInTime); err != nil {
				return err
			}
			return store.SoftDeleteTimelineFrom(ctx, db, assessment, cmd.PointInTime)
		})
	case *Undelete:
		return result, rewriteHistory(ctx, db, c, u, command, func(assessment uuid.UUID) error {
			if err := engine.Undelete(ctx, db, assessment, cmd.PointInTime); err != nil {
				return err
			}
			return store.UndeleteTimelineFrom(ctx, db, assessment, cmd.PointInTime)
		})
	case *AddCollectionItem:
		data = event.CollectionItemAdded{
			CollectionItemUUID: cmd.CollectionItemUUID, CollectionUUID: cmd.CollectionUUID.UUID(),
			Answers: cmd.Answers.Clone(), Properties: cmd.Properties.Clone(), Index: cmd.Index,
		}
		result = addedCollectionItem(cmd.CollectionItemUUID)
	case *CreateCollection:
		created := event.CollectionCreated{CollectionUUID: cmd.CollectionUUID, Name: cmd.Name}
		if cmd.ParentCollectionItemUUID != nil {
			parent := cmd.ParentCollectionItemUUID.UUID()
			created.ParentCollectionItemUUID = &parent
		}
		data, result = created, createdCollection(cmd.CollectionUUID)
	case *RemoveCollectionItem:
		data = event.CollectionItemRemoved{CollectionItemUUID: cmd.CollectionItemUUID.UUID()}
	case *ReorderCollectionItem:
		data = event.CollectionItemReordered{CollectionItemUUID: cmd.CollectionItemUUID.UUID(), Index: cmd.Index}
	case *Rollback:
		data = event.AssessmentRolledBack{RolledBackTo: cmd.PointInTime}
	case *UpdateAssessmentAnswers:
		data = event.AssessmentAnswersUpdated{Added: cmd.Added.Clone(), Removed: cmd.Removed}
	case *UpdateAssessmentProperties:
		data = event.AssessmentPropertiesUpdated{Added: cmd.Added.Clone(), Removed: cmd.Removed}
	case *UpdateCollectionItemAnswers:
		data = event.CollectionItemAnswersUpdated{CollectionItemUUID: cmd.CollectionItemUUID.UUID(), Added: cmd.Added.Clone(), Removed: cmd.Removed}
	case *UpdateCollectionItemProperties:
		data = event.CollectionItemPropertiesUpdated{CollectionItemUUID: cmd.CollectionItemUUID.UUID(), Added: cmd.Added.Clone(), Removed: cmd.Removed}
	case *UpdateFlags:
		data = event.AssessmentFlagsUpdated{Flags: cmd.Flags}
	case *UpdateFormVersion:
		data = event.FormVersionUpdated{Version: cmd.Version}
	default:
		return nil, fmt.Errorf("no handler for %s", command.Type())
	}

	user, err := u.FindUser(ctx, db, command.Requester())
	if err != nil {
		return nil, err
	}
	assessment, err := u.FindAssessment(ctx, db, command.AssessmentUUID())
	if err != nil {
		return nil, err
	}
	drafts, err := engine.HandleEvent(ctx, db, c, u, entity.NewEvent(user, assessment, data, c.RequestDateTime(), command.Autosaved()))
	if err != nil {
		return nil, err
	}
	u.AddTimeline(drafts, command.CustomTimeline())
	return result, nil
}

func createAssessment(ctx context.Context, db store.DB, c clock.Clock, u *engine.UnitOfWork, cmd *CreateAssessment) (Result, error) {
	createdAt := c.RequestDateTime()
	assessment := &entity.Assessment{UUID: cmd.GeneratedAssessment, CreatedAt: createdAt, Type: cmd.AssessmentType}
	if cmd.Identifiers != nil {
		for identifierType, id := range cmd.Identifiers.All() {
			assessment.Identifiers = append(assessment.Identifiers, entity.AssessmentIdentifier{
				UUID: uuid.New(), CreatedAt: createdAt, Pair: model.IdentifierPair{Type: model.IdentifierType(identifierType), ID: id},
			})
		}
	}
	u.AddAssessment(assessment)

	user, err := u.FindUser(ctx, db, cmd.User)
	if err != nil {
		return nil, err
	}
	var properties model.Answers
	if cmd.Properties != nil {
		properties = cmd.Properties.Clone()
	}
	created := entity.NewEvent(user, assessment,
		event.AssessmentCreated{FormVersion: cmd.FormVersion, Properties: properties, Flags: orEmpty(cmd.Flags)}, createdAt, false)
	assigned := entity.NewEvent(user, assessment, event.AssignedToUser{UserUUID: user.UUID}, assessment.CreatedAt, false)

	for _, e := range []*entity.Event{created, assigned} {
		drafts, err := engine.HandleEvent(ctx, db, c, u, e)
		if err != nil {
			return nil, err
		}
		u.AddTimeline(drafts, cmd.Timeline)
	}
	return createdAssessment(assessment.UUID), nil
}

// rewriteHistory changes the assessment's events with change, then replaces its snapshots with
// ones rebuilt from the events that remain.
func rewriteHistory(
	ctx context.Context, db store.DB, c clock.Clock, u *engine.UnitOfWork, command Command, change func(uuid.UUID) error,
) error {
	assessment, err := u.FindAssessment(ctx, db, command.AssessmentUUID())
	if err != nil {
		return err
	}
	if err := change(assessment.UUID); err != nil {
		return err
	}
	_, evicted, err := engine.RebuildState(ctx, db, c, assessment, nil)
	if err != nil {
		return err
	}
	u.Evictions = append(u.Evictions, evicted...)

	if custom := command.CustomTimeline(); custom != nil {
		user, err := u.FindUser(ctx, db, command.Requester())
		if err != nil {
			return err
		}
		u.Timeline = append(u.Timeline, entity.CustomTimeline(c.RequestDateTime(), user, assessment.UUID, custom))
	}
	return nil
}
