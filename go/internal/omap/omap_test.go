package omap

import (
	"encoding/json"
	"testing"
)

func TestKeepsInsertionOrderThroughJSON(t *testing.T) {
	var m Map[int]
	if err := json.Unmarshal([]byte(`{"b":1,"a":2,"c":3}`), &m); err != nil {
		t.Fatal(err)
	}
	m.Set("a", 20)
	m.Set("d", 4)
	m.Delete("b")

	out, _ := json.Marshal(m)
	if string(out) != `{"a":20,"c":3,"d":4}` {
		t.Fatalf("got %s", out)
	}
}

func TestZeroValueIsUsableAndDistinctFromEmpty(t *testing.T) {
	var unset Map[string]
	if out, _ := json.Marshal(unset); string(out) != `{}` {
		t.Fatalf("got %s", out)
	}
	var empty Map[string]
	_ = json.Unmarshal([]byte(`{}`), &empty)
	if !unset.IsZero() || empty.IsZero() {
		t.Fatal("expected only the unset map to be zero")
	}
}
