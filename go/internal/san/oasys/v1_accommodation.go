package oasys

import . "github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/oasys/codes" //nolint:revive // reads like the Kotlin mapping

func accommodation(*Provider) []Question {
	analysis := practitionerAnalysis("ACCOMMODATION")
	return append(q(
		"o3-3", func(p *Provider) any {
			return p.When(p.Value(FieldCurrentAccommodation),
				On("YES", ValueNoAccommodation),
				On("NO", ValueTemporary, ValueSettled))
		},
		"o3-4", func(p *Provider) any {
			if p.Is(p.Value(FieldCurrentAccommodation), ValueNoAccommodation) {
				return "2"
			}
			return p.When(p.Value(FieldSuitableHousing),
				On("0", ValueYes), On("1", ValueYesWithConcerns), On("2", ValueNo))
		},
		"o3-5", func(p *Provider) any {
			return p.When(p.Value(FieldCurrentAccommodation),
				On("0", ValueSettled),
				On("2", ValueNoAccommodation),
				OnFunc(func() any {
					return p.When(p.Value(FieldTypeOfTemporaryAccommodation), On("2", ValueShortTerm))
				}, ValueTemporary))
		},
		"o3-6", func(p *Provider) any {
			return p.When(p.Value(FieldCurrentAccommodation),
				On("2", ValueNoAccommodation),
				ElseFunc(func() any {
					return p.When(p.Value(FieldSuitableHousingLocation), On("0", ValueYes), On("2", ValueNo))
				}))
		},
	), analysis.questions("o3", true)...)
}
