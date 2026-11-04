// Package formconfig has the parts of a SAN form config the OASys mapping needs. Anything else in
// a config is ignored.
package formconfig

import (
	"encoding/json"
	"fmt"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/omap"
)

type FormConfig struct {
	Version string                `json:"version" required:"true"`
	Fields  omap.Map[FieldConfig] `json:"fields"`
}

type FieldConfig struct {
	Code       string    `json:"code" required:"true"`
	Options    []Option  `json:"options"`
	Type       FieldType `json:"type"`
	Section    string    `json:"section"`
	Collection string    `json:"collection"`
}

func (f FieldConfig) MarshalJSON() ([]byte, error) {
	type plain FieldConfig
	if f.Options == nil {
		f.Options = []Option{}
	}
	return json.Marshal(plain(f))
}

func (f *FieldConfig) UnmarshalJSON(data []byte) error {
	type plain FieldConfig
	decoded := plain{Type: FieldTypeText}
	if err := json.Unmarshal(data, &decoded); err != nil {
		return err
	}
	*f = FieldConfig(decoded)
	return nil
}

// HasOption reports whether value is one of the field's options.
func (f FieldConfig) HasOption(value string) bool {
	for _, option := range f.Options {
		if option.Value != nil && *option.Value == value {
			return true
		}
	}
	return false
}

type Option struct {
	Value *string `json:"value"`
}

type FieldType string

const (
	FieldTypeRadio    FieldType = "RADIO"
	FieldTypeCheckbox FieldType = "CHECKBOX"
	FieldTypeText     FieldType = "TEXT"
	FieldTypeDate     FieldType = "DATE"
	FieldTypeSelect   FieldType = "SELECT"
)

func (t *FieldType) UnmarshalJSON(data []byte) error {
	var raw string
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	switch fieldType := FieldType(raw); fieldType {
	case FieldTypeRadio, FieldTypeCheckbox, FieldTypeText, FieldTypeDate, FieldTypeSelect:
		*t = fieldType
		return nil
	default:
		return fmt.Errorf("unknown field type '%s'", raw)
	}
}
