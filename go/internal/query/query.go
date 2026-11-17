// Package query handles reads of assessment data. Most queries accept a timestamp to read an
// assessment as it was at that time.
package query

import (
	"encoding/json"
	"fmt"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
)

// AssessmentIdentifier identifies an assessment by UUID ("UUID") or by an identifier from an
// upstream system ("EXTERNAL").
type AssessmentIdentifier struct {
	UUID     *uuid.UUID
	External *ExternalIdentifier
}

type ExternalIdentifier struct {
	Identifier     string               `json:"identifier" required:"true"`
	IdentifierType model.IdentifierType `json:"identifierType" required:"true"`
	AssessmentType string               `json:"assessmentType" required:"true"`
}

func ByUUID(id uuid.UUID) AssessmentIdentifier { return AssessmentIdentifier{UUID: &id} }

func (i AssessmentIdentifier) MarshalJSON() ([]byte, error) {
	if i.External != nil {
		return jsonx.Tag("EXTERNAL", *i.External)
	}
	return jsonx.Tag("UUID", struct {
		UUID *uuid.UUID `json:"uuid"`
	}{i.UUID})
}

func (i *AssessmentIdentifier) UnmarshalJSON(data []byte) error {
	typeName, err := jsonx.TypeOf(data)
	if err != nil {
		return err
	}
	switch typeName {
	case "UUID":
		var byUUID struct {
			UUID uuid.UUID `json:"uuid" required:"true"`
		}
		if err := jsonx.Unmarshal(data, &byUUID); err != nil {
			return err
		}
		*i = ByUUID(byUUID.UUID)
	case "EXTERNAL":
		var external ExternalIdentifier
		if err := jsonx.Unmarshal(data, &external); err != nil {
			return err
		}
		*i = AssessmentIdentifier{External: &external}
	default:
		return fmt.Errorf("could not resolve type id '%s' as an assessment identifier", typeName)
	}
	return nil
}

// Query is a read of assessment data, identified in JSON by its "type".
type Query interface {
	Type() string
	Requester() model.UserDetails
}

type common struct {
	User      model.UserDetails       `json:"user" required:"true"`
	Timestamp *datetime.LocalDateTime `json:"timestamp"`
}

func (c *common) Requester() model.UserDetails { return c.User }

type AssessmentVersion struct {
	common
	AssessmentIdentifier AssessmentIdentifier `json:"assessmentIdentifier" required:"true"`
}

type CollectionItem struct {
	common
	AssessmentIdentifier AssessmentIdentifier `json:"assessmentIdentifier" required:"true"`
	CollectionItemUUID   uuid.UUID            `json:"collectionItemUuid" required:"true"`
	// Depth is how many levels of nested collections to include; -1 for all of them.
	Depth int `json:"depth"`
}

type Collection struct {
	common
	AssessmentIdentifier AssessmentIdentifier `json:"assessmentIdentifier" required:"true"`
	CollectionUUID       uuid.UUID            `json:"collectionUuid" required:"true"`
	// Depth is how many levels of nested collections to include; -1 for all of them.
	Depth int `json:"depth"`
}

type DailyVersions struct {
	common
	AssessmentIdentifier AssessmentIdentifier `json:"assessmentIdentifier" required:"true"`
}

type AssessmentsModifiedSince struct {
	common
	AssessmentType string                 `json:"assessmentType" required:"true"`
	Since          datetime.LocalDateTime `json:"since" required:"true"`
	// After is the nextCursor from the previous page.
	After *uuid.UUID `json:"after"`
	Limit int        `json:"limit"`
}

type AssessmentsSoftDeletedSince struct {
	common
	AssessmentType string                 `json:"assessmentType" required:"true"`
	Since          datetime.LocalDateTime `json:"since" required:"true"`
}

