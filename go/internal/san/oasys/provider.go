// Package oasys maps a SAN assessment to the equivalent OASys question answers, for the OASys
// integration.
//
// Reading a field or option that isn't in the form config fails the whole mapping, as it did in
// the Kotlin service. To keep the mapping readable, those failures panic with a mappingError,
// which OasysEquivalent recovers and returns as an error, as encoding/json does internally.
package oasys

import (
	"fmt"
	"regexp"
	"slices"
	"strings"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/aggregate"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/formconfig"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/oasys/codes"
)

type mappingError struct{ error }

func fail(format string, args ...any) {
	panic(mappingError{fmt.Errorf(format, args...)})
}

// Matches the HMPO form wizard default formatters used by the original SAN service. The UI
// stores backticks HTML-encoded, so the encoded form is matched too.
var (
	hyphens     = regexp.MustCompile("[–—-]+")
	apostrophes = regexp.MustCompile("[`‘’]|&#96;")
	quotes      = regexp.MustCompile("[“”]")
)

func normaliseForOasys(value string) string {
	value = hyphens.ReplaceAllString(strings.TrimSpace(value), "-")
	value = apostrophes.ReplaceAllString(value, "'")
	return quotes.ReplaceAllString(value, `"`)
}

// Provider reads answers for the mapping, validating every field and option against the form
// config.
//
// Option lookups (Get) are validated against the field most recently read, so mappings must read
// a field before comparing its options, as the Kotlin mapping did.
type Provider struct {
	assessment *aggregate.Assessment
	config     *formconfig.FormConfig
	context    *string
}

func (p *Provider) SetContext(field codes.Field) {
	context := field.Lower()
	p.context = &context
}

func (p *Provider) fieldConfig(field codes.Field) (formconfig.FieldConfig, *model.Value) {
	p.SetContext(field)
	config, ok := p.config.Fields.Get(field.Lower())
	if !ok {
		fail("Field %s does not exist in form config version %s", field.Lower(), p.config.Version)
	}
	if answer, ok := p.assessment.Answers.Get(config.Code); ok {
		return config, &answer
	}
	if property, ok := p.assessment.Properties.Get(config.Code); ok {
		return config, &property
	}
	return config, nil
}

// Value is the answer to a single-value field, or nil.
func (p *Provider) Value(field codes.Field) *string {
	config, answer := p.fieldConfig(field)
	if config.Type == formconfig.FieldTypeCheckbox {
		fail("Invalid use of '.value' on a MultipleValuesAnswer")
	}
	if answer == nil {
		return nil
	}
	single, ok := answer.SingleValue()
	if !ok {
		return nil
	}
	normalised := normaliseForOasys(single)
	return &normalised
}

// Values is the answer to a checkbox field, and whether there is one.
func (p *Provider) Values(field codes.Field) ([]string, bool) {
	config, answer := p.fieldConfig(field)
	if config.Type != formconfig.FieldTypeCheckbox {
		fail("Invalid use of '.values' on a SingleValueAnswer")
	}
	if answer == nil {
		return nil, false
	}
	multi, ok := answer.MultiValues()
	if !ok {
		return nil, false
	}
	normalised := make([]string, len(multi))
	for i, value := range multi {
		normalised[i] = normaliseForOasys(value)
	}
	if slices.Equal(normalised, []string{""}) {
		return []string{}, true
	}
	return normalised, true
}

// Collection is the answers of each item in the named collection.
func (p *Provider) Collection(collection codes.Collection) []model.Answers {
	p.context = nil
	inConfig := false
	for _, field := range p.config.Fields.All() {
		inConfig = inConfig || field.Collection == string(collection)
	}
	if !inConfig {
		fail("Collection %s does not exist in form config version %s", collection, p.config.Version)
	}
	for _, found := range p.assessment.Collections {
		if found.Name == string(collection) {
			items := make([]model.Answers, len(found.Items))
			for i, item := range found.Items {
				items[i] = item.Answers
			}
			return items
		}
	}
	return []model.Answers{}
}

// Get is the option's name, provided it's an option of the field most recently read.
func (p *Provider) Get(value codes.Value) string {
	if p.context == nil {
		fail("Cannot obtain values without a field context. Call answer() first")
	}
	config, ok := p.config.Fields.Get(*p.context)
	if !ok || !config.HasOption(string(value)) {
		fail("%s is not a valid option for field %s in form config version %s", value, *p.context, p.config.Version)
	}
	return string(value)
}

// Is reports whether subject is the option, validating the option even when there's no subject.
func (p *Provider) Is(subject *string, value codes.Value) bool {
	option := p.Get(value)
	return subject != nil && *subject == option
}

// Contains reports whether values includes the option, validating the option first.
func (p *Provider) Contains(values []string, value codes.Value) bool {
	return slices.Contains(values, p.Get(value))
}

// Arm is one branch of When.
type Arm struct {
	options   []codes.Value
	otherwise bool
	result    func() any
}

// On gives result when the subject is any of the options.
func On(result any, options ...codes.Value) Arm {
	return Arm{options: options, result: func() any { return result }}
}

// OnFunc is On with a result that's only worked out if it's needed.
func OnFunc(result func() any, options ...codes.Value) Arm {
	return Arm{options: options, result: result}
}

// Else gives result when no other arm matched.
func Else(result any) Arm { return ElseFunc(func() any { return result }) }

func ElseFunc(result func() any) Arm { return Arm{otherwise: true, result: result} }

// When is the equivalent of a Kotlin `when (subject) { ap.get(A), ap.get(B) -> x ... else -> y }`.
//
// Options are validated lazily and in order, stopping at the first match, against whichever field
// was read most recently. That is what made the Kotlin mapping fail (or not) for options missing
// from a form config, so it has to be preserved. Without an Else, no match gives nil.
func (p *Provider) When(subject *string, arms ...Arm) any {
	for _, arm := range arms {
		if arm.otherwise {
			return arm.result()
		}
		for _, option := range arm.options {
			if p.Is(subject, option) {
				return arm.result()
			}
		}
	}
	return nil
}
