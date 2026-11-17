package jsonx

import (
	"encoding/json"
	"fmt"
	"reflect"
	"testing"
)

type inner struct {
	ID   string `json:"id" required:"true"`
	Name string `json:"name"`
}

type outer struct {
	inner
	User  inner   `json:"user" required:"true"`
	Items []inner `json:"items"`
}

func TestRequiresPresentNonNullFieldsAtAnyDepth(t *testing.T) {
	cases := map[string]string{
		`{"id":"1","user":{"id":"2"}}`:                        "",
		`{"user":{"id":"2"}}`:                                 "missing required property 'id'",
		`{"id":"1","user":null}`:                              "missing required property 'user'",
		`{"id":"1","user":{"name":"x"}}`:                      "missing required property 'user.id'",
		`{"id":"1","user":{"id":"2"},"items":[{"name":"x"}]}`: "missing required property 'items[0].id'",
		`{"id":"","user":{"id":""}}`:                          "",
	}
	for in, want := range cases {
		var v outer
		err := Unmarshal([]byte(in), &v)
		got := ""
		if err != nil {
			got = err.Error()
		}
		if got != want {
			t.Errorf("%s: got %q, want %q", in, got, want)
		}
	}
}

func TestTagsObjects(t *testing.T) {
	out, _ := Tag("Thing", struct {
		A int `json:"a"`
	}{1})
	if string(out) != `{"type":"Thing","a":1}` {
		t.Errorf("got %s", out)
	}
	empty, _ := Tag("Empty", struct{}{})
	if string(empty) != `{"type":"Empty"}` {
		t.Errorf("got %s", empty)
	}
}

func TestObjectsKeepTheirOrder(t *testing.T) {
	out, _ := json.Marshal(ObjectOf("z", 1, "a", []string{"x"}))
	if string(out) != `{"z":1,"a":["x"]}` {
		t.Errorf("got %s", out)
	}
}

type embedsInterface struct {
	fmt.Stringer
	ID string `json:"id" required:"true"`
}

func TestIgnoresEmbeddedInterfaces(t *testing.T) {
	var v embedsInterface
	if err := checkRequired([]byte(`{"id":"1"}`), reflect.TypeOf(v), ""); err != nil {
		t.Errorf("got %v", err)
	}
}
