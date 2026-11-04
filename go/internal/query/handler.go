package query

import (
	"context"
	"fmt"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/aggregate"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/clock"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/engine"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/store"
)

// FindAssessment finds an assessment by UUID, or by the external identifier it had at the point
// in time.
func FindAssessment(
	ctx context.Context, db store.DB, identifier AssessmentIdentifier, pointInTime datetime.LocalDateTime,
) (*entity.Assessment, error) {
	if identifier.UUID != nil {
		return engine.FindAssessment(ctx, db, *identifier.UUID)
	}
	external := identifier.External
	found, err := store.FindAssessmentUUIDByExternalIdentifier(
		ctx, db, external.IdentifierType, external.Identifier, external.AssessmentType, pointInTime)
	if err != nil {
		return nil, err
	}
	var assessment *entity.Assessment
	if found != nil {
		if assessment, err = store.FindAssessment(ctx, db, *found); err != nil {
			return nil, err
		}
	}
	if assessment == nil {
		return nil, apperr.AssessmentNotFoundByIdentifier(string(external.IdentifierType), external.Identifier, external.AssessmentType)
	}
	return assessment, nil
}

func (d *Dispatcher) handle(ctx context.Context, db store.DB, c clock.Clock, query Query) (Result, error) {
	switch q := query.(type) {
	case *AssessmentVersion:
		assessment, err := FindAssessment(ctx, db, q.AssessmentIdentifier, c.Now())
		if err != nil {
			return nil, err
		}
		result, err := assessmentVersion(ctx, db, c, assessment, q.Timestamp)
		if err != nil {
			return nil, err
		}
		if q.Timestamp == nil {
			d.Cache.CacheLatest(result)
		}
		return result, nil

	case *Collection:
		snapshot, err := latest(ctx, db, c, q.AssessmentIdentifier, q.Timestamp)
		if err != nil {
			return nil, err
		}
		collection := snapshot.Data.GetCollection(q.CollectionUUID)
		if collection == nil {
			return nil, apperr.CollectionNotFound(q.CollectionUUID, snapshot.UUID)
		}
		switch {
		case q.Depth == -1:
			return CollectionResult{collection}, nil
		case q.Depth >= 0:
			return CollectionResult{truncateCollection(collection, q.Depth, 0)}, nil
		}
		return nil, apperr.CollectionDepthOutOfBounds(q.Depth, q.CollectionUUID)

	case *CollectionItem:
		snapshot, err := latest(ctx, db, c, q.AssessmentIdentifier, q.Timestamp)
		if err != nil {
			return nil, err
		}
		item := snapshot.Data.GetCollectionItem(q.CollectionItemUUID)
		if item == nil {
			return nil, apperr.CollectionItemNotFound(q.CollectionItemUUID, snapshot.UUID)
		}
		switch {
		case q.Depth == -1:
			return CollectionItemResult{item}, nil
		case q.Depth >= 0:
			return CollectionItemResult{truncateCollectionItem(item, q.Depth, 0)}, nil
		}
		return nil, apperr.CollectionDepthOutOfBounds(q.Depth, q.CollectionItemUUID)

	case *DailyVersions:
		assessment, err := FindAssessment(ctx, db, q.AssessmentIdentifier, c.Now())
		if err != nil {
			return nil, err
		}
		versions, err := store.FindDailyVersions(ctx, db, assessment.UUID)
		return DailyVersionsResult{versions}, err

	case *Timeline:
		return timeline(ctx, db, c, q)

	case *AssessmentsModifiedSince:
		if err := d.validateMaxLookback(c, q.Since); err != nil {
			return nil, err
		}
		if q.Limit < 1 {
			return nil, apperr.InvalidQuery("The 'limit' parameter must be at least 1")
		}
		ids, err := store.FindAssessmentUUIDsModifiedSince(ctx, db, q.AssessmentType, q.Since, q.After, q.Limit)
		if err != nil {
			return nil, err
		}
		result := AssessmentsModifiedSinceResult{Assessments: []*AssessmentVersionResult{}}
		for _, id := range ids {
			assessment, err := engine.FindAssessment(ctx, db, id)
			if err != nil {
				return nil, err
			}
			version, err := assessmentVersion(ctx, db, c, assessment, nil)
			if err != nil {
				return nil, err
			}
			result.Assessments = append(result.Assessments, version)
		}
		if len(ids) == q.Limit {
			result.NextCursor = &ids[len(ids)-1]
		}
		return result, nil

	case *AssessmentsSoftDeletedSince:
		if err := d.validateMaxLookback(c, q.Since); err != nil {
			return nil, err
		}
		ids, err := store.FindAssessmentUUIDsSoftDeletedSince(ctx, db, q.AssessmentType, q.Since)
		return AssessmentsSoftDeletedSinceResult{ids}, err
	}
	return nil, fmt.Errorf("no handler for %s", query.Type())
}

