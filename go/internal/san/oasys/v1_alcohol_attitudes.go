package oasys

import . "github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/oasys/codes" //nolint:revive // reads like the Kotlin mapping

func alcoholMisuse(*Provider) []Question {
	analysis := practitionerAnalysis("ALCOHOL_USE")
	return append(q(
		"o9-1", func(p *Provider) any {
			return p.When(p.Value(FieldAlcoholUse),
				OnFunc(func() any { return alcoholScore(unitsScore(p, frequencyScore(p, 0))) }, ValueYesWithinLastThreeMonths),
				On("0", ValueNo, ValueYesNotInLastThreeMonths))
		},
		"o9-1-t", func(p *Provider) any {
			return p.When(p.Value(FieldAlcoholUse), OnFunc(func() any {
				frequency := p.When(p.Value(FieldAlcoholFrequency),
					On("Only drinks once a month or less", ValueOnceAMonthOrLess),
					On("Drinks multiple times a month", ValueMultipleTimesAMonth),
					On("Drinks less than four times a week", ValueLessThan4TimesAWeek),
					On("Drinks more than four times a week", ValueMoreThan4TimesAWeek))
				units := p.When(p.Value(FieldAlcoholUnits),
					On("consumes 1 to 2 units a day, when they drink", ValueUnits1To2),
					On("consumes 3 to 4 units a day, when they drink", ValueUnits3To4),
					On("consumes 5 to 6 units a day, when they drink", ValueUnits5To6),
					On("consumes 7 to 9 units a day, when they drink", ValueUnits7To9),
					On("consumes 10 or more units a day, when they drink", ValueUnits10OrMore))
				if frequency == nil || units == nil {
					return nil
				}
				return frequency.(string) + " and " + units.(string) + "."
			}, ValueYesWithinLastThreeMonths))
		},
		"o9-2", func(p *Provider) any {
			return p.When(p.Value(FieldAlcoholUse),
				OnFunc(func() any {
					return p.When(p.Value(FieldAlcoholEvidenceOfExcessDrinking),
						On("0", ValueNoEvidence), On("1", ValueYesWithSomeEvidence), On("2", ValueYesWithEvidence))
				}, ValueYesWithinLastThreeMonths, ValueYesNotInLastThreeMonths),
				On("0", ValueNo))
		},
	), analysis.questions("o9", true)...)
}

func alcoholScore(total int) any {
	switch {
	case total >= 8:
		return "2"
	case total >= 5:
		return "1"
	case total >= 0:
		return "0"
	}
	return nil
}

func unitsScore(p *Provider, total int) int {
	return total + p.When(p.Value(FieldAlcoholUnits),
		On(1, ValueUnits3To4), On(2, ValueUnits5To6), On(3, ValueUnits7To9), On(4, ValueUnits10OrMore), Else(0)).(int)
}

func frequencyScore(p *Provider, total int) int {
	return total + p.When(p.Value(FieldAlcoholFrequency),
		On(1, ValueMultipleTimesAMonth), On(3, ValueLessThan4TimesAWeek), On(4, ValueMoreThan4TimesAWeek), Else(0)).(int)
}

func attitudes(*Provider) []Question {
	analysis := practitionerAnalysis("THINKING_BEHAVIOURS_ATTITUDES")
	return append(q(
		"o12-1", func(p *Provider) any {
			return p.When(p.Value(FieldThinkingBehavioursAttitudesCriminalBehaviour),
				On("0", ValueNo), On("1", ValueSometimes), On("2", ValueYes))
		},
		"o12-3", func(p *Provider) any {
			return p.When(p.Value(FieldThinkingBehavioursAttitudesPositiveAttitude),
				On("0", ValueYesPositive), On("1", ValueNegativeAttitudeNoConcerns), On("2", ValueNegativeAttitudeAndConcerns))
		},
		"o12-4", func(p *Provider) any {
			return p.When(p.Value(FieldThinkingBehavioursAttitudesSupervision),
				On("0", ValueYesSupervision), On("1", ValueUnsureSupervision), On("2", ValueNoSupervision))
		},
		"o12-9", func(p *Provider) any {
			return p.When(p.Value(FieldThinkingBehavioursAttitudesHostileOrientation),
				On("0", ValueNo), On("1", ValueSome), On("2", ValueYes))
		},
	), analysis.questions("o12", false)...)
}
