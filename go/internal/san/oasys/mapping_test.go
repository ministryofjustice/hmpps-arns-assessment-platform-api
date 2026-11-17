package oasys

import (
	"encoding/json"
	"slices"
	"strings"
	"testing"

	"github.com/google/uuid"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/aggregate"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/formconfig"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/oasys/codes"
)

// fullConfig has every field the v1 mapping reads, accepting every option, so each test only
// has to set the answers it cares about.
func fullConfig() *formconfig.FormConfig {
	options := make([]formconfig.Option, len(codes.Values))
	for i, value := range codes.Values {
		name := string(value)
		options[i] = formconfig.Option{Value: &name}
	}
	checkboxes := []codes.Field{
		codes.FieldDrugsInjected, codes.FieldEducationDifficulties, codes.FieldFinanceIncome, codes.FieldLivingWith,
		codes.FieldOffenceAnalysisElements, codes.FieldOffenceAnalysisMotivations,
		codes.FieldPersonalRelationshipsCommunityImportantPeople,
	}
	config := &formconfig.FormConfig{Version: "v1.0"}
	for _, field := range codes.Fields {
		fieldType := formconfig.FieldTypeRadio
		if slices.Contains(checkboxes, field) || strings.HasPrefix(string(field), "DRUGS_INJECTED_") {
			fieldType = formconfig.FieldTypeCheckbox
		}
		config.Fields.Set(field.Lower(), formconfig.FieldConfig{Code: field.Lower(), Options: options, Type: fieldType})
	}
	config.Fields.Set("victim_collection_field", formconfig.FieldConfig{
		Code: "victims", Type: formconfig.FieldTypeText, Collection: string(codes.CollectionOffenceAnalysisVictim),
	})
	return config
}

func answer(assessment *aggregate.Assessment, field codes.Field, value model.Value) {
	assessment.Answers.Set(field.Lower(), value)
}

func single(value codes.Value) model.Value { return model.Single(string(value)) }

func mapped(t *testing.T, equivalent jsonx.Object, code string) string {
	t.Helper()
	raw, ok := equivalent.Get(code)
	if !ok {
		t.Fatalf("no answer for %s", code)
	}
	return string(raw)
}

func TestMapsEverySectionInOrderIncludingUnansweredQuestions(t *testing.T) {
	equivalent, err := OasysEquivalent(&aggregate.Assessment{}, fullConfig())
	if err != nil {
		t.Fatal(err)
	}

	keys := equivalent.Keys()
	if !slices.Equal(keys[:3], []string{"o3-3", "o3-4", "o3-5"}) || keys[len(keys)-1] != "o11-99" {
		t.Errorf("unexpected order %v ... %s", keys[:3], keys[len(keys)-1])
	}
	for code, want := range map[string]string{"o3-3": "null", "oAC_SAN_SECTION_COMP": `"NO"`, "o8-4": `"0"`, "o6-8": `"3"`} {
		if got := mapped(t, equivalent, code); got != want {
			t.Errorf("%s: got %s, want %s", code, got, want)
		}
	}
}

func TestScoresAlcoholUse(t *testing.T) {
	assessment := &aggregate.Assessment{}
	answer(assessment, codes.FieldAlcoholUse, single(codes.ValueYesWithinLastThreeMonths))
	answer(assessment, codes.FieldAlcoholFrequency, single(codes.ValueMoreThan4TimesAWeek))
	answer(assessment, codes.FieldAlcoholUnits, single(codes.ValueUnits10OrMore))

	equivalent, err := OasysEquivalent(assessment, fullConfig())
	if err != nil {
		t.Fatal(err)
	}

	if got := mapped(t, equivalent, "o9-1"); got != `"2"` {
		t.Errorf("o9-1: got %s", got)
	}
	if got := mapped(t, equivalent, "o9-1-t"); got != `"Drinks more than four times a week and consumes 10 or more units a day, when they drink."` {
		t.Errorf("o9-1-t: got %s", got)
	}
	if got := mapped(t, equivalent, "o9-2"); got != "null" {
		t.Errorf("o9-2: got %s", got)
	}
}