func latest(
	ctx context.Context, db store.DB, c clock.Clock, identifier AssessmentIdentifier, timestamp *datetime.LocalDateTime,
) (*aggregate.Entity, error) {
	assessment, err := FindAssessment(ctx, db, identifier, c.Now())
	if err != nil {
		return nil, err
	}
	state, err := engine.FetchOrCreateState(ctx, db, c, assessment, timestamp)
	if err != nil {
		return nil, err
	}
	return state.Latest(), nil
}

func assessmentVersion(
	ctx context.Context, db store.DB, c clock.Clock, assessment *entity.Assessment, timestamp *datetime.LocalDateTime,
) (*AssessmentVersionResult, error) {
	state, err := engine.FetchOrCreateState(ctx, db, c, assessment, timestamp)
	if err != nil {
		return nil, err
	}
	snapshot := state.Latest()
	data := snapshot.Data

	users, err := store.FindUsersByUUIDs(ctx, db, data.Collaborators)
	if err != nil {
		return nil, err
	}
	collaborators := make([]model.User, 0, len(users))
	for _, user := range users {
		collaborators = append(collaborators, user.User())
	}

	var assigned *model.User
	if data.AssignedUser != nil {
		for _, collaborator := range collaborators {
			if collaborator.ID == *data.AssignedUser {
				assigned = &collaborator
				break
			}
		}
		if assigned == nil {
			found, err := store.FindUserByUUID(ctx, db, *data.AssignedUser)
			if err != nil {
				return nil, err
			}
			user := found.User()
			assigned = &user
		}
	}

	return &AssessmentVersionResult{
		AssessmentUUID: assessment.UUID, AggregateUUID: snapshot.UUID, AssessmentType: assessment.Type,
		FormVersion: data.FormVersion, CreatedAt: snapshot.EventsFrom, UpdatedAt: snapshot.EventsTo,
		Answers: data.Answers, Properties: data.Properties, Collections: data.Collections,
		Collaborators: collaborators, Identifiers: assessment.IdentifiersMap(), AssignedUser: assigned, Flags: data.Flags,
	}, nil
}

func timeline(ctx context.Context, db store.DB, c clock.Clock, q *Timeline) (Result, error) {
	if q.PageNumber < 0 {
		return nil, apperr.InvalidQuery("Page index must not be less than zero")
	}
	if q.PageSize < 1 {
		return nil, apperr.InvalidQuery("Page size must not be less than one")
	}

	criteria := store.TimelineCriteria{
		From: q.From, To: q.To,
		IncludeEventTypes: q.IncludeEventTypes, ExcludeEventTypes: q.ExcludeEventTypes,
		IncludeCustomTypes: q.IncludeCustomTypes, ExcludeCustomTypes: q.ExcludeCustomTypes,
	}
	if q.AssessmentIdentifier != nil {
		assessment, err := FindAssessment(ctx, db, *q.AssessmentIdentifier, c.Now())
		if err != nil {
			return nil, err
		}
		criteria.AssessmentUUID = &assessment.UUID
	}
	if q.Subject != nil {
		user, err := store.FindUserByUserIDAndAuthSource(ctx, db, q.Subject.ID, q.Subject.AuthSource)
		if err != nil {
			return nil, err
		}
		if user == nil {
			return nil, apperr.UserDetailsNotFound()
		}
		criteria.UserUUID = &user.UUID
	}

	entries, total, err := store.FindTimelinePage(ctx, db, criteria, q.PageNumber, q.PageSize)
	if err != nil {
		return nil, err
	}
	result := TimelineResult{
		Timeline: make([]model.TimelineItem, 0, len(entries)),
		PageInfo: PageInfo{PageNumber: q.PageNumber, TotalPages: int((total + int64(q.PageSize) - 1) / int64(q.PageSize))},
	}
	for _, entry := range entries {
		item, err := entry.Item()
		if err != nil {
			return nil, err
		}
		result.Timeline = append(result.Timeline, item)
	}
	return result, nil
}