type Timeline struct {
	common
	PageNumber           int                   `json:"pageNumber"`
	PageSize             int                   `json:"pageSize"`
	AssessmentIdentifier *AssessmentIdentifier `json:"assessmentIdentifier"`
	// Subject limits the timeline to entries by this user.
	Subject            *model.UserDetails      `json:"subject"`
	From               *datetime.LocalDateTime `json:"from"`
	To                 *datetime.LocalDateTime `json:"to"`
	IncludeEventTypes  []string                `json:"includeEventTypes"`
	ExcludeEventTypes  []string                `json:"excludeEventTypes"`
	IncludeCustomTypes []string                `json:"includeCustomTypes"`
	ExcludeCustomTypes []string                `json:"excludeCustomTypes"`
}

func (*AssessmentVersion) Type() string           { return "AssessmentVersionQuery" }
func (*CollectionItem) Type() string              { return "CollectionItemQuery" }
func (*Collection) Type() string                  { return "CollectionQuery" }
func (*DailyVersions) Type() string               { return "DailyVersionsQuery" }
func (*AssessmentsModifiedSince) Type() string    { return "GetAssessmentsModifiedSinceQuery" }
func (*AssessmentsSoftDeletedSince) Type() string { return "GetAssessmentsSoftDeletedSinceQuery" }
func (*Timeline) Type() string                    { return "TimelineQuery" }

func (q AssessmentVersion) MarshalJSON() ([]byte, error) {
	type plain AssessmentVersion
	return jsonx.Tag(q.Type(), plain(q))
}

func (q CollectionItem) MarshalJSON() ([]byte, error) {
	type plain CollectionItem
	return jsonx.Tag(q.Type(), plain(q))
}

func (q Collection) MarshalJSON() ([]byte, error) {
	type plain Collection
	return jsonx.Tag(q.Type(), plain(q))
}

func (q DailyVersions) MarshalJSON() ([]byte, error) {
	type plain DailyVersions
	return jsonx.Tag(q.Type(), plain(q))
}

func (q AssessmentsModifiedSince) MarshalJSON() ([]byte, error) {
	type plain AssessmentsModifiedSince
	return jsonx.Tag(q.Type(), plain(q))
}

func (q AssessmentsSoftDeletedSince) MarshalJSON() ([]byte, error) {
	type plain AssessmentsSoftDeletedSince
	return jsonx.Tag(q.Type(), plain(q))
}

func (q Timeline) MarshalJSON() ([]byte, error) {
	type plain Timeline
	return jsonx.Tag(q.Type(), plain(q))
}

// List decodes queries by their "type".
type List []Query

func (l *List) UnmarshalJSON(data []byte) error {
	var raw []json.RawMessage
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	queries := make(List, 0, len(raw))
	for i, item := range raw {
		decoded, err := Unmarshal(item)
		if err != nil {
			return fmt.Errorf("queries[%d]: %w", i, err)
		}
		queries = append(queries, decoded)
	}
	*l = queries
	return nil
}

func Unmarshal(data []byte) (Query, error) {
	typeName, err := jsonx.TypeOf(data)
	if err != nil {
		return nil, err
	}
	var decoded Query
	switch typeName {
	case "AssessmentVersionQuery":
		decoded = &AssessmentVersion{}
	case "CollectionItemQuery":
		decoded = &CollectionItem{}
	case "CollectionQuery":
		decoded = &Collection{}
	case "DailyVersionsQuery":
		decoded = &DailyVersions{}
	case "GetAssessmentsModifiedSinceQuery":
		decoded = &AssessmentsModifiedSince{Limit: 50}
	case "GetAssessmentsSoftDeletedSinceQuery":
		decoded = &AssessmentsSoftDeletedSince{}
	case "TimelineQuery":
		decoded = &Timeline{PageSize: 50}
	default:
		return nil, fmt.Errorf("could not resolve type id '%s' as a query", typeName)
	}
	return decoded, jsonx.Unmarshal(data, decoded)
}

// SubjectAccessRequest is everything held about a person. It isn't available through /query.
type SubjectAccessRequest struct {
	Timestamp             *datetime.LocalDateTime
	AssessmentIdentifiers []model.IdentifierPair
	From, To              *datetime.Date
}
