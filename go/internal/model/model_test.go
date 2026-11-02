package model

import (
	"encoding/json"
	"slices"
	"testing"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
)

func TestValuesUseTheKotlinTypeNames(t *testing.T) {
	out, _ := json.Marshal(Single("YES"))
	if string(out) != `{"type":"Single","value":"YES"}` {
		t.Errorf("got %s", out)
	}
	var multi Value
	if err := json.Unmarshal([]byte(`{"type":"Multi","values":["A","B"]}`), &multi); err != nil {
		t.Fatal(err)
	}
	if values, ok := multi.MultiValues(); !ok || !slices.Equal(values, []string{"A", "B"}) {
		t.Errorf("got %v", values)
	}
	for _, invalid := range []string{`{"value":"x"}`, `{"type":"Single"}`, `{"type":"Other","value":"x"}`} {
		if json.Unmarshal([]byte(invalid), &multi) == nil {
			t.Errorf("expected %s to be rejected", invalid)
		}
	}
}

func TestUserDetailsDefaultTheAuthSourceAndRequireTheRest(t *testing.T) {
	var user UserDetails
	if err := json.Unmarshal([]byte(`{"id":"u","name":"User"}`), &user); err != nil || user.AuthSource != AuthSourceNotSpecified {
		t.Errorf("got %+v, %v", user, err)
	}
	if json.Unmarshal([]byte(`{"id":"u"}`), &user) == nil {
		t.Error("expected a missing name to be rejected")
	}
}

func item(id uuid.UUID, collections ...*Collection) *CollectionItem {
	return &CollectionItem{UUID: id, CreatedAt: datetime.Now(), UpdatedAt: datetime.Now(), Collections: collections}
}

func collection(id uuid.UUID, name string, items ...*CollectionItem) *Collection {
	return &Collection{UUID: id, CreatedAt: datetime.Now(), UpdatedAt: datetime.Now(), Name: name, Items: items}
}

func ids(c *Collection) []uuid.UUID {
	var out []uuid.UUID
	for _, item := range c.Items {
		out = append(out, item.UUID)
	}
	return out
}

func TestFindsAndRemovesNestedItems(t *testing.T) {
	nestedItem, nestedCollection, outer := uuid.New(), uuid.New(), uuid.New()
	goals := collection(uuid.New(), "GOALS", item(outer, collection(nestedCollection, "NOTES", item(nestedItem))))

	if goals.FindItem(nestedItem) == nil || goals.IndexOf(nestedItem) != -1 || goals.IndexOf(outer) != 0 {
		t.Fatal("expected nested items to be found but not indexed")
	}
	if goals.Items[0].FindCollectionWithItem(nestedItem).UUID != nestedCollection {
		t.Error("expected the containing collection")
	}
	if !goals.RemoveItem(nestedItem) || goals.FindItem(nestedItem) != nil || goals.RemoveItem(nestedItem) {
		t.Error("expected the nested item to be removed once")
	}
}

func TestReordersItemsClampingTheTargetIndex(t *testing.T) {
	a, b, c := uuid.New(), uuid.New(), uuid.New()
	list := collection(uuid.New(), "LIST", item(a), item(b), item(c))

	list.ReorderItem(a, 1)
	if !slices.Equal(ids(list), []uuid.UUID{b, a, c}) {
		t.Errorf("got %v", ids(list))
	}
	list.ReorderItem(c, -5)
	if !slices.Equal(ids(list), []uuid.UUID{c, b, a}) {
		t.Errorf("got %v", ids(list))
	}
	list.ReorderItem(c, 99)
	if !slices.Equal(ids(list), []uuid.UUID{b, a, c}) {
		t.Errorf("got %v", ids(list))
	}
}

func TestClonesAreIndependent(t *testing.T) {
	original := collection(uuid.New(), "GOALS", item(uuid.New()))
	original.Items[0].Answers.Set("q", Single("a"))

	clone := original.Clone()
	clone.Items[0].Answers.Set("q", Single("changed"))
	clone.Items = nil

	value, _ := original.Items[0].Answers.Get("q")
	if answer, _ := value.SingleValue(); answer != "a" || len(original.Items) != 1 {
		t.Error("expected the original to be unchanged")
	}
}
