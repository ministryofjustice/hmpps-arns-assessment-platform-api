package oasys

import (
	"fmt"
	"strings"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/jsonx"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/model"
	. "github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/oasys/codes" //nolint:revive // reads like the Kotlin mapping
)

func offenceAnalysis(p *Provider) []Question {
	questions := q(
		"o2-1", valueOf(FieldOffenceAnalysisDescriptionOfOffence),
		"o2-2_V2_WEAPON", ticked(FieldOffenceAnalysisElements, ValueWeapon),
		"o2-2_V2_ANYVIOL", ticked(FieldOffenceAnalysisElements, ValueViolenceOrCoercion),
		"o2-2_V2_ARSON", ticked(FieldOffenceAnalysisElements, ValueArson),
		"o2-2_V2_DOM_ABUSE", ticked(FieldOffenceAnalysisElements, ValueDomesticAbuse),
		"o2-2_V2_EXCESSIVE", ticked(FieldOffenceAnalysisElements, ValueExcessiveOrSadisticViolence),
		"o2-2_V2_PHYSICALDAM", ticked(FieldOffenceAnalysisElements, ValuePhysicalDamageToProperty),
		"o2-2_V2_SEXUAL", ticked(FieldOffenceAnalysisElements, ValueSexualElement),
		"o2-3", offenceAnalysisQ3,
		"o2-6", yesNoIfAnswered(FieldOffenceAnalysisImpactOnVictims),
		"o2-7", func(p *Provider) any {
			involved := p.Value(FieldOffenceAnalysisHowManyInvolved)
			if involved == nil {
				return nil
			}
			return p.When(involved,
				On("YES", ValueOne, ValueTwo, ValueThree, ValueFour, ValueFive, ValueSixTo10, ValueElevenTo15, ValueMoreThan15),
				On("NO", ValueNone))
		},
		"o2-7-1", func(p *Provider) any {
			involved := p.Value(FieldOffenceAnalysisHowManyInvolved)
			if involved == nil {
				return nil
			}
			return p.When(involved,
				On("110", ValueOne), On("120", ValueTwo), On("130", ValueThree), On("140", ValueFour),
				On("150", ValueFive), On("160", ValueSixTo10), On("170", ValueElevenTo15), On("180", ValueMoreThan15))
		},
		"o2-7-2", ticked(FieldOffenceAnalysisMotivations, ValuePressurisedByOthers),
		"o2-7-3", func(p *Provider) any {
			withDetails := func(label string, field Field) func() any {
				return func() any {
					if details := p.Value(field); details != nil {
						return label + " - " + *details
					}
					return label
				}
			}
			return p.When(p.Value(FieldOffenceAnalysisLeader),
				OnFunc(withDetails("Yes", FieldOffenceAnalysisLeaderYesDetails), ValueYes),
				OnFunc(withDetails("No", FieldOffenceAnalysisLeaderNoDetails), ValueNo))
		},
		"o2-8", valueOf(FieldOffenceAnalysisReason),
		"o2-9_V2_SEXUAL", ticked(FieldOffenceAnalysisMotivations, ValueSexualMotivation),
		"o2-9_V2_FINANCIAL", ticked(FieldOffenceAnalysisMotivations, ValueFinancialMotivation),
		"o2-9_V2_ADDICTION", ticked(FieldOffenceAnalysisMotivations, ValueAddictionsOrPerceivedNeeds),
		"o2-9_V2_EMOTIONAL", ticked(FieldOffenceAnalysisMotivations, ValueEmotionalState),
		"o2-9_V2_RACIAL", ticked(FieldOffenceAnalysisMotivations, ValueHatredOfIdentifiableGroups),
		"o2-9_V2_THRILL", ticked(FieldOffenceAnalysisMotivations, ValueThrillSeeking),
		"o2-9_V2_OTHER", ticked(FieldOffenceAnalysisMotivations, ValueOther),
		"o2-9-t_V2", valueOf(FieldOffenceAnalysisMotivationsOtherDetails),
		"o2-11", yesNoIfAnswered(FieldOffenceAnalysisAcceptResponsibility),
		"o2-11-t", detailsFor(FieldOffenceAnalysisAcceptResponsibility,
			FieldOffenceAnalysisAcceptResponsibilityYesDetails, FieldOffenceAnalysisAcceptResponsibilityNoDetails),
		"o2-12", valueOf(FieldOffenceAnalysisPatternsOfOffending),
		"o2-13", yesNoIfAnswered(FieldOffenceAnalysisEscalation),
		"o2-98", detailsFor(FieldOffenceAnalysisRisk, FieldOffenceAnalysisRiskYesDetails, FieldOffenceAnalysisRiskNoDetails),
		"o2-99", yesNoIfAnswered(FieldOffenceAnalysisRisk),
	)

	// One entry per victim, as victim0, victim1, ...
	for index, victim := range p.Collection(CollectionOffenceAnalysisVictim) {
		questions = append(questions, Question{fmt.Sprintf("victim%d", index), func(p *Provider) any {
			return jsonx.ObjectOf(
				"oAGE_OF_VICTIM_ELM", victimAge(p, victim),
				"oGENDER_ELM", victimGender(p, victim),
				"oETHNIC_CATEGORY_ELM", victimRace(p, victim),
				"oVICTIM_RELATION_ELM", victimRelationship(p, victim),
			)
		}})
	}
	return questions
}

