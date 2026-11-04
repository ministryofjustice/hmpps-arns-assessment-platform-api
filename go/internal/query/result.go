package query

import (
	"strconv"
	"strings"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/aggregate"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/entity"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/omap"
)

// Result is what a query returns. Every result writes its own "type", so results nested in other
// results carry it too, as they did from the Kotlin service.
type Result interface {
	resultType() string
}

type AssessmentVersionResult struct {
	AssessmentUUID uuid.UUID              `json:"assessmentUuid"`
	AggregateUUID  uuid.UUID              `json:"aggregateUuid"`
	AssessmentType string                 `json:"assessmentType"`
	FormVersion    string                 `json:"formVersion"`
	CreatedAt      datetime.LocalDateTime `json:"createdAt"`
	UpdatedAt      datetime.LocalDateTime `json:"updatedAt"`
	Answers        model.Answers          `json:"answers"`
	Properties     model.Answers          `json:"properties"`
	Collections    []*model.Collection    `json:"collections"`
	Collaborators  []model.User           `json:"collaborators"`
	Identifiers    omap.Map[string]       `json:"identifiers"`
	AssignedUser   *model.User            `json:"assignedUser"`
	Flags          []string               `json:"flags"`
}

type CollectionItemResult struct {
	CollectionItem *model.CollectionItem `json:"collectionItem"`
}

type CollectionResult struct {
	Collection *model.Collection `json:"collection"`
}

type DailyVersionsResult struct {
	Versions []model.DailyVersionDetails `json:"versions"`
}

type AssessmentsModifiedSinceResult struct {
	Assessments []*AssessmentVersionResult `json:"assessments"`
	// NextCursor is passed as after to get the next page, when there may be one.
	NextCursor *uuid.UUID `json:"nextCursor"`
}

type AssessmentsSoftDeletedSinceResult struct {
	Assessments []uuid.UUID `json:"assessments"`
}

type PageInfo struct {
	PageNumber int `json:"pageNumber"`
	TotalPages int `json:"totalPages"`
}

type TimelineResult struct {
	Timeline []model.TimelineItem `json:"timeline"`
	PageInfo PageInfo             `json:"pageInfo"`
}

type SubjectAccessRequestResult struct {
	Results []SubjectAccessRequestAssessmentVersion `json:"results"`
}

func (AssessmentVersionResult) resultType() string { return "AssessmentVersionQueryResult" }
func (CollectionItemResult) resultType() string    { return "CollectionItemQueryResult" }
func (CollectionResult) resultType() string        { return "CollectionQueryResult" }
func (DailyVersionsResult) resultType() string     { return "DailyVersionsQueryResult" }
func (AssessmentsModifiedSinceResult) resultType() string {
	return "GetAssessmentsModifiedSinceQueryResult"
}
func (AssessmentsSoftDeletedSinceResult) resultType() string {
	return "GetAssessmentsSoftDeletedSinceQueryResult"
}
func (TimelineResult) resultType() string             { return "TimelineQueryResult" }
func (SubjectAccessRequestResult) resultType() string { return "SubjectAccessRequestQueryResult" }

func (r AssessmentVersionResult) MarshalJSON() ([]byte, error) {
	type plain AssessmentVersionResult
	if r.Collections == nil {
		r.Collections = []*model.Collection{}
	}
	if r.Collaborators == nil {
		r.Collaborators = []model.User{}
	}
	if r.Flags == nil {
		r.Flags = []string{}
	}
	return jsonx.Tag(r.resultType(), plain(r))
}

func (r CollectionItemResult) MarshalJSON() ([]byte, error) {
	type plain CollectionItemResult
	return jsonx.Tag(r.resultType(), plain(r))
}

func (r CollectionResult) MarshalJSON() ([]byte, error) {
	type plain CollectionResult
	return jsonx.Tag(r.resultType(), plain(r))
}

func (r DailyVersionsResult) MarshalJSON() ([]byte, error) {
	type plain DailyVersionsResult
	if r.Versions == nil {
		r.Versions = []model.DailyVersionDetails{}
	}
	return jsonx.Tag(r.resultType(), plain(r))
}

func (r AssessmentsModifiedSinceResult) MarshalJSON() ([]byte, error) {
	type plain AssessmentsModifiedSinceResult
	if r.Assessments == nil {
		r.Assessments = []*AssessmentVersionResult{}
	}
	return jsonx.Tag(r.resultType(), plain(r))
}

