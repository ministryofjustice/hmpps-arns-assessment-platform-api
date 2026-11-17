package oasys

import (
	"encoding/json"
	"fmt"
	"slices"
	"strings"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/aggregate"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/formconfig"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/oasys/codes"
)

// Question is an OASys question code and how to answer it. A nil answer is written as null.
type Question struct {
	Code   string
	Answer func(*Provider) any
}

// q builds Questions from alternating codes and answer functions.
func q(pairs ...any) []Question {
	questions := make([]Question, 0, len(pairs)/2)
	for i := 0; i+1 < len(pairs); i += 2 {
		questions = append(questions, Question{pairs[i].(string), pairs[i+1].(func(*Provider) any)})
	}
	return questions
}

// A section lists its questions, in output order. Listing them may read answers.
type section func(*Provider) []Question

var versions = map[string][]section{
	"v1.0": {
		accommodation,
		alcoholMisuse,
		attitudes,
		drugs,
		education,
		emotionalWellbeing,
		financialManagement,
		lifestyleAssociates,
		newSections,
		offenceAnalysis,
		predictors,
		relationships,
		thinkingBehaviours,
	},
}

// OasysEquivalent is the OASys equivalent of the assessment, keyed by OASys question code.
func OasysEquivalent(assessment *aggregate.Assessment, config *formconfig.FormConfig) (equivalent jsonx.Object, err error) {
	sections, ok := versions[config.Version]
	if !ok {
		return jsonx.Object{}, fmt.Errorf("No data mapping found for form version %s", config.Version) //nolint:staticcheck // matches the Kotlin message
	}

	defer func() {
		if recovered := recover(); recovered != nil {
			failure, ok := recovered.(mappingError)
			if !ok {
				panic(recovered)
			}
			err = failure.error
		}
	}()

	p := &Provider{assessment: assessment, config: config}
	for _, section := range sections {
		for _, question := range section(p) {
			answer, err := json.Marshal(question.Answer(p))
			if err != nil {
				return jsonx.Object{}, err
			}
			equivalent.Set(question.Code, answer)
		}
	}
	return equivalent, nil
}

// yesNo is YES or NO when the field has that answer.
func yesNo(field codes.Field) func(*Provider) any {
	return func(p *Provider) any {
		return p.When(p.Value(field), On("YES", codes.ValueYes), On("NO", codes.ValueNo))
	}
}

// valueOf is the single answer to the field as-is.
func valueOf(field codes.Field) func(*Provider) any {
	return func(p *Provider) any { return optional(p.Value(field)) }
}

// ticked is YES or NO for whether a checkbox field has the option ticked, or nil when unanswered.
func ticked(field codes.Field, option codes.Value) func(*Provider) any {
	return func(p *Provider) any {
		values, answered := p.Values(field)
		switch {
		case !answered:
			return nil
		case p.Contains(values, option):
			return "YES"
		default:
			return "NO"
		}
	}
}

func optional(value *string) any {
	if value == nil {
		return nil
	}
	return *value
}

// anyOf is a short-circuiting any, in order.
func anyOf[T any](items []T, predicate func(T) bool) bool {
	return slices.ContainsFunc(items, predicate)
}

// practitionerAnalysis is the questions at the end of each section, for the section prefix.
type practitionerAnalysis string

func (a practitionerAnalysis) field(suffix string) codes.Field {
	name := codes.Field(string(a) + "_PRACTITIONER_ANALYSIS_" + suffix)
	if !slices.Contains(codes.Fields, name) {
		fail("No field named %s", name)
	}
	return name
}

func notBlank(value *string) *string {
	if value == nil || strings.TrimSpace(*value) == "" {
		return nil
	}
	return value
}

func (a practitionerAnalysis) labelledNotes(p *Provider, area, yesLabel, noLabel string) *string {
	yes := notBlank(p.Value(a.field(area + "_YES_DETAILS")))
	no := notBlank(p.Value(a.field(area + "_NO_DETAILS")))
	var note string
	switch {
	case yes != nil:
		note = yesLabel + " - " + *yes
	case no != nil:
		note = noLabel + " - " + *no
	default:
		return nil
	}
	return &note
}

func (a practitionerAnalysis) notes(p *Provider) any {
	var notes []string
	for _, note := range []*string{
		a.labelledNotes(p, "STRENGTHS_OR_PROTECTIVE_FACTORS",
			"Strengths and protective factor notes", "Area not linked to strengths and positive factors notes"),
		a.labelledNotes(p, "RISK_OF_SERIOUS_HARM",
			"Area linked to serious harm notes", "Area not linked to serious harm notes"),
		a.labelledNotes(p, "RISK_OF_REOFFENDING",
			"Risk of reoffending notes", "Area not linked to reoffending notes"),
	} {
		if note != nil {
			notes = append(notes, *note)
		}
	}
	if len(notes) == 0 {
		return nil
	}
	return strings.Join(notes, "\n")
}

func (a practitionerAnalysis) riskOfSeriousHarm(p *Provider) any {
	return yesNo(a.field("RISK_OF_SERIOUS_HARM"))(p)
}

func (a practitionerAnalysis) riskOfReoffending(p *Provider) any {
	return yesNo(a.field("RISK_OF_REOFFENDING"))(p)
}

func (a practitionerAnalysis) strengthsOrProtectiveFactors(p *Provider) any {
	return yesNo(a.field("STRENGTHS_OR_PROTECTIVE_FACTORS"))(p)
}

// analysisQuestions are the usual practitioner analysis questions for a section.
func (a practitionerAnalysis) questions(prefix string, withStrength bool) []Question {
	questions := q(
		prefix+"-97", a.notes,
		prefix+"-98", a.riskOfSeriousHarm,
		prefix+"-99", a.riskOfReoffending,
	)
	if withStrength {
		questions = append(questions, Question{prefix + "_SAN_STRENGTH", a.strengthsOrProtectiveFactors})
	}
	return questions
}