// victimAnswer reads a victim's answer, with the field set as the context for option lookups.
func victimAnswer(p *Provider, victim model.Answers, field Field) *string {
	p.SetContext(field)
	if answer, ok := victim.Get(field.Lower()); ok {
		if single, ok := answer.SingleValue(); ok {
			return &single
		}
	}
	return nil
}

func victimAge(p *Provider, victim model.Answers) any {
	return p.When(victimAnswer(p, victim, FieldOffenceAnalysisVictimAge),
		On("0", ValueAge0To4Years), On("1", ValueAge5To11Years), On("2", ValueAge12To15Years),
		On("3", ValueAge16To17Years), On("4", ValueAge18To20Years), On("5", ValueAge21To25Years),
		On("6", ValueAge26To49Years), On("7", ValueAge50To64Years), On("8", ValueAge65AndOver))
}

func victimGender(p *Provider, victim model.Answers) any {
	return p.When(victimAnswer(p, victim, FieldOffenceAnalysisVictimSex),
		On("1", ValueMale), On("2", ValueFemale), On("0", ValueUnknown))
}

func victimRace(p *Provider, victim model.Answers) any {
	return p.When(victimAnswer(p, victim, FieldOffenceAnalysisVictimRace),
		On("W1", ValueWhiteEnglishWelshScottishNorthernIrishOrBritish),
		On("W2", ValueWhiteIrish),
		On("W4", ValueWhiteGypsyOrIrishTraveller),
		On("W5", ValueWhiteRoma),
		On("W9", ValueWhiteAnyOtherWhiteBackground),
		On("M1", ValueMixedWhiteAndBlackCaribbean),
		On("M2", ValueMixedWhiteAndBlackAfrican),
		On("M3", ValueMixedWhiteAndAsian),
		On("M9", ValueMixedAnyOtherMixedOrMultipleEthnicBackgroundBackground),
		On("A1", ValueAsianOrAsianBritishIndian),
		On("A2", ValueAsianOrAsianBritishPakistani),
		On("A3", ValueAsianOrAsianBritishBangladeshi),
		On("A4", ValueAsianOrAsianBritishChinese),
		On("A9", ValueAsianOrAsianBritishAnyOtherAsianBackground),
		On("B1", ValueBlackOrBlackBritishCaribbean),
		On("B2", ValueBlackOrBlackBritishAfrican),
		On("B9", ValueBlackOrBlackBritishAnyOtherBlackBackground),
		On("O2", ValueArab),
		On("O9", ValueAnyOtherEthnicGroup),
		On("NS", ValueUnknown))
}

func victimRelationship(p *Provider, victim model.Answers) any {
	return p.When(victimAnswer(p, victim, FieldOffenceAnalysisVictimRelationship),
		On("0", ValueStranger), On("12", ValueCriminalJusticeStaff), On("14", ValuePopParentOrStepParent),
		On("15", ValuePopExPartner), On("5", ValuePopChildOrStepChild), On("6", ValueOtherFamilyMember),
		On("1", ValuePopPartner), On("13", ValueOther))
}

// yesNoIfAnswered is YES or NO, without validating the options when the field is unanswered.
func yesNoIfAnswered(field Field) func(*Provider) any {
	return func(p *Provider) any {
		answer := p.Value(field)
		if answer == nil {
			return nil
		}
		return p.When(answer, On("YES", ValueYes), On("NO", ValueNo))
	}
}

// detailsFor is the yes or no details for a yes/no field.
func detailsFor(field, yesDetails, noDetails Field) func(*Provider) any {
	return func(p *Provider) any {
		return p.When(p.Value(field),
			OnFunc(func() any { return optional(p.Value(yesDetails)) }, ValueYes),
			OnFunc(func() any { return optional(p.Value(noDetails)) }, ValueNo))
	}
}

func offenceAnalysisQ3(p *Provider) any {
	var answers []string
	elements, _ := p.Values(FieldOffenceAnalysisElements)
	for _, element := range elements {
		if mapped := p.When(&element, On("DIRECTCONT", ValueVictimTargeted), On("HATE", ValueHatredOfIdentifiableGroups)); mapped != nil {
			answers = append(answers, mapped.(string))
		}
	}
	for _, victim := range p.Collection(CollectionOffenceAnalysisVictim) {
		if p.Is(victimAnswer(p, victim, FieldOffenceAnalysisVictimRelationship), ValueStranger) {
			answers = append(answers, "STRANGERS")
			break
		}
	}
	if len(answers) == 0 {
		return nil
	}
	return strings.Join(answers, ",")
}