func TestMapsEachVictimAndCheckboxAnswers(t *testing.T) {
	assessment := &aggregate.Assessment{}
	answer(assessment, codes.FieldOffenceAnalysisElements, model.Multi(string(codes.ValueWeapon), string(codes.ValueVictimTargeted)))
	victim := &model.CollectionItem{UUID: uuid.New()}
	victim.Answers.Set(codes.FieldOffenceAnalysisVictimAge.Lower(), single(codes.ValueAge18To20Years))
	victim.Answers.Set(codes.FieldOffenceAnalysisVictimRelationship.Lower(), single(codes.ValueStranger))
	assessment.Collections = []*model.Collection{{UUID: uuid.New(), Name: "OFFENCE_ANALYSIS_VICTIM", Items: []*model.CollectionItem{victim}}}

	equivalent, err := OasysEquivalent(assessment, fullConfig())
	if err != nil {
		t.Fatal(err)
	}

	for code, want := range map[string]string{
		"o2-2_V2_WEAPON": `"YES"`,
		"o2-2_V2_ARSON":  `"NO"`,
		"o2-3":           `"DIRECTCONT,STRANGERS"`,
		"victim0":        `{"oAGE_OF_VICTIM_ELM":"4","oGENDER_ELM":null,"oETHNIC_CATEGORY_ELM":null,"oVICTIM_RELATION_ELM":"0"}`,
	} {
		if got := mapped(t, equivalent, code); got != want {
			t.Errorf("%s: got %s, want %s", code, got, want)
		}
	}
	keys := equivalent.Keys()
	if keys[slices.Index(keys, "victim0")-1] != "o2-99" {
		t.Error("expected victims to follow the offence analysis questions")
	}
}

func TestFailsForOptionsMissingFromTheFormConfigOnlyWhenTheyAreChecked(t *testing.T) {
	config := fullConfig()
	temporary, _ := config.Fields.Get(codes.FieldTypeOfTemporaryAccommodation.Lower())
	temporary.Options = slices.DeleteFunc(slices.Clone(temporary.Options), func(o formconfig.Option) bool { return *o.Value == "SHORT_TERM" })
	config.Fields.Set(codes.FieldTypeOfTemporaryAccommodation.Lower(), temporary)

	// SHORT_TERM is only checked for temporary accommodation...
	noAccommodation := &aggregate.Assessment{}
	answer(noAccommodation, codes.FieldCurrentAccommodation, single(codes.ValueNoAccommodation))
	if equivalent, err := OasysEquivalent(noAccommodation, config); err != nil || mapped(t, equivalent, "o3-5") != `"2"` {
		t.Errorf("got %v", err)
	}

	// ...so only then does its absence from the config fail the mapping, as it did in Kotlin.
	temporaryAccommodation := &aggregate.Assessment{}
	answer(temporaryAccommodation, codes.FieldCurrentAccommodation, single(codes.ValueTemporary))
	_, err := OasysEquivalent(temporaryAccommodation, config)
	if err == nil || err.Error() != "SHORT_TERM is not a valid option for field type_of_temporary_accommodation in form config version v1.0" {
		t.Errorf("got %v", err)
	}
}

func TestFailsForUnknownFormVersionsAndFields(t *testing.T) {
	config := fullConfig()
	config.Version = "v2.0"
	if _, err := OasysEquivalent(&aggregate.Assessment{}, config); err == nil || err.Error() != "No data mapping found for form version v2.0" {
		t.Errorf("got %v", err)
	}

	config = fullConfig()
	config.Fields.Delete(codes.FieldDrugUse.Lower())
	if _, err := OasysEquivalent(&aggregate.Assessment{}, config); err == nil || err.Error() != "Field drug_use does not exist in form config version v1.0" {
		t.Errorf("got %v", err)
	}
}

func TestNormalisesAnswersForOasys(t *testing.T) {
	assessment := &aggregate.Assessment{}
	answer(assessment, codes.FieldOtherDrugName, model.Single("  It’s a “test” — really  "))
	answer(assessment, codes.FieldDrugsInjected, model.Multi(""))
	p := &Provider{assessment: assessment, config: fullConfig()}

	if got := *p.Value(codes.FieldOtherDrugName); got != `It's a "test" - really` {
		t.Errorf("got %q", got)
	}
	if values, answered := p.Values(codes.FieldDrugsInjected); !answered || len(values) != 0 {
		t.Errorf("got %v %v", values, answered)
	}
	out, _ := json.Marshal(p.When(nil, Else("x")))
	if string(out) != `"x"` {
		t.Errorf("got %s", out)
	}
}
