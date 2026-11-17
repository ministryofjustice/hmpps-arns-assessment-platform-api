// Package plan has behaviour specific to sentence plans.
package plan

import (
	"context"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/command"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/query"
)

const removedNote = "Automatically removed as the previous supervision period has ended."

type NewPeriodOfSupervisionRequest struct {
	User           model.UserDetails `json:"user" required:"true"`
	AssessmentUUID uuid.UUID         `json:"assessmentUuid" required:"true"`
}

type Service struct {
	Commands *command.Dispatcher
	Queries  *query.Dispatcher
}

// StartNewPeriodOfSupervision removes the plan's active and future goals, noting why, and its
// agreements.
func (s *Service) StartNewPeriodOfSupervision(ctx context.Context, c clock.Clock, request NewPeriodOfSupervisionRequest) error {
	versionQuery := &query.AssessmentVersion{AssessmentIdentifier: query.ByUUID(request.AssessmentUUID)}
	versionQuery.User = request.User
	result, err := s.Queries.Dispatch(ctx, s.Queries.Pool, c, versionQuery)
	if err != nil {
		return err
	}
	plan := result.(*query.AssessmentVersionResult)
	if plan.AssessmentType != "SENTENCE_PLAN" {
		return apperr.AssessmentNotPlan(request.AssessmentUUID)
	}

	now := clock.System().Now().JavaString()
	assessment := command.ReferenceTo(request.AssessmentUUID)
	base := func() commonFields { return commonFields{User: request.User, Assessment: assessment} }

	var goalsRemoved int
	var commands []command.Command
	for _, collection := range plan.Collections {
		if collection.Name != "GOALS" {
			continue
		}
		for _, goal := range collection.Items {
			status, _ := goal.Properties.Get("status")
			if value, ok := status.SingleValue(); !ok || (value != "ACTIVE" && value != "FUTURE") {
				continue
			}
			goalsRemoved++

			var notesUUID uuid.UUID
			var createNotes command.Command
			for _, nested := range goal.Collections {
				if nested.Name == "NOTES" {
					notesUUID = nested.UUID
					break
				}
			}
			if notesUUID == uuid.Nil {
				notesUUID = uuid.New()
				parent := command.ReferenceTo(goal.UUID)
				createNotes = base().createCollection("NOTES", &parent, notesUUID)
			}

			var removedStatus, emptyAnswers, note, noteProperties model.Answers
			removedStatus.Set("status", model.Single("REMOVED"))
			removedStatus.Set("status_date", model.Single(now))
			note.Set("note", model.Single(removedNote))
			note.Set("created_by", model.Single("System"))
			noteProperties.Set("type", model.Single("REMOVED"))
			noteProperties.Set("created_at", model.Single(now))

			commands = append(commands,
				base().updateItemProperties(goal.UUID, removedStatus, []string{}),
				base().updateItemAnswers(goal.UUID, emptyAnswers, []string{"target_date"}))
			if createNotes != nil {
				commands = append(commands, createNotes)
			}
			commands = append(commands, base().addItem(notesUUID, note, noteProperties))
		}
		break
	}

	for _, collection := range plan.Collections {
		if collection.Name == "PLAN_AGREEMENTS" {
			for _, agreement := range collection.Items {
				commands = append(commands, base().removeItem(agreement.UUID))
			}
			break
		}
	}

	commands = append(commands, &command.CreateTimelineItem{
		User: request.User, Assessment: assessment, Timestamp: clock.System().Now(),
		Timeline: model.Timeline{Type: "NEW_PERIOD_OF_SUPERVISION", Data: jsonx.ObjectOf("Goals removed", goalsRemoved)},
	})

	_, err = s.Commands.Dispatch(ctx, c, commands)
	return err
}

// commonFields builds commands for the plan's assessment on behalf of the user.
type commonFields struct {
	User       model.UserDetails
	Assessment command.Reference
}

func (f commonFields) createCollection(name string, parent *command.Reference, collectionUUID uuid.UUID) command.Command {
	created := &command.CreateCollection{Name: name, ParentCollectionItemUUID: parent, CollectionUUID: collectionUUID}
	created.User, created.Assessment = f.User, f.Assessment
	return created
}

func (f commonFields) updateItemProperties(item uuid.UUID, added model.Answers, removed []string) command.Command {
	update := &command.UpdateCollectionItemProperties{CollectionItemUUID: command.ReferenceTo(item), Added: added, Removed: removed}
	update.User, update.Assessment = f.User, f.Assessment
	return update
}

func (f commonFields) updateItemAnswers(item uuid.UUID, added model.Answers, removed []string) command.Command {
	update := &command.UpdateCollectionItemAnswers{CollectionItemUUID: command.ReferenceTo(item), Added: added, Removed: removed}
	update.User, update.Assessment = f.User, f.Assessment
	return update
}

func (f commonFields) addItem(collection uuid.UUID, answers, properties model.Answers) command.Command {
	add := &command.AddCollectionItem{
		CollectionUUID: command.ReferenceTo(collection), Answers: answers, Properties: properties, CollectionItemUUID: uuid.New(),
	}
	add.User, add.Assessment = f.User, f.Assessment
	return add
}

func (f commonFields) removeItem(item uuid.UUID) command.Command {
	remove := &command.RemoveCollectionItem{CollectionItemUUID: command.ReferenceTo(item)}
	remove.User, remove.Assessment = f.User, f.Assessment
	return remove
}
