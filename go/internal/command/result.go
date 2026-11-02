package command

import (
	"fmt"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
)

// Result is what handling a command produced, identified in JSON by its "type".
type Result interface {
	// createdUUID is what an @index placeholder for the command refers to, if anything.
	createdUUID() *uuid.UUID
}

type Success struct {
	Message string `json:"message"`
	Success bool   `json:"success"`
}

type CreatedAssessment struct {
	AssessmentUUID uuid.UUID `json:"assessmentUuid"`
	Message        string    `json:"message"`
	Success        bool      `json:"success"`
}

type CreatedCollection struct {
	CollectionUUID uuid.UUID `json:"collectionUuid"`
	Message        string    `json:"message"`
	Success        bool      `json:"success"`
}

type AddedCollectionItem struct {
	CollectionItemUUID uuid.UUID `json:"collectionItemUuid"`
	Message            string    `json:"message"`
	Success            bool      `json:"success"`
}

func success() Result { return Success{"Done", true} }

func createdAssessment(id uuid.UUID) Result {
	return CreatedAssessment{id, fmt.Sprintf("Assessment created successfully with UUID %s", id), true}
}

func createdCollection(id uuid.UUID) Result {
	return CreatedCollection{id, fmt.Sprintf("Collection created successfully with UUID %s", id), true}
}

func addedCollectionItem(id uuid.UUID) Result {
	return AddedCollectionItem{id, fmt.Sprintf("Collection item added successfully with UUID %s", id), true}
}

func (Success) createdUUID() *uuid.UUID               { return nil }
func (r CreatedAssessment) createdUUID() *uuid.UUID   { return &r.AssessmentUUID }
func (r CreatedCollection) createdUUID() *uuid.UUID   { return &r.CollectionUUID }
func (r AddedCollectionItem) createdUUID() *uuid.UUID { return &r.CollectionItemUUID }

func (r Success) MarshalJSON() ([]byte, error) {
	type plain Success
	return jsonx.Tag("CommandSuccessCommandResult", plain(r))
}

func (r CreatedAssessment) MarshalJSON() ([]byte, error) {
	type plain CreatedAssessment
	return jsonx.Tag("CreateAssessmentCommandResult", plain(r))
}

func (r CreatedCollection) MarshalJSON() ([]byte, error) {
	type plain CreatedCollection
	return jsonx.Tag("CreateCollectionCommandResult", plain(r))
}

func (r AddedCollectionItem) MarshalJSON() ([]byte, error) {
	type plain AddedCollectionItem
	return jsonx.Tag("AddCollectionItemCommandResult", plain(r))
}

type Response struct {
	Request Command `json:"request"`
	Result  Result  `json:"result"`
}

type Responses struct {
	Commands []Response `json:"commands"`
}
