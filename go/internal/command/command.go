// Package command handles changes to assessments: each batch of commands is applied in one
// transaction, raising events that update the assessment aggregate.
package command

import (
	"encoding/json"
	"fmt"
	"strconv"
	"strings"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/omap"
)

// Command is a change to an assessment, identified in JSON by its "type".
type Command interface {
	Type() string
	Requester() model.UserDetails
	// AssessmentUUID is the assessment the command applies to. References must be resolved first.
	AssessmentUUID() uuid.UUID
	CustomTimeline() *model.Timeline
	CommandHooks() []Hook
	// Autosaved is only ever true for the commands that can be autosaved.
	Autosaved() bool
	references() []*Reference
}

// common has the fields most commands share.
type common struct {
	User       model.UserDetails `json:"user" required:"true"`
	Assessment Reference         `json:"assessmentUuid" required:"true"`
	Timeline   *model.Timeline   `json:"timeline"`
	Hooks      []Hook            `json:"hooks"`
}

func (c *common) Requester() model.UserDetails    { return c.User }
func (c *common) AssessmentUUID() uuid.UUID       { return c.Assessment.UUID() }
func (c *common) CustomTimeline() *model.Timeline { return c.Timeline }
func (c *common) CommandHooks() []Hook            { return c.Hooks }
func (c *common) Autosaved() bool                 { return false }
func (c *common) references() []*Reference        { return []*Reference{&c.Assessment} }

type autosave struct {
	Autosave bool `json:"autosaved"`
}

type AddCollectionItem struct {
	common
	autosave
	CollectionUUID     Reference     `json:"collectionUuid" required:"true"`
	Answers            model.Answers `json:"answers" required:"true"`
	Properties         model.Answers `json:"properties" required:"true"`
	Index              *int          `json:"index"`
	CollectionItemUUID uuid.UUID     `json:"-"`
}

type CreateAssessment struct {
	User                model.UserDetails `json:"user" required:"true"`
	FormVersion         string            `json:"formVersion" required:"true"`
	AssessmentType      string            `json:"assessmentType" required:"true"`
	Identifiers         *omap.Map[string] `json:"identifiers"`
	Properties          *model.Answers    `json:"properties"`
	Flags               []string          `json:"flags"`
	Timeline            *model.Timeline   `json:"timeline"`
	Hooks               []Hook            `json:"hooks"`
	GeneratedAssessment uuid.UUID         `json:"-"`
}

type CreateCollection struct {
	common
	autosave
	Name                     string     `json:"name" required:"true"`
	ParentCollectionItemUUID *Reference `json:"parentCollectionItemUuid"`
	CollectionUUID           uuid.UUID  `json:"-"`
}

type CreateTimelineItem struct {
	User       model.UserDetails      `json:"user" required:"true"`
	Assessment Reference              `json:"assessmentUuid" required:"true"`
	Timestamp  datetime.LocalDateTime `json:"timestamp" required:"true"`
	Timeline   model.Timeline         `json:"timeline" required:"true"`
	Hooks      []Hook                 `json:"hooks"`
}

type RemoveCollectionItem struct {
	common
	CollectionItemUUID Reference `json:"collectionItemUuid" required:"true"`
}

type ReorderCollectionItem struct {
	common
	autosave
	CollectionItemUUID Reference `json:"collectionItemUuid" required:"true"`
	Index              int       `json:"index" required:"true"`
}

type Rollback struct {
	common
	PointInTime datetime.LocalDateTime `json:"pointInTime" required:"true"`
}

// SoftDelete marks every event and timeline entry from the point in time onwards as deleted.
type SoftDelete struct {
	common
	PointInTime datetime.LocalDateTime `json:"pointInTime" required:"true"`
}

// Undelete reverses a SoftDelete, provided nothing has been written since.
type Undelete struct {
	common
	PointInTime datetime.LocalDateTime `json:"pointInTime" required:"true"`
}

type UpdateAssessmentAnswers struct {
	common
	autosave
	Added   model.Answers `json:"added" required:"true"`
	Removed []string      `json:"removed" required:"true"`
}