func relationships(*Provider) []Question {
	analysis := practitionerAnalysis("PERSONAL_RELATIONSHIPS_COMMUNITY")
	scored := func(field Field, arms ...Arm) func(*Provider) any {
		return func(p *Provider) any { return p.When(p.Value(field), arms...) }
	}
	return append(q(
		"o6-1", func(p *Provider) any {
			answer := p.Value(FieldPersonalRelationshipsCommunityFamilyRelationship)
			if answer == nil {
				return nil
			}
			return p.When(answer,
				On("2", ValueUnstableRelationship), On("1", ValueMixedRelationship), On("0", ValueStableRelationship), Else("M"))
		},
		"o6-3", scored(FieldPersonalRelationshipsCommunityChildhood,
			On("0", ValuePositiveChildhood), On("1", ValueMixedChildhood), On("2", ValueNegativeChildhood)),
		"o6-4", scored(FieldPersonalRelationshipsCommunityCurrentRelationship,
			On("0", ValueHappyRelationship), On("1", ValueConcernsHappyRelationship), On("2", ValueUnhappyRelationship)),
		"o6-6", scored(FieldPersonalRelationshipsCommunityIntimateRelationship,
			On("0", ValueStableRelationships), On("1", ValuePositiveAndNegativeRelationships), On("2", ValueUnstableRelationships)),
		"o6-7da", func(p *Provider) any {
			perpetrator := yesNo(FieldOffenceAnalysisPerpetratorOfDomesticAbuse)(p)
			victim := yesNo(FieldOffenceAnalysisVictimOfDomesticAbuse)(p)
			switch {
			case perpetrator == "YES" || victim == "YES":
				return "YES"
			case perpetrator == "NO" && victim == "NO":
				return "NO"
			}
			return nil
		},
		"o6-7-1-1da", domesticAbuseBy(FieldOffenceAnalysisVictimOfDomesticAbuse, FieldOffenceAnalysisVictimOfDomesticAbuseType, ValueIntimatePartner, ValueFamilyMember),
		"o6-7-1-2da", domesticAbuseBy(FieldOffenceAnalysisVictimOfDomesticAbuse, FieldOffenceAnalysisVictimOfDomesticAbuseType, ValueFamilyMember, ValueIntimatePartner),
		"o6-7-2-1da", domesticAbuseBy(FieldOffenceAnalysisPerpetratorOfDomesticAbuse, FieldOffenceAnalysisPerpetratorOfDomesticAbuseType, ValueIntimatePartner, ValueFamilyMember),
		"o6-7-2-2da", domesticAbuseBy(FieldOffenceAnalysisPerpetratorOfDomesticAbuse, FieldOffenceAnalysisPerpetratorOfDomesticAbuseType, ValueFamilyMember, ValueIntimatePartner),
		"o6-8", func(p *Provider) any {
			if livingWith, answered := p.Values(FieldLivingWith); answered && p.Contains(livingWith, ValuePartner) {
				return "1"
			}
			if people, answered := p.Values(FieldPersonalRelationshipsCommunityImportantPeople); answered && p.Contains(people, ValuePartnerIntimateRelationship) {
				return "2"
			}
			return "3"
		},
		"o6-9", func(p *Provider) any {
			people, answered := p.Values(FieldPersonalRelationshipsCommunityImportantPeople)
			switch {
			case !answered:
				return nil
			case p.Contains(people, ValueChildParentalResponsibilities):
				return "YES"
			}
			return "NO"
		},
		"o6-10", scored(FieldPersonalRelationshipsCommunityParentalResponsibilities,
			On("Significantproblems", ValueNo), On("Someproblems", ValueSometimes), On("Noproblems", ValueYes)),
		"o6-11", yesNo(FieldThinkingBehavioursAttitudesRiskSexualHarm),
		"o6-12", scored(FieldThinkingBehavioursAttitudesEmotionalIntimacy, On("2", ValueYes), On("1", ValueSometimes), On("0", ValueNo)),
	), analysis.questions("o6", true)...)
}

// domesticAbuseBy is whether domestic abuse involved the given relationship, when there was some.
func domesticAbuseBy(abuse, abuseType Field, included, excluded Value) func(*Provider) any {
	return func(p *Provider) any {
		return p.When(p.Value(abuse),
			OnFunc(func() any {
				return p.When(p.Value(abuseType),
					On("YES", included, ValueFamilyMemberAndIntimatePartner),
					On("NO", excluded))
			}, ValueYes),
			On("NO", ValueNo))
	}
}