func (r AssessmentsSoftDeletedSinceResult) MarshalJSON() ([]byte, error) {
	type plain AssessmentsSoftDeletedSinceResult
	if r.Assessments == nil {
		r.Assessments = []uuid.UUID{}
	}
	return jsonx.Tag(r.resultType(), plain(r))
}

func (r TimelineResult) MarshalJSON() ([]byte, error) {
	type plain TimelineResult
	if r.Timeline == nil {
		r.Timeline = []model.TimelineItem{}
	}
	return jsonx.Tag(r.resultType(), plain(r))
}

func (r SubjectAccessRequestResult) MarshalJSON() ([]byte, error) {
	type plain SubjectAccessRequestResult
	return jsonx.Tag(r.resultType(), plain(r))
}

type Response struct {
	Request Query  `json:"request"`
	Result  Result `json:"result"`
}

type Responses struct {
	Queries []Response `json:"queries"`
}

// SubjectAccessRequestAssessmentVersion is an assessment flattened for the subject access
// request template.
type SubjectAccessRequestAssessmentVersion struct {
	AssessmentType         string                 `json:"assessmentType"`
	CreatedAt              datetime.LocalDateTime `json:"createdAt"`
	UpdatedAt              datetime.LocalDateTime `json:"updatedAt"`
	Answers                []RenderedValue        `json:"answers"`
	Properties             []RenderedValue        `json:"properties"`
	Collections            []RenderedCollection   `json:"collections"`
	Identifiers            omap.Map[string]       `json:"identifiers"`
	HasAnswersOrProperties bool                   `json:"hasAnswersOrProperties"`
}

type RenderedValue struct {
	Key         string   `json:"key"`
	Single      *string  `json:"single"`
	Multiple    []string `json:"multiple"`
	HasSingle   bool     `json:"hasSingle"`
	HasMultiple bool     `json:"hasMultiple"`
}

type RenderedCollectionItem struct {
	Name       string          `json:"name"`
	Answers    []RenderedValue `json:"answers"`
	Properties []RenderedValue `json:"properties"`
}

type RenderedCollection struct {
	Name  string                   `json:"name"`
	Items []RenderedCollectionItem `json:"items"`
}

func newSubjectAccessRequestVersion(snapshot *aggregate.Entity, assessment *entity.Assessment) SubjectAccessRequestAssessmentVersion {
	answers, properties := renderedValues(snapshot.Data.Answers), renderedValues(snapshot.Data.Properties)
	collections := []RenderedCollection{}
	for _, collection := range snapshot.Data.Collections {
		collections = append(collections, flatten(collection, nil)...)
	}
	return SubjectAccessRequestAssessmentVersion{
		AssessmentType: assessment.Type, CreatedAt: snapshot.EventsFrom, UpdatedAt: snapshot.EventsTo,
		Answers: answers, Properties: properties, Collections: collections, Identifiers: assessment.IdentifiersMap(),
		HasAnswersOrProperties: len(answers) > 0 || len(properties) > 0,
	}
}

func renderedValues(values model.Answers) []RenderedValue {
	rendered := []RenderedValue{}
	for key, value := range values.All() {
		if single, ok := value.SingleValue(); ok {
			rendered = append(rendered, RenderedValue{Key: key, Single: &single, HasSingle: true})
		} else {
			multiple, _ := value.MultiValues()
			if multiple == nil {
				multiple = []string{}
			}
			rendered = append(rendered, RenderedValue{Key: key, Multiple: multiple, HasMultiple: true})
		}
	}
	return rendered
}

// flatten gives the collection followed by every collection nested beneath it, each named by
// its path, such as GOALS/1/STEPS.
func flatten(collection *model.Collection, path []string) []RenderedCollection {
	collectionPath := append(append([]string{}, path...), collection.Name)
	itemPath := func(index int) []string {
		return append(append([]string{}, collectionPath...), strconv.Itoa(index+1))
	}

	current := RenderedCollection{Name: strings.Join(collectionPath, "/"), Items: []RenderedCollectionItem{}}
	var nested []RenderedCollection
	for index, item := range collection.Items {
		current.Items = append(current.Items, RenderedCollectionItem{
			Name:       strings.Join(itemPath(index), "/"),
			Answers:    renderedValues(item.Answers),
			Properties: renderedValues(item.Properties),
		})
		for _, child := range item.Collections {
			nested = append(nested, flatten(child, itemPath(index))...)
		}
	}
	return append([]RenderedCollection{current}, nested...)
}
