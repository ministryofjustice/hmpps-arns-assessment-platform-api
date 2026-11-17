// Package omap provides a string-keyed map that keeps insertion order, including in JSON.
//
// The Kotlin service used LinkedHashMaps for answers and properties, so their order shows up in
// stored JSON and in timeline entries. Go maps would sort the keys instead.
package omap

import (
	"bytes"
	"encoding/json"
	"fmt"
	"iter"
	"slices"
)

// Map is usable as its zero value, which marshals as an empty object.
type Map[V any] struct {
	keys   []string
	values map[string]V
}

// Of builds a map from alternating keys and values.
func Of[V any](pairs ...any) Map[V] {
	var m Map[V]
	for i := 0; i+1 < len(pairs); i += 2 {
		m.Set(pairs[i].(string), pairs[i+1].(V))
	}
	return m
}

func (m *Map[V]) Len() int { return len(m.keys) }

// IsZero reports whether the map was never set, as opposed to being empty.
func (m *Map[V]) IsZero() bool { return m.values == nil }

func (m *Map[V]) Get(key string) (V, bool) {
	value, ok := m.values[key]
	return value, ok
}

// Set adds or replaces a value. A replaced value keeps its position.
func (m *Map[V]) Set(key string, value V) {
	if m.values == nil {
		m.values = map[string]V{}
	}
	if _, exists := m.values[key]; !exists {
		m.keys = append(m.keys, key)
	}
	m.values[key] = value
}

// SetAll sets every entry of other, in its order.
func (m *Map[V]) SetAll(other Map[V]) {
	for key, value := range other.All() {
		m.Set(key, value)
	}
}

// Delete removes a key, keeping the order of the rest. Reports whether it was present.
func (m *Map[V]) Delete(key string) bool {
	if _, exists := m.values[key]; !exists {
		return false
	}
	delete(m.values, key)
	m.keys = slices.DeleteFunc(m.keys, func(k string) bool { return k == key })
	return true
}

func (m *Map[V]) Keys() []string { return slices.Clone(m.keys) }

func (m *Map[V]) All() iter.Seq2[string, V] {
	return func(yield func(string, V) bool) {
		for _, key := range m.keys {
			if !yield(key, m.values[key]) {
				return
			}
		}
	}
}

// Clone is shallow: the values themselves are copied by assignment.
func (m Map[V]) Clone() Map[V] {
	clone := Map[V]{keys: slices.Clone(m.keys)}
	if m.values != nil {
		clone.values = make(map[string]V, len(m.values))
		for key, value := range m.values {
			clone.values[key] = value
		}
	}
	return clone
}

func (m Map[V]) MarshalJSON() ([]byte, error) {
	var buf bytes.Buffer
	buf.WriteByte('{')
	for i, key := range m.keys {
		if i > 0 {
			buf.WriteByte(',')
		}
		encodedKey, err := json.Marshal(key)
		if err != nil {
			return nil, err
		}
		encodedValue, err := json.Marshal(m.values[key])
		if err != nil {
			return nil, err
		}
		buf.Write(encodedKey)
		buf.WriteByte(':')
		buf.Write(encodedValue)
	}
	buf.WriteByte('}')
	return buf.Bytes(), nil
}

func (m *Map[V]) UnmarshalJSON(data []byte) error {
	*m = Map[V]{}
	if bytes.Equal(bytes.TrimSpace(data), []byte("null")) {
		return nil
	}
	decoder := json.NewDecoder(bytes.NewReader(data))
	if token, err := decoder.Token(); err != nil || token != json.Delim('{') {
		return fmt.Errorf("expected a JSON object")
	}
	m.values = map[string]V{}
	for decoder.More() {
		token, err := decoder.Token()
		if err != nil {
			return err
		}
		key := token.(string)
		var value V
		if err := decoder.Decode(&value); err != nil {
			return fmt.Errorf("%s: %w", key, err)
		}
		m.Set(key, value)
	}
	_, err := decoder.Token()
	return err
}
