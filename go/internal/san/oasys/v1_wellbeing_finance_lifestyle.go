package oasys

import . "github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/oasys/codes" //nolint:revive // reads like the Kotlin mapping

func emotionalWellbeing(*Provider) []Question {
	analysis := practitionerAnalysis("HEALTH_WELLBEING")
	// Questions 2 and 6 are both answered from the mental health condition.
	mentalHealthScore := func(p *Provider) any {
		return p.When(p.Value(FieldHealthWellbeingMentalHealthCondition),
			On("2", ValueYesOngoingSevere), On("1", ValueYesOngoing, ValueYesInThePast), On("0", ValueNo))
	}
	return append(q(
		"o10-1", func(p *Provider) any {
			return p.When(p.Value(FieldHealthWellbeingCopingDayToDayLife),
				On("0", ValueYes), On("1", ValueYesSomeDifficulties), On("2", ValueNo))
		},
		"o10-2", mentalHealthScore,
		"o10-4", func(p *Provider) any {
			return p.When(p.Value(FieldHealthWellbeingAttitudeTowardsSelf),
				On("0", ValuePositive), On("1", ValueSomeNegativeAspects), On("2", ValueNegative))
		},
		"o10-5", func(p *Provider) any {
			answers := []any{
				yesNo(FieldHealthWellbeingAttemptedSuicideOrSuicidalThoughts)(p),
				yesNo(FieldHealthWellbeingSelfHarmed)(p),
			}
			switch {
			case anyOf(answers, func(answer any) bool { return answer == "YES" }):
				return "YES"
			case anyOf(answers, func(answer any) bool { return answer == "NO" }):
				return "NO"
			}
			return nil
		},
		"o10-6", mentalHealthScore,
		"o10-7_V2_CHILDHOOD", yesNo(FieldPersonalRelationshipsCommunityChildhoodBehaviour),
		"o10-7_V2_HISTHEADINJ", yesNo(FieldHealthWellbeingHeadInjuryOrIllness),
		"o10-7_V2_HISTPSYCH", func(p *Provider) any {
			return p.When(p.Value(FieldHealthWellbeingMentalHealthCondition),
				On("YES", ValueYesOngoingSevere, ValueYesOngoing, ValueYesInThePast),
				On("NO", ValueNo, ValueUnknown))
		},
		"o10-7_V2_PSYCHTREAT", func(p *Provider) any {
			return p.When(p.Value(FieldHealthWellbeingMentalHealthCondition),
				On("NO", ValueNo, ValueUnknown),
				ElseFunc(func() any {
					return p.When(p.Value(FieldHealthWellbeingPsychiatricTreatment),
						On("YES", ValueYes, ValuePendingTreatment), On("NO", ValueNo, ValueUnknown))
				}))
		},
	), analysis.questions("o10", true)...)
}

func financialManagement(*Provider) []Question {
	analysis := practitionerAnalysis("FINANCE")
	return append(q(
		"o5-3", func(p *Provider) any {
			return p.When(p.Value(FieldFinanceMoneyManagement),
				On("0", ValueGood, ValueFairlyGood), On("1", ValueFairlyBad), On("2", ValueBad))
		},
		"o5-4", func(p *Provider) any {
			income, answered := p.Values(FieldFinanceIncome)
			switch {
			case !answered:
				return nil
			case p.Contains(income, ValueOffending):
				if len(income) == 1 {
					return "2"
				}
				return "1"
			case p.Contains(income, ValueUnknown):
				return "M"
			}
			return "0"
		},
		"o5-5", func(p *Provider) any {
			income, answered := p.Values(FieldFinanceIncome)
			if !answered {
				return nil
			}
			if p.Contains(income, ValueFamilyOrFriends) {
				return p.When(p.Value(FieldFamilyOrFriendsDetails), On("2", ValueYes), On("M", ValueUnknown), Else("0"))
			}
			return "0"
		},
		"o5-6", func(p *Provider) any {
			return p.When(p.Value(FieldFinanceMoneyManagement), On("0", ValueGood, ValueFairlyGood))
		},
	), analysis.questions("o5", true)...)
}