func (d *Dispatcher) validateMaxLookback(c clock.Clock, since datetime.LocalDateTime) error {
	if d.MaxLookbackDays > 0 && since.Before(c.Now().AddDays(-d.MaxLookbackDays)) {
		return apperr.InvalidQuery(fmt.Sprintf("The 'since' parameter cannot be older than %d day(s)", d.MaxLookbackDays))
	}
	return nil
}

func subjectAccessRequest(ctx context.Context, db store.DB, c clock.Clock, q *SubjectAccessRequest) (*SubjectAccessRequestResult, error) {
	var from, to *datetime.LocalDateTime
	if q.From != nil {
		start := q.From.StartOfDay()
		from = &start
	}
	if q.To != nil {
		end := q.To.EndOfDay().ToDatabasePrecision()
		to = &end
	}
	ids, err := store.FindAssessmentUUIDsByExternalIdentifiers(ctx, db, q.AssessmentIdentifiers, from, to)
	if err != nil {
		return nil, err
	}
	if len(ids) == 0 {
		identifiers := make([]string, len(q.AssessmentIdentifiers))
		for i, pair := range q.AssessmentIdentifiers {
			identifiers[i] = fmt.Sprintf("%s=%s", pair.Type, pair.ID)
		}
		return nil, apperr.SubjectAccessRequestNoAssessments(identifiers)
	}

	pointInTime := c.Now()
	switch {
	case q.To != nil:
		pointInTime = q.To.EndOfDay()
	case q.Timestamp != nil:
		pointInTime = *q.Timestamp
	}

	result := &SubjectAccessRequestResult{Results: []SubjectAccessRequestAssessmentVersion{}}
	for _, id := range ids {
		assessment, err := engine.FindAssessment(ctx, db, id)
		if err != nil {
			return nil, err
		}
		state, err := engine.FetchOrCreateState(ctx, db, c, assessment, &pointInTime)
		if err != nil {
			return nil, err
		}
		result.Results = append(result.Results, newSubjectAccessRequestVersion(state.Latest(), assessment))
	}
	return result, nil
}

// truncateCollection keeps items nested depth collections deep, and no deeper.
func truncateCollection(collection *model.Collection, depth, currentDepth int) *model.Collection {
	truncated := *collection
	truncated.Items = make([]*model.CollectionItem, len(collection.Items))
	for i, item := range collection.Items {
		copied := *item
		copied.Collections = []*model.Collection{}
		if currentDepth < depth {
			for _, nested := range item.Collections {
				copied.Collections = append(copied.Collections, truncateCollection(nested, depth, currentDepth+1))
			}
		}
		truncated.Items[i] = &copied
	}
	return &truncated
}

// truncateCollectionItem keeps collections nested depth items deep, and no deeper.
func truncateCollectionItem(item *model.CollectionItem, depth, currentDepth int) *model.CollectionItem {
	truncated := *item
	truncated.Collections = make([]*model.Collection, len(item.Collections))
	for i, collection := range item.Collections {
		copied := *collection
		copied.Items = []*model.CollectionItem{}
		if currentDepth < depth {
			for _, nested := range collection.Items {
				copied.Items = append(copied.Items, truncateCollectionItem(nested, depth, currentDepth+1))
			}
		}
		truncated.Collections[i] = &copied
	}
	return &truncated
}