type UpdateAssessmentProperties struct {
	common
	autosave
	Added   model.Answers `json:"added" required:"true"`
	Removed []string      `json:"removed" required:"true"`
}

type UpdateCollectionItemAnswers struct {
	common
	autosave
	CollectionItemUUID Reference     `json:"collectionItemUuid" required:"true"`
	Added              model.Answers `json:"added" required:"true"`
	Removed            []string      `json:"removed" required:"true"`
}

type UpdateCollectionItemProperties struct {
	common
	autosave
	CollectionItemUUID Reference     `json:"collectionItemUuid" required:"true"`
	Added              model.Answers `json:"added" required:"true"`
	Removed            []string      `json:"removed" required:"true"`
}

type UpdateFlags struct {
	common
	autosave
	Flags []string `json:"flags" required:"true"`
}

type UpdateFormVersion struct {
	common
	Version string `json:"version" required:"true"`
}

func (*AddCollectionItem) Type() string              { return "AddCollectionItemCommand" }
func (*CreateAssessment) Type() string               { return "CreateAssessmentCommand" }
func (*CreateCollection) Type() string               { return "CreateCollectionCommand" }
func (*CreateTimelineItem) Type() string             { return "CreateTimelineItemCommand" }
func (*RemoveCollectionItem) Type() string           { return "RemoveCollectionItemCommand" }
func (*ReorderCollectionItem) Type() string          { return "ReorderCollectionItemCommand" }
func (*Rollback) Type() string                       { return "RollbackCommand" }
func (*SoftDelete) Type() string                     { return "SoftDeleteCommand" }
func (*Undelete) Type() string                       { return "UndeleteCommand" }
func (*UpdateAssessmentAnswers) Type() string        { return "UpdateAssessmentAnswersCommand" }
func (*UpdateAssessmentProperties) Type() string     { return "UpdateAssessmentPropertiesCommand" }
func (*UpdateCollectionItemAnswers) Type() string    { return "UpdateCollectionItemAnswersCommand" }
func (*UpdateCollectionItemProperties) Type() string { return "UpdateCollectionItemPropertiesCommand" }
func (*UpdateFlags) Type() string                    { return "UpdateFlagsCommand" }
func (*UpdateFormVersion) Type() string              { return "UpdateFormVersionCommand" }

func (c *AddCollectionItem) Autosaved() bool              { return c.Autosave }
func (c *CreateCollection) Autosaved() bool               { return c.Autosave }
func (c *ReorderCollectionItem) Autosaved() bool          { return c.Autosave }
func (c *UpdateAssessmentAnswers) Autosaved() bool        { return c.Autosave }
func (c *UpdateAssessmentProperties) Autosaved() bool     { return c.Autosave }
func (c *UpdateCollectionItemAnswers) Autosaved() bool    { return c.Autosave }
func (c *UpdateCollectionItemProperties) Autosaved() bool { return c.Autosave }
func (c *UpdateFlags) Autosaved() bool                    { return c.Autosave }

func (c *CreateAssessment) Requester() model.UserDetails    { return c.User }
func (c *CreateAssessment) AssessmentUUID() uuid.UUID       { return c.GeneratedAssessment }
func (c *CreateAssessment) CustomTimeline() *model.Timeline { return c.Timeline }
func (c *CreateAssessment) CommandHooks() []Hook            { return c.Hooks }
func (c *CreateAssessment) Autosaved() bool                 { return false }
func (c *CreateAssessment) references() []*Reference        { return nil }

func (c *CreateTimelineItem) Requester() model.UserDetails    { return c.User }
func (c *CreateTimelineItem) AssessmentUUID() uuid.UUID       { return c.Assessment.UUID() }
func (c *CreateTimelineItem) CustomTimeline() *model.Timeline { return &c.Timeline }
func (c *CreateTimelineItem) CommandHooks() []Hook            { return c.Hooks }
func (c *CreateTimelineItem) Autosaved() bool                 { return false }
func (c *CreateTimelineItem) references() []*Reference        { return []*Reference{&c.Assessment} }

func (c *AddCollectionItem) references() []*Reference {
	return []*Reference{&c.CollectionUUID, &c.Assessment}
}

