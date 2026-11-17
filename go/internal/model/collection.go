package model

import (
	"encoding/json"
	"slices"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
)

type Collection struct {
	UUID      uuid.UUID              `json:"uuid"`
	CreatedAt datetime.LocalDateTime `json:"createdAt"`
	UpdatedAt datetime.LocalDateTime `json:"updatedAt"`
	Name      string                 `json:"name"`
	Items     []*CollectionItem      `json:"items"`
}

type CollectionItem struct {
	UUID        uuid.UUID              `json:"uuid"`
	CreatedAt   datetime.LocalDateTime `json:"createdAt"`
	UpdatedAt   datetime.LocalDateTime `json:"updatedAt"`
	Answers     Answers                `json:"answers"`
	Properties  Answers                `json:"properties"`
	Collections []*Collection          `json:"collections"`
}

// MarshalJSON writes an empty list rather than null for no items.
func (c Collection) MarshalJSON() ([]byte, error) {
	type plain Collection
	if c.Items == nil {
		c.Items = []*CollectionItem{}
	}
	return json.Marshal(plain(c))
}

// MarshalJSON writes an empty list rather than null for no collections.
func (i CollectionItem) MarshalJSON() ([]byte, error) {
	type plain CollectionItem
	if i.Collections == nil {
		i.Collections = []*Collection{}
	}
	return json.Marshal(plain(i))
}

func (c *Collection) Clone() *Collection {
	clone := *c
	clone.Items = make([]*CollectionItem, len(c.Items))
	for i, item := range c.Items {
		clone.Items[i] = item.Clone()
	}
	return &clone
}

func (i *CollectionItem) Clone() *CollectionItem {
	clone := *i
	clone.Answers = i.Answers.Clone()
	clone.Properties = i.Properties.Clone()
	clone.Collections = make([]*Collection, len(i.Collections))
	for index, collection := range i.Collections {
		clone.Collections[index] = collection.Clone()
	}
	return &clone
}

// FindItem finds an item in this collection or any collection nested beneath it.
func (c *Collection) FindItem(id uuid.UUID) *CollectionItem {
	for _, item := range c.Items {
		if item.UUID == id {
			return item
		}
	}
	for _, item := range c.Items {
		for _, nested := range item.Collections {
			if found := nested.FindItem(id); found != nil {
				return found
			}
		}
	}
	return nil
}

// IndexOf is the index of a direct item, or -1, like List.indexOf.
func (c *Collection) IndexOf(id uuid.UUID) int {
	return slices.IndexFunc(c.Items, func(item *CollectionItem) bool { return item.UUID == id })
}

func (c *Collection) RemoveItem(id uuid.UUID) bool {
	before := len(c.Items)
	c.Items = slices.DeleteFunc(c.Items, func(item *CollectionItem) bool { return item.UUID == id })
	if len(c.Items) != before {
		return true
	}
	for _, item := range c.Items {
		for _, nested := range item.Collections {
			if nested.RemoveItem(id) {
				return true
			}
		}
	}
	return false
}

// ReorderItem moves an item to targetIndex, clamped to the bounds of its collection.
func (c *Collection) ReorderItem(id uuid.UUID, targetIndex int) bool {
	if index := c.IndexOf(id); index != -1 {
		target := min(max(targetIndex, 0), len(c.Items)-1)
		item := c.Items[index]
		c.Items = slices.Delete(c.Items, index, index+1)
		c.Items = slices.Insert(c.Items, target, item)
		return true
	}
	for _, item := range c.Items {
		for _, nested := range item.Collections {
			if nested.ReorderItem(id, targetIndex) {
				return true
			}
		}
	}
	return false
}

func (i *CollectionItem) FindCollection(id uuid.UUID) *Collection {
	for _, collection := range i.Collections {
		if collection.UUID == id {
			return collection
		}
	}
	for _, collection := range i.Collections {
		for _, item := range collection.Items {
			if found := item.FindCollection(id); found != nil {
				return found
			}
		}
	}
	return nil
}

// FindCollectionWithItem finds the collection, at any depth beneath this item, that directly
// contains the given item.
func (i *CollectionItem) FindCollectionWithItem(itemID uuid.UUID) *Collection {
	for _, collection := range i.Collections {
		if collection.IndexOf(itemID) != -1 {
			return collection
		}
		for _, item := range collection.Items {
			if found := item.FindCollectionWithItem(itemID); found != nil {
				return found
			}
		}
	}
	return nil
}