func lifestyleAssociates(*Provider) []Question {
	analysis := practitionerAnalysis("THINKING_BEHAVIOURS_ATTITUDES")
	return append(q(
		"o7-2", func(p *Provider) any {
			return p.When(p.Value(FieldThinkingBehavioursAttitudesOffendingActivities),
				On("0", ValueNoOffendingActivities), On("1", ValueSometimesOffendingActivities), On("2", ValueYesOffendingActivities))
		},
		"o7-3", func(p *Provider) any {
			return p.When(p.Value(FieldThinkingBehavioursAttitudesPeerPressure),
				On("0", ValueYes), On("1", ValueSome), On("2", ValueNo))
		},
		"o7-4", func(p *Provider) any {
			return p.When(p.Value(FieldThinkingBehavioursAttitudesManipulativePredatoryBehaviour),
				On("2", ValueYes), On("1", ValueSome), On("0", ValueNo))
		},
		"o7-5", func(p *Provider) any {
			return p.When(p.Value(FieldThinkingBehavioursAttitudesStableBehaviour),
				On("0", ValueYes), On("1", ValueSometimes), On("2", ValueNo))
		},
	), analysis.questions("o7", false)...)
}

func thinkingBehaviours(*Provider) []Question {
	analysis := practitionerAnalysis("THINKING_BEHAVIOURS_ATTITUDES")
	scored := func(field Field, arms ...Arm) func(*Provider) any {
		return func(p *Provider) any { return p.When(p.Value(field), arms...) }
	}
	return append(q(
		"o11-2", scored(FieldThinkingBehavioursAttitudesImpulsiveBehaviour, On("0", ValueNo), On("1", ValueSometimes), On("2", ValueYes)),
		"o11-3", scored(FieldThinkingBehavioursAttitudesViolenceControllingBehaviour, On("0", ValueNoViolence), On("1", ValueSometimes), On("2", ValueYesViolence)),
		"o11-4", scored(FieldThinkingBehavioursAttitudesTemperManagement, On("0", ValueYes), On("1", ValueSometimes), On("2", ValueNo)),
		"o11-6", scored(FieldThinkingBehavioursAttitudesProblemSolving, On("0", ValueYes), On("1", ValueLimitedProblemSolving), On("2", ValueNo)),
		"o11-7", scored(FieldThinkingBehavioursAttitudesConsequences, On("0", ValueYes), On("1", ValueSometimes), On("2", ValueNo)),
		"o11-9", scored(FieldThinkingBehavioursAttitudesPeoplesViews, On("0", ValueYes), On("1", ValueSometimes), On("2", ValueNo)),
		"o11-11", scored(FieldThinkingBehavioursAttitudesSexualPreoccupation, On("2", ValueYes), On("1", ValueSometimes), On("0", ValueNo)),
		"o11-12", scored(FieldThinkingBehavioursAttitudesOffenceRelatedSexualInterest,
			On("2", ValueYesOffenceRelatedSexualInterest), On("1", ValueSomeOffenceRelatedSexualInterest), On("0", ValueNoOffenceRelatedSexualInterest)),
	), analysis.questions("o11", false)...)
}

func predictors(*Provider) []Question { return nil }

func newSections(*Provider) []Question {
	analysis := practitionerAnalysis("THINKING_BEHAVIOURS_ATTITUDES")
	complete := func(field Field) func(*Provider) any {
		return func(p *Provider) any {
			if p.Is(p.Value(field), ValueYes) {
				return "YES"
			}
			return "NO"
		}
	}
	return q(
		"o1-30", func(p *Provider) any {
			elements, _ := p.Values(FieldOffenceAnalysisElements)
			sexualElement := p.Contains(elements, ValueSexualElement)
			motivations, _ := p.Values(FieldOffenceAnalysisMotivations)
			sexualMotivation := p.Contains(motivations, ValueSexualMotivation)
			if sexualElement || sexualMotivation {
				return "YES"
			}
			return nil
		},
		"oTBA_SAN_LINKED_ROSH", analysis.riskOfSeriousHarm,
		"oTBA_SAN_LINKED_REOFFEND", analysis.riskOfReoffending,
		"oTBA_SAN_STRENGTH", analysis.strengthsOrProtectiveFactors,
		"oAC_SAN_SECTION_COMP", complete(FieldAccommodationSectionComplete),
		"oEE_SAN_SECTION_COMP", complete(FieldEmploymentEducationSectionComplete),
		"oFI_SAN_SECTION_COMP", complete(FieldFinanceSectionComplete),
		"oSMD_SAN_SECTION_COMP", complete(FieldDrugUseSectionComplete),
		"oSMA_SAN_SECTION_COMP", complete(FieldAlcoholUseSectionComplete),
		"oHW_SAN_SECTION_COMP", complete(FieldHealthWellbeingSectionComplete),
		"oPRC_SAN_SECTION_COMP", complete(FieldPersonalRelationshipsCommunitySectionComplete),
		"oTBA_SAN_SECTION_COMP", complete(FieldThinkingBehavioursAttitudesSectionComplete),
		"oOA_SAN_SECTION_COMP", complete(FieldOffenceAnalysisSectionComplete),
	)
}