func (c *CreateCollection) references() []*Reference {
	if c.ParentCollectionItemUUID == nil {
		return []*Reference{&c.Assessment}
	}
	return []*Reference{&c.Assessment, c.ParentCollectionItemUUID}
}

func (c *RemoveCollectionItem) references() []*Reference {
	return []*Reference{&c.CollectionItemUUID, &c.Assessment}
}

func (c *ReorderCollectionItem) references() []*Reference {
	return []*Reference{&c.CollectionItemUUID, &c.Assessment}
}

func (c *UpdateCollectionItemAnswers) references() []*Reference {
	return []*Reference{&c.CollectionItemUUID, &c.Assessment}
}

func (c *UpdateCollectionItemProperties) references() []*Reference {
	return []*Reference{&c.CollectionItemUUID, &c.Assessment}
}

func orEmpty(values []string) []string {
	if values == nil {
		return []string{}
	}
	return values
}

func (c AddCollectionItem) MarshalJSON() ([]byte, error) {
	type plain AddCollectionItem
	return jsonx.Tag(c.Type(), plain(c))
}

func (c CreateAssessment) MarshalJSON() ([]byte, error) {
	type plain CreateAssessment
	c.Flags = orEmpty(c.Flags)
	return jsonx.Tag(c.Type(), plain(c))
}

func (c CreateCollection) MarshalJSON() ([]byte, error) {
	type plain CreateCollection
	return jsonx.Tag(c.Type(), plain(c))
}

func (c CreateTimelineItem) MarshalJSON() ([]byte, error) {
	type plain CreateTimelineItem
	return jsonx.Tag(c.Type(), plain(c))
}

func (c RemoveCollectionItem) MarshalJSON() ([]byte, error) {
	type plain RemoveCollectionItem
	return jsonx.Tag(c.Type(), plain(c))
}

func (c ReorderCollectionItem) MarshalJSON() ([]byte, error) {
	type plain ReorderCollectionItem
	return jsonx.Tag(c.Type(), plain(c))
}

func (c Rollback) MarshalJSON() ([]byte, error) {
	type plain Rollback
	return jsonx.Tag(c.Type(), plain(c))
}

func (c SoftDelete) MarshalJSON() ([]byte, error) {
	type plain SoftDelete
	return jsonx.Tag(c.Type(), plain(c))
}

func (c Undelete) MarshalJSON() ([]byte, error) {
	type plain Undelete
	return jsonx.Tag(c.Type(), plain(c))
}

func (c UpdateAssessmentAnswers) MarshalJSON() ([]byte, error) {
	type plain UpdateAssessmentAnswers
	return jsonx.Tag(c.Type(), plain(c))
}

func (c UpdateAssessmentProperties) MarshalJSON() ([]byte, error) {
	type plain UpdateAssessmentProperties
	return jsonx.Tag(c.Type(), plain(c))
}

func (c UpdateCollectionItemAnswers) MarshalJSON() ([]byte, error) {
	type plain UpdateCollectionItemAnswers
	return jsonx.Tag(c.Type(), plain(c))
}

func (c UpdateCollectionItemProperties) MarshalJSON() ([]byte, error) {
	type plain UpdateCollectionItemProperties
	return jsonx.Tag(c.Type(), plain(c))
}

func (c UpdateFlags) MarshalJSON() ([]byte, error) {
	type plain UpdateFlags
	return jsonx.Tag(c.Type(), plain(c))
}

func (c UpdateFormVersion) MarshalJSON() ([]byte, error) {
	type plain UpdateFormVersion
	return jsonx.Tag(c.Type(), plain(c))
}

// List decodes commands by their "type".
type List []Command

func (l *List) UnmarshalJSON(data []byte) error {
	var raw []json.RawMessage
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	commands := make(List, 0, len(raw))
	for i, item := range raw {
		decoded, err := Unmarshal(item)
		if err != nil {
			return fmt.Errorf("commands[%d]: %w", i, err)
		}
		commands = append(commands, decoded)
	}
	*l = commands
	return nil
}

