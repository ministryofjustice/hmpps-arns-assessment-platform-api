package oasys

import (
	"strconv"

	. "github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/oasys/codes" //nolint:revive // reads like the Kotlin mapping
)

func education(*Provider) []Question {
	analysis := practitionerAnalysis("EMPLOYMENT_EDUCATION")
	levels2To8 := []Value{ValueLevel2, ValueLevel3, ValueLevel4, ValueLevel5, ValueLevel6, ValueLevel7, ValueLevel8}

	return q(
		"o4-2", func(p *Provider) any {
			return p.When(p.Value(FieldEmploymentStatus),
				On("YES", ValueUnemployedLookingForWork, ValueUnemployedNotLookingForWork),
				On("NO", ValueEmployed, ValueSelfEmployed),
				On("NA", ValueRetired, ValueCurrentlyUnavailableForWork))
		},
		"o4-3", func(p *Provider) any {
			return p.When(p.Value(FieldEmploymentStatus),
				OnFunc(func() any {
					return p.When(p.Value(FieldHasBeenEmployed), On("2", ValueNo), ElseFunc(func() any { return employmentHistory(p) }))
				}, ValueUnemployedNotLookingForWork, ValueUnemployedLookingForWork),
				ElseFunc(func() any { return employmentHistory(p) }))
		},
		"o4-4", func(p *Provider) any {
			return p.When(p.Value(FieldEducationTransferableSkills),
				On("2", ValueNo), On("1", ValueYesSomeSkills), On("0", ValueYes))
		},
		"o4-7", func(p *Provider) any {
			difficulties, _ := p.Values(FieldEducationDifficulties)
			// The categories' options are validated up front, as they were keys of a map in Kotlin.
			for _, category := range []Value{ValueReading, ValueWriting, ValueNumeracy} {
				p.Get(category)
			}
			switch {
			case p.Contains(difficulties, ValueNone):
				return "0"
			case len(difficulties) > 0:
				highest := -1
				for _, field := range []Field{
					FieldEducationDifficultiesReadingSeverity,
					FieldEducationDifficultiesWritingSeverity,
					FieldEducationDifficultiesNumeracySeverity,
				} {
					if severity := p.When(p.Value(field), On(2, ValueSignificantDifficulties), On(1, ValueSomeDifficulties)); severity != nil {
						highest = max(highest, severity.(int))
					}
				}
				if highest == -1 {
					return nil
				}
				return strconv.Itoa(highest)
			}
			return nil
		},
		"o4-7-1", func(p *Provider) any {
			difficulties, answered := p.Values(FieldEducationDifficulties)
			if !answered || len(difficulties) == 0 || p.Contains(difficulties, ValueNone) {
				return nil
			}
			return difficulties
		},
		"o4-8", func(p *Provider) any {
			return p.When(p.Value(FieldHealthWellbeingLearningDifficulties),
				On("2", ValueYesSignificantDifficulties), On("1", ValueYesSomeDifficulties), On("0", ValueNo))
		},
		"o4-9", func(p *Provider) any {
			highestLevel := p.Value(FieldEducationHighestLevelCompleted)
			hasQualifications := func() any {
				return p.When(p.Value(FieldEducationProfessionalOrVocationalQualifications), On("0", ValueYes))
			}
			return p.When(highestLevel,
				OnFunc(func() any {
					return p.When(p.Value(FieldEducationProfessionalOrVocationalQualifications), On("0", ValueYes), On("2", ValueNo))
				}, ValueLevel1, ValueEntryLevel, ValueNoneOfThese),
				On("0", levels2To8...),
				OnFunc(hasQualifications, ValueNotSure),
				ElseFunc(func() any {
					if highestLevel == nil {
						return hasQualifications()
					}
					return nil
				}))
		},
		"o4-10", func(p *Provider) any {
			return p.When(p.Value(FieldEducationExperience),
				On("0", ValuePositive, ValueMostlyPositive),
				On("1", ValuePositiveAndNegative),
				On("2", ValueNegative, ValueMostlyNegative))
		},
		"o4-94", analysis.notes,
		"o4-96", analysis.riskOfSeriousHarm,
		"o4-98", analysis.riskOfReoffending,
		"o4_SAN_STRENGTH", analysis.strengthsOrProtectiveFactors,
		"oSC2", yesNo(FieldEducationProfessionalOrVocationalQualifications),
		"oSC2-t", valueOf(FieldEducationProfessionalOrVocationalQualificationsYesDetails),
		"oSC3", func(p *Provider) any {
			return p.When(p.Value(FieldEducationHighestLevelCompleted),
				On("ANYOTHER", ValueEntryLevel),
				On("MATHSENGLISH", append([]Value{ValueLevel1}, levels2To8...)...),
				On("NOQUAL", ValueNoneOfThese))
		},
		"oSC4", func(p *Provider) any {
			return p.When(p.Value(FieldEmploymentStatus),
				On("FULLTIME", ValueRetired),
				OnFunc(func() any {
					return p.When(p.Value(FieldEmploymentType),
						On("FULLTIME", ValueFullTime),
						On("PARTTIME", ValuePartTime, ValueTemporaryOrCasual, ValueApprenticeship))
				}, ValueEmployed),
				OnFunc(func() any {
					return p.When(p.Value(FieldHasBeenEmployed), On("UNEMPLOYED", ValueNo))
				}, ValueCurrentlyUnavailableForWork, ValueUnemployedLookingForWork, ValueUnemployedNotLookingForWork))
		},
		"oSC5", func(p *Provider) any {
			return p.When(p.Value(FieldEmploymentStatus),
				On("YES", ValueEmployed, ValueSelfEmployed),
				On("NO", ValueUnemployedLookingForWork, ValueUnemployedNotLookingForWork))
		},
		"oSC8", func(p *Provider) any {
			return p.When(p.Value(FieldFinanceMoneyManagement),
				On("YES", ValueGood), On("SOMETIMES", ValueFairlyGood), On("NOTCONFIDENT", ValueFairlyBad, ValueBad))
		},
	)
}

func employmentHistory(p *Provider) any {
	return p.When(p.Value(FieldEmploymentHistory),
		On("0", ValueStable), On("1", ValuePeriodsOfInstability), On("2", ValueUnstable), On("M", ValueUnknown))
}
