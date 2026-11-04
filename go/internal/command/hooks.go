package command

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/engine"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/event"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/formconfig"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/oasys"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/store"
)

// Hook is extra work a command asks for once it has been handled, identified by its "type".
type Hook struct {
	UpdateOasysDataMapping *UpdateOasysDataMapping
}

// UpdateOasysDataMapping stores the OASys equivalent of the assessment's answers in its
// oasys_equivalent property.
type UpdateOasysDataMapping struct {
	FormConfig formconfig.FormConfig `json:"formConfig" required:"true"`
}

func (h Hook) MarshalJSON() ([]byte, error) {
	return jsonx.Tag("UpdateOasysDataMapping", h.UpdateOasysDataMapping)
}

func (h *Hook) UnmarshalJSON(data []byte) error {
	typeName, err := jsonx.TypeOf(data)
	if err != nil {
		return err
	}
	if typeName != "UpdateOasysDataMapping" {
		return fmt.Errorf("could not resolve type id '%s' as a hook", typeName)
	}
	h.UpdateOasysDataMapping = &UpdateOasysDataMapping{}
	return jsonx.Unmarshal(data, h.UpdateOasysDataMapping)
}

func runHooks(ctx context.Context, db store.DB, c clock.Clock, u *engine.UnitOfWork, command Command) error {
	for _, hook := range command.CommandHooks() {
		if hook.UpdateOasysDataMapping != nil {
			if err := updateOasysDataMapping(ctx, db, c, u, command, hook.UpdateOasysDataMapping); err != nil {
				return err
			}
		}
	}
	return nil
}

func updateOasysDataMapping(
	ctx context.Context, db store.DB, c clock.Clock, u *engine.UnitOfWork, command Command, hook *UpdateOasysDataMapping,
) error {
	assessment, err := u.FindAssessment(ctx, db, command.AssessmentUUID())
	if err != nil {
		return err
	}
	state, err := u.State(ctx, db, c, assessment, nil)
	if err != nil {
		return err
	}
	equivalent, err := oasys.OasysEquivalent(state.Latest().Data, &hook.FormConfig)
	if err != nil {
		return err
	}
	encoded, err := json.Marshal(equivalent)
	if err != nil {
		return err
	}

	user, err := u.FindUser(ctx, db, command.Requester())
	if err != nil {
		return err
	}
	var added model.Answers
	added.Set("oasys_equivalent", model.Single(string(encoded)))
	e := entity.NewEvent(user, assessment, event.AssessmentPropertiesUpdated{Added: added, Removed: []string{}},
		c.RequestDateTime(), command.Autosaved())

	// The mapping isn't something the user did, so it isn't added to the timeline.
	_, err = engine.HandleEvent(ctx, db, c, u, e)
	return err
}
