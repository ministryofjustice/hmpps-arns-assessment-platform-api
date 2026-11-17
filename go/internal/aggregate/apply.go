package aggregate

import (
	"fmt"
	"slices"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/event"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
)

// Apply applies any event that doesn't need to read other state (that is, anything but a
// rollback), returning the timeline entry it produces.
func Apply(e *entity.Event, state *State, c clock.Clock) (*entity.TimelineDraft, error) {
	aggregate := state.ForWrite(c)
	data := aggregate.Data
	user := e.User.UUID

	var timeline jsonx.Object
	switch ev := e.Data.(type) {
	case event.AssessmentCreated:
		data.Properties.SetAll(ev.Properties)
		data.FormVersion = ev.FormVersion
		data.AddCollaborator(user)
		data.Flags = append(data.Flags, ev.Flags...)
		timeline = jsonx.ObjectOf("formVersion", ev.FormVersion, "properties", keys(ev.Properties))

	case event.AssessmentAnswersUpdated:
		data.Answers.SetAll(ev.Added)
		for _, fieldCode := range ev.Removed {
			if !data.Answers.Delete(fieldCode) {
				return nil, apperr.AnswerNotFound(fieldCode, aggregate.UUID)
			}
		}
		data.AddCollaborator(user)
		timeline = jsonx.ObjectOf("added", keys(ev.Added), "removed", orEmpty(ev.Removed))

	case event.AssessmentPropertiesUpdated:
		data.Properties.SetAll(ev.Added)
		for _, propertyName := range ev.Removed {
			if !data.Properties.Delete(propertyName) {
				return nil, apperr.PropertyNotFound(propertyName, aggregate.UUID)
			}
		}
		data.AddCollaborator(user)
		timeline = jsonx.ObjectOf("added", keys(ev.Added), "removed", orEmpty(ev.Removed))

	case event.AssessmentFlagsUpdated:
		data.Flags = slices.Clone(ev.Flags)
		data.AddCollaborator(user)
		timeline = jsonx.ObjectOf("flags", orEmpty(ev.Flags))

	case event.AssignedToUser:
		assigned := ev.UserUUID
		data.AssignedUser = &assigned
		timeline = jsonx.ObjectOf("assignee", e.User)

	case event.FormVersionUpdated:
		data.FormVersion = ev.Version
		data.AddCollaborator(user)
		timeline = jsonx.ObjectOf("version", ev.Version)

	case event.CollectionCreated:
		collection := &model.Collection{UUID: ev.CollectionUUID, CreatedAt: e.CreatedAt, UpdatedAt: e.CreatedAt, Name: ev.Name}
		if parent := ev.ParentCollectionItemUUID; parent != nil {
			item := data.GetCollectionItem(*parent)
			if item == nil {
				return nil, apperr.CollectionItemNotFound(*parent, aggregate.UUID)
			}
			item.Collections = append(item.Collections, collection)
		} else {
			data.Collections = append(data.Collections, collection)
		}
		data.AddCollaborator(user)
		timeline = jsonx.ObjectOf("collection", ev.Name, "collectionUuid", ev.CollectionUUID)

	case event.CollectionItemAdded:
		item := &model.CollectionItem{
			UUID: ev.CollectionItemUUID, CreatedAt: e.CreatedAt, UpdatedAt: e.CreatedAt,
			Answers: ev.Answers.Clone(), Properties: ev.Properties.Clone(),
		}
		collection := data.GetCollection(ev.CollectionUUID)
		if collection == nil {
			return nil, apperr.CollectionNotFound(ev.CollectionUUID, aggregate.UUID)
		}
		switch {
		case ev.Index != nil && *ev.Index < 0:
			return nil, apperr.CollectionIndexOutOfBounds(*ev.Index, len(collection.Items))
		case ev.Index != nil && *ev.Index <= len(collection.Items):
			collection.Items = slices.Insert(collection.Items, *ev.Index, item)
		default:
			collection.Items = append(collection.Items, item)
		}
		index := len(collection.Items) - 1
		if ev.Index != nil {
			index = *ev.Index
		}
		timeline = jsonx.ObjectOf(
			"index", index, "collection", collection.Name,
			"collectionItemUuid", ev.CollectionItemUUID, "collectionUuid", ev.CollectionUUID,
		)
		data.AddCollaborator(user)

	case event.CollectionItemAnswersUpdated:
		collection, item, err := containingCollection(data, ev.CollectionItemUUID, aggregate)
		if err != nil {
			return nil, err
		}
		item.UpdatedAt = e.CreatedAt
		item.Answers.SetAll(ev.Added)
		for _, fieldCode := range ev.Removed {
			item.Answers.Delete(fieldCode)
		}
		data.AddCollaborator(user)
		timeline = jsonx.ObjectOf(
			"collection", collection.Name, "index", collection.IndexOf(ev.CollectionItemUUID),
			"added", keys(ev.Added), "removed", orEmpty(ev.Removed),
		)

	case event.CollectionItemPropertiesUpdated:
		collection, item, err := containingCollection(data, ev.CollectionItemUUID, aggregate)
		if err != nil {
			return nil, err
		}
		item.UpdatedAt = e.CreatedAt
		item.Properties.SetAll(ev.Added)
		for _, propertyName := range ev.Removed {
			item.Properties.Delete(propertyName)
		}
		data.AddCollaborator(user)
		timeline = jsonx.ObjectOf(
			"collection", collection.Name, "index", collection.IndexOf(ev.CollectionItemUUID),
			"added", keys(ev.Added), "removed", orEmpty(ev.Removed),
		)

	case event.CollectionItemRemoved:
		collection := data.GetCollectionWithItem(ev.CollectionItemUUID)
		if collection == nil {
			return nil, apperr.CollectionItemNotFound(ev.CollectionItemUUID, aggregate.UUID)
		}
		timeline = jsonx.ObjectOf("collection", collection.Name, "index", collection.IndexOf(ev.CollectionItemUUID))
		collection.RemoveItem(ev.CollectionItemUUID)
		data.AddCollaborator(user)

	case event.CollectionItemReordered:
		collection := data.GetCollectionWithItem(ev.CollectionItemUUID)
		if collection == nil {
			return nil, apperr.CollectionItemNotFound(ev.CollectionItemUUID, aggregate.UUID)
		}
		timeline = jsonx.ObjectOf(
			"collection", collection.Name, "collectionItemUuid", ev.CollectionItemUUID,
			"index", ev.Index, "previousIndex", collection.IndexOf(ev.CollectionItemUUID),
		)
		collection.ReorderItem(ev.CollectionItemUUID, ev.Index)
		data.AddCollaborator(user)

	default:
		return nil, fmt.Errorf("%s cannot be applied without reading other state", e.Data.Type())
	}

	aggregate.Applied(e.CreatedAt, c)
	return entity.NewTimelineDraft(e, timeline), nil
}

func containingCollection(data *Assessment, itemUUID uuid.UUID, aggregate *Entity) (*model.Collection, *model.CollectionItem, error) {
	collection := data.GetCollectionWithItem(itemUUID)
	if collection == nil {
		return nil, nil, apperr.CollectionItemNotFound(itemUUID, aggregate.UUID)
	}
	return collection, collection.FindItem(itemUUID), nil
}

func keys(answers model.Answers) []string {
	return orEmpty(answers.Keys())
}

func orEmpty(values []string) []string {
	if values == nil {
		return []string{}
	}
	return values
}
