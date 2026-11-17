// Package jsonx has the JSON helpers the API needs beyond encoding/json: polymorphic types
// identified by a "type" property, ordered objects, and rejecting missing required fields.
package jsonx

import (
	"bytes"
	"encoding/json"
	"fmt"
	"reflect"
	"strings"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/omap"
)

// Object is an arbitrary JSON object, kept in order.
type Object = omap.Map[json.RawMessage]

// ObjectOf builds an Object from alternating keys and values, marshalling each value.
func ObjectOf(pairs ...any) Object {
	var object Object
	for i := 0; i+1 < len(pairs); i += 2 {
		encoded, err := json.Marshal(pairs[i+1])
		if err != nil {
			panic(fmt.Sprintf("unmarshallable value for %v: %v", pairs[i], err))
		}
		object.Set(pairs[i].(string), encoded)
	}
	return object
}

// Tag marshals v (which must not itself implement json.Marshaler) with a leading "type" property.
func Tag(typeName string, v any) ([]byte, error) {
	body, err := json.Marshal(v)
	if err != nil {
		return nil, err
	}
	tag, _ := json.Marshal(typeName)
	out := append([]byte(`{"type":`), tag...)
	if !bytes.Equal(body, []byte("{}")) {
		out = append(out, ',')
		out = append(out, body[1:len(body)-1]...)
	}
	return append(out, '}'), nil
}

// TypeOf reads the "type" property of a JSON object.
func TypeOf(data []byte) (string, error) {
	var envelope struct {
		Type *string `json:"type"`
	}
	if err := json.Unmarshal(data, &envelope); err != nil {
		return "", err
	}
	if envelope.Type == nil {
		return "", fmt.Errorf("missing type id property 'type'")
	}
	return *envelope.Type, nil
}

// Unmarshal decodes into v, first checking that every field tagged `required:"true"` is present
// and not null, at any depth. encoding/json alone would leave them as zero values.
func Unmarshal(data []byte, v any) error {
	if err := checkRequired(data, reflect.TypeOf(v), ""); err != nil {
		return err
	}
	return json.Unmarshal(data, v)
}

func checkRequired(data []byte, t reflect.Type, path string) error {
	for t.Kind() == reflect.Pointer {
		t = t.Elem()
	}
	if bytes.Equal(bytes.TrimSpace(data), []byte("null")) {
		return nil
	}

	switch t.Kind() {
	case reflect.Slice:
		var items []json.RawMessage
		if json.Unmarshal(data, &items) != nil {
			return nil // left for encoding/json to report
		}
		for i, item := range items {
			if err := checkRequired(item, t.Elem(), fmt.Sprintf("%s[%d]", path, i)); err != nil {
				return err
			}
		}
		return nil
	case reflect.Struct:
	default:
		return nil
	}

	var fields map[string]json.RawMessage
	if json.Unmarshal(data, &fields) != nil {
		return nil
	}
	prefix := ""
	if path != "" {
		prefix = path + "."
	}
	return checkFields(fields, t, prefix)
}

func checkFields(fields map[string]json.RawMessage, t reflect.Type, prefix string) error {
	for i := range t.NumField() {
		field := t.Field(i)
		name, _, _ := strings.Cut(field.Tag.Get("json"), ",")
		if field.Anonymous && name == "" {
			embedded := field.Type
			for embedded.Kind() == reflect.Pointer {
				embedded = embedded.Elem()
			}
			if embedded.Kind() == reflect.Struct {
				if err := checkFields(fields, embedded, prefix); err != nil {
					return err
				}
			}
			continue
		}
		if name == "-" || !field.IsExported() {
			continue
		}
		if name == "" {
			name = field.Name
		}

		value, present := fields[name]
		if !present || bytes.Equal(bytes.TrimSpace(value), []byte("null")) {
			if field.Tag.Get("required") == "true" {
				return fmt.Errorf("missing required property '%s%s'", prefix, name)
			}
			continue
		}
		if err := checkRequired(value, field.Type, prefix+name); err != nil {
			return err
		}
	}
	return nil
}