// Unmarshal decodes a command by its "type", generating the UUIDs for anything it creates.
func Unmarshal(data []byte) (Command, error) {
	typeName, err := jsonx.TypeOf(data)
	if err != nil {
		return nil, err
	}
	var decoded Command
	switch typeName {
	case "AddCollectionItemCommand":
		decoded = &AddCollectionItem{CollectionItemUUID: uuid.New()}
	case "CreateAssessmentCommand":
		decoded = &CreateAssessment{GeneratedAssessment: uuid.New()}
	case "CreateCollectionCommand":
		decoded = &CreateCollection{CollectionUUID: uuid.New()}
	case "CreateTimelineItemCommand":
		decoded = &CreateTimelineItem{}
	case "RemoveCollectionItemCommand":
		decoded = &RemoveCollectionItem{}
	case "ReorderCollectionItemCommand":
		decoded = &ReorderCollectionItem{}
	case "RollbackCommand":
		decoded = &Rollback{}
	case "SoftDeleteCommand":
		decoded = &SoftDelete{}
	case "UndeleteCommand":
		decoded = &Undelete{}
	case "UpdateAssessmentAnswersCommand":
		decoded = &UpdateAssessmentAnswers{}
	case "UpdateAssessmentPropertiesCommand":
		decoded = &UpdateAssessmentProperties{}
	case "UpdateCollectionItemAnswersCommand":
		decoded = &UpdateCollectionItemAnswers{}
	case "UpdateCollectionItemPropertiesCommand":
		decoded = &UpdateCollectionItemProperties{}
	case "UpdateFlagsCommand":
		decoded = &UpdateFlags{}
	case "UpdateFormVersionCommand":
		decoded = &UpdateFormVersion{}
	default:
		return nil, fmt.Errorf("could not resolve type id '%s' as a command", typeName)
	}
	if err := jsonx.Unmarshal(data, decoded); err != nil {
		return nil, err
	}
	if create, ok := decoded.(*CreateAssessment); ok && create.Identifiers != nil {
		for identifierType := range create.Identifiers.All() {
			if _, err := model.ParseIdentifierType(identifierType); err != nil {
				return nil, err
			}
		}
	}
	return decoded, nil
}

// Reference is a UUID in a command, given either directly or as @index to use the UUID created
// by an earlier command in the same request. It's written back exactly as it was given.
type Reference struct {
	raw   string
	value *uuid.UUID
}

// ReferenceTo is an already resolved reference.
func ReferenceTo(id uuid.UUID) Reference { return Reference{raw: id.String(), value: &id} }

// UUID is the resolved UUID. Commands are always resolved before they're handled.
func (r Reference) UUID() uuid.UUID {
	if r.value == nil {
		panic(fmt.Sprintf("reference '%s' used before it was resolved", r.raw))
	}
	return *r.value
}

func (r *Reference) resolve(earlier []Response) error {
	if index, isPlaceholder := strings.CutPrefix(r.raw, "@"); isPlaceholder {
		position, err := strconv.Atoi(index)
		if err != nil || position < 0 {
			return apperr.InvalidReference(r.raw)
		}
		if position >= len(earlier) {
			return apperr.PlaceholderNotFound(position)
		}
		created := earlier[position].Result.createdUUID()
		if created == nil {
			return apperr.PlaceholderNotFound(position)
		}
		r.value = created
		return nil
	}
	parsed, err := uuid.Parse(r.raw)
	if err != nil {
		return apperr.InvalidReference(r.raw)
	}
	r.value = &parsed
	return nil
}

func (r Reference) MarshalJSON() ([]byte, error) { return json.Marshal(r.raw) }

func (r *Reference) UnmarshalJSON(data []byte) error {
	var raw string
	if err := json.Unmarshal(data, &raw); err != nil {
		return fmt.Errorf("expected a UUID or placeholder string")
	}
	*r = Reference{raw: raw}
	return nil
}

// resolveReferences resolves UUIDs and @index placeholders against the earlier commands' results.
func resolveReferences(command Command, earlier []Response) error {
	for _, reference := range command.references() {
		if err := reference.resolve(earlier); err != nil {
			return err
		}
	}
	return nil
}
