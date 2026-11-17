// Package model has the types shared across commands, queries, events and aggregates.
package model

import (
	"encoding/json"
	"fmt"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/omap"
)

// Value is an answer or property: a single value or several.
type Value struct {
	single *string
	multi  []string
}

func Single(value string) Value    { return Value{single: &value} }
func Multi(values ...string) Value { return Value{multi: append([]string{}, values...)} }
func (v Value) IsMulti() bool      { return v.single == nil }
func (v Value) SingleValue() (string, bool) {
	if v.single == nil {
		return "", false
	}
	return *v.single, true
}
func (v Value) MultiValues() ([]string, bool) { return v.multi, v.single == nil }

func (v Value) MarshalJSON() ([]byte, error) {
	if v.single != nil {
		return json.Marshal(struct {
			Type  string `json:"type"`
			Value string `json:"value"`
		}{"Single", *v.single})
	}
	values := v.multi
	if values == nil {
		values = []string{}
	}
	return json.Marshal(struct {
		Type   string   `json:"type"`
		Values []string `json:"values"`
	}{"Multi", values})
}

func (v *Value) UnmarshalJSON(data []byte) error {
	var raw struct {
		Type   *string   `json:"type"`
		Value  *string   `json:"value"`
		Values *[]string `json:"values"`
	}
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	switch {
	case raw.Type == nil:
		return fmt.Errorf("missing type id property 'type'")
	case *raw.Type == "Single" && raw.Value != nil:
		*v = Single(*raw.Value)
	case *raw.Type == "Multi" && raw.Values != nil:
		*v = Multi(*raw.Values...)
	case *raw.Type == "Single":
		return fmt.Errorf("missing required property 'value'")
	case *raw.Type == "Multi":
		return fmt.Errorf("missing required property 'values'")
	default:
		return fmt.Errorf("unknown value type '%s'", *raw.Type)
	}
	return nil
}

// Answers are answers or properties keyed by field code, in insertion order.
type Answers = omap.Map[Value]

// AuthSource is where a user signed in.
type AuthSource string

const (
	AuthSourceOasys        AuthSource = "OASYS"
	AuthSourceHmppsAuth    AuthSource = "HMPPS_AUTH"
	AuthSourceNotSpecified AuthSource = "NOT_SPECIFIED"
)

func ParseAuthSource(value string) (AuthSource, error) {
	switch source := AuthSource(value); source {
	case AuthSourceOasys, AuthSourceHmppsAuth, AuthSourceNotSpecified:
		return source, nil
	default:
		return "", fmt.Errorf("unknown auth source '%s'", value)
	}
}

func (a *AuthSource) UnmarshalJSON(data []byte) error {
	var raw string
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	parsed, err := ParseAuthSource(raw)
	*a = parsed
	return err
}

// IdentifierType is the kind of identifier an upstream system uses for an assessment's subject.
type IdentifierType string

const (
	IdentifierCRN     IdentifierType = "CRN"
	IdentifierPRN     IdentifierType = "PRN"
	IdentifierNomisID IdentifierType = "NOMIS_ID"
)

func ParseIdentifierType(value string) (IdentifierType, error) {
	switch identifierType := IdentifierType(value); identifierType {
	case IdentifierCRN, IdentifierPRN, IdentifierNomisID:
		return identifierType, nil
	default:
		return "", fmt.Errorf("unknown identifier type '%s'", value)
	}
}

func (i *IdentifierType) UnmarshalJSON(data []byte) error {
	var raw string
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	parsed, err := ParseIdentifierType(raw)
	*i = parsed
	return err
}

// IdentifierPair is an identifier from an upstream system, such as a CRN.
type IdentifierPair struct {
	Type IdentifierType `json:"type"`
	ID   string         `json:"id"`
}

// UserDetails is the user a request is made on behalf of, as supplied by the calling service.
type UserDetails struct {
	ID         string     `json:"id" required:"true"`
	Name       string     `json:"name" required:"true"`
	AuthSource AuthSource `json:"authSource"`
}

func (u *UserDetails) UnmarshalJSON(data []byte) error {
	type plain UserDetails
	decoded := plain{AuthSource: AuthSourceNotSpecified}
	if err := jsonx.Unmarshal(data, &decoded); err != nil {
		return err
	}
	*u = UserDetails(decoded)
	return nil
}

// User is a user as returned by queries.
type User struct {
	ID         uuid.UUID   `json:"id"`
	Name       string      `json:"name"`
	UserID     *string     `json:"userId"`
	AuthSource *AuthSource `json:"authSource"`
}

// UnknownUser stands in for a user who can't be found.
func UnknownUser() User { return User{ID: uuid.Nil, Name: "UNKNOWN"} }

// Timeline is a custom timeline entry supplied alongside a command.
type Timeline struct {
	Type string       `json:"type" required:"true"`
	Data jsonx.Object `json:"data" required:"true"`
}

// TimelineItem is a timeline entry as returned by queries.
type TimelineItem struct {
	UUID       uuid.UUID              `json:"uuid" required:"true"`
	Position   int32                  `json:"position" required:"true"`
	Timestamp  datetime.LocalDateTime `json:"timestamp" required:"true"`
	User       User                   `json:"user" required:"true"`
	Assessment uuid.UUID              `json:"assessment" required:"true"`
	Event      *string                `json:"event"`
	Data       jsonx.Object           `json:"data"`
	CustomType *string                `json:"customType"`
	CustomData *jsonx.Object          `json:"customData"`
}

type DailyVersionDetails struct {
	CreatedAt            datetime.LocalDateTime `json:"createdAt"`
	UpdatedAt            datetime.LocalDateTime `json:"updatedAt"`
	LastTimelineItemUUID uuid.UUID              `json:"lastTimelineItemUuid"`
}
