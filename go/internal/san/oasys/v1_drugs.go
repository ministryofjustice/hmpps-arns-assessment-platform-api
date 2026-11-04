package oasys

import . "github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/san/oasys/codes" //nolint:revive // reads like the Kotlin mapping

// drug is one drug's fields: how often it was used in the last six months, when it was last
// used, and when it was injected (for drugs that can be).
type drug struct {
	frequency, lastUsed, injected Field
}

func drugs(*Provider) []Question {
	analysis := practitionerAnalysis("DRUG_USE")
	heroin := drug{FieldHowOftenUsedLastSixMonthsHeroin, FieldDrugLastUsedHeroin, FieldDrugsInjectedHeroin}
	methadone := drug{FieldHowOftenUsedLastSixMonthsMethadoneNotPrescribed, FieldDrugLastUsedMethadoneNotPrescribed, FieldDrugsInjectedMethadoneNotPrescribed}
	opiates := drug{FieldHowOftenUsedLastSixMonthsOtherOpiates, FieldDrugLastUsedOtherOpiates, FieldDrugsInjectedOtherOpiates}
	crack := drug{FieldHowOftenUsedLastSixMonthsCrack, FieldDrugLastUsedCrack, FieldDrugsInjectedCrack}
	cocaine := drug{FieldHowOftenUsedLastSixMonthsCocaine, FieldDrugLastUsedCocaine, FieldDrugsInjectedCocaine}
	prescribed := drug{FieldHowOftenUsedLastSixMonthsMisusedPrescribedDrugs, FieldDrugLastUsedMisusedPrescribedDrugs, FieldDrugsInjectedMisusedPrescribedDrugs}
	benzodiazepines := drug{FieldHowOftenUsedLastSixMonthsBenzodiazepines, FieldDrugLastUsedBenzodiazepines, FieldDrugsInjectedBenzodiazepines}
	amphetamines := drug{FieldHowOftenUsedLastSixMonthsAmphetamines, FieldDrugLastUsedAmphetamines, FieldDrugsInjectedAmphetamines}
	steroids := drug{FieldHowOftenUsedLastSixMonthsSteroids, FieldDrugLastUsedSteroids, FieldDrugsInjectedSteroids}
	other := drug{FieldHowOftenUsedLastSixMonthsOtherDrug, FieldDrugLastUsedOtherDrug, FieldDrugsInjectedOtherDrug}

	return append(q(
		"o8-1", yesNo(FieldDrugUse),
		"o8-2-1-1", usageFrequencyScore(heroin.frequency),
		"o8-2-1-3", yesIfMoreThanSix(heroin.lastUsed),
		"o8-2-1-2", yesIfContains(heroin.injected, ValueLastSix),
		"o8-2-1-4", yesIfContains(heroin.injected, ValueMoreThanSix),
		"o8-2-2-1", usageFrequencyScore(methadone.frequency),
		"o8-2-2-3", yesIfMoreThanSix(methadone.lastUsed),
		"o8-2-2-2", yesIfContains(methadone.injected, ValueLastSix),
		"o8-2-2-4", yesIfContains(methadone.injected, ValueMoreThanSix),
		"o8-2-3-1", usageFrequencyScore(opiates.frequency),
		"o8-2-3-3", yesIfMoreThanSix(opiates.lastUsed),
		"o8-2-3-2", yesIfContains(opiates.injected, ValueLastSix),
		"o8-2-3-4", yesIfContains(opiates.injected, ValueMoreThanSix),
		"o8-2-4-1", usageFrequencyScore(crack.frequency),
		"o8-2-4-2", yesIfContains(crack.injected, ValueLastSix),
		"o8-2-4-3", yesIfMoreThanSix(crack.lastUsed),
		"o8-2-4-4", yesIfContains(crack.injected, ValueMoreThanSix),
		"o8-2-5-1", usageFrequencyScore(cocaine.frequency),
		"o8-2-5-2", yesIfContains(cocaine.injected, ValueLastSix),
		"o8-2-5-3", yesIfMoreThanSix(cocaine.lastUsed),
		"o8-2-5-4", yesIfContains(cocaine.injected, ValueMoreThanSix),
		"o8-2-6-1", usageFrequencyScore(prescribed.frequency),
		"o8-2-6-2", yesIfContains(prescribed.injected, ValueLastSix),
		"o8-2-6-3", yesIfMoreThanSix(prescribed.lastUsed),
		"o8-2-6-4", yesIfContains(prescribed.injected, ValueMoreThanSix),
		"o8-2-7-1", usageFrequencyScore(benzodiazepines.frequency),
		"o8-2-7-3", yesIfMoreThanSix(benzodiazepines.lastUsed),
		"o8-2-7-2", yesIfContains(benzodiazepines.injected, ValueLastSix),
		"o8-2-7-4", yesIfContains(benzodiazepines.injected, ValueMoreThanSix),
		"o8-2-8-1", usageFrequencyScore(amphetamines.frequency),
		"o8-2-8-2", yesIfContains(amphetamines.injected, ValueLastSix),
		"o8-2-8-3", yesIfMoreThanSix(amphetamines.lastUsed),
		"o8-2-8-4", yesIfContains(amphetamines.injected, ValueMoreThanSix),
		"o8-2-9-1", usageFrequencyScore(FieldHowOftenUsedLastSixMonthsHallucinogenics),
		"o8-2-9-3", yesIfMoreThanSix(FieldDrugLastUsedHallucinogenics),
		"o8-2-10-1", usageFrequencyScore(FieldHowOftenUsedLastSixMonthsEcstasy),
		"o8-2-10-3", yesIfMoreThanSix(FieldDrugLastUsedEcstasy),
		"o8-2-11-1", usageFrequencyScore(FieldHowOftenUsedLastSixMonthsCannabis),
		"o8-2-11-3", yesIfMoreThanSix(FieldDrugLastUsedCannabis),
		"o8-2-12-1", usageFrequencyScore(FieldHowOftenUsedLastSixMonthsSolvents),
		"o8-2-12-3", yesIfMoreThanSix(FieldDrugLastUsedSolvents),
		"o8-2-13-1", usageFrequencyScore(steroids.frequency),
		"o8-2-13-2", yesIfContains(steroids.injected, ValueLastSix),
		"o8-2-13-3", yesIfMoreThanSix(steroids.lastUsed),
		"o8-2-13-4", yesIfContains(steroids.injected, ValueMoreThanSix),
		"o8-2-15-1", usageFrequencyScore(FieldHowOftenUsedLastSixMonthsSpice),
		"o8-2-15-3", yesIfMoreThanSix(FieldDrugLastUsedSpice),
		"o8-2-14-1", usageFrequencyScore(other.frequency),
		"o8-2-14-2", yesIfContains(other.injected, ValueLastSix),
		"o8-2-14-3", yesIfMoreThanSix(other.lastUsed),
		"o8-2-14-4", yesIfContains(other.injected, ValueMoreThanSix),
		"o8-2-14-t", valueOf(FieldOtherDrugName),
		"o8-4", drugsQ4,
		"o8-5", drugsQ5,
		"o8-6", drugsQ6,
		"o8-8", func(p *Provider) any {
			return p.When(p.Value(FieldDrugsPractitionerAnalysisMotivatedToStop),
				On("0", ValueFullMotivation), On("1", ValuePartialMotivation), On("2", ValueNoMotivation), On("M", ValueUnknown))
		},
	), analysis.questions("o8", true)...)
}

func usageFrequencyScore(field Field) func(*Provider) any {
	return func(p *Provider) any {
		return p.When(p.Value(field),
			On("100", ValueDaily), On("110", ValueWeekly), On("120", ValueMonthly), On("130", ValueOccasionally))
	}
}

func yesIfMoreThanSix(field Field) func(*Provider) any {
	return func(p *Provider) any {
		if isMoreThanSix(p, field) {
			return "YES"
		}
		return nil
	}
}

func yesIfContains(field Field, value Value) func(*Provider) any {
	return func(p *Provider) any {
		if values, answered := p.Values(field); answered && p.Contains(values, value) {
			return "YES"
		}
		return nil
	}
}

func isUsing(p *Provider, field Field, frequencies []Value) bool {
	usage := p.Value(field)
	if usage == nil {
		return false
	}
	// Every frequency is validated, not just up to the first match, as in Kotlin.
	options := make([]string, len(frequencies))
	for i, frequency := range frequencies {
		options[i] = p.Get(frequency)
	}
	return anyOf(options, func(option string) bool { return option == *usage })
}

func isMoreThanSix(p *Provider, field Field) bool { return p.Is(p.Value(field), ValueMoreThanSix) }
func isLastSix(p *Provider, field Field) bool     { return p.Is(p.Value(field), ValueLastSix) }

func containsMoreThanSix(p *Provider, field Field) bool {
	values, _ := p.Values(field)
	return p.Contains(values, ValueMoreThanSix)
}

func containsLastSix(p *Provider, field Field) bool {
	values, _ := p.Values(field)
	return p.Contains(values, ValueLastSix)
}

func hasInjected(p *Provider, drug Value) bool {
	values, _ := p.Values(FieldDrugsInjected)
	return p.Contains(values, drug)
}

func drugsQ4(p *Provider) any {
	frequencies := []Value{ValueDaily, ValueWeekly, ValueMonthly, ValueOccasionally}
	usage := []Field{
		FieldHowOftenUsedLastSixMonthsHeroin, FieldHowOftenUsedLastSixMonthsMethadoneNotPrescribed,
		FieldHowOftenUsedLastSixMonthsOtherOpiates, FieldHowOftenUsedLastSixMonthsCrack,
		FieldHowOftenUsedLastSixMonthsCocaine, FieldHowOftenUsedLastSixMonthsMisusedPrescribedDrugs,
	}
	if anyOf(usage, func(field Field) bool { return isUsing(p, field, frequencies) }) {
		return "2"
	}
	return "0"
}

func drugsQ5(p *Provider) any {
	usage := []Field{
		FieldHowOftenUsedLastSixMonthsHeroin, FieldHowOftenUsedLastSixMonthsMethadoneNotPrescribed,
		FieldHowOftenUsedLastSixMonthsOtherOpiates, FieldHowOftenUsedLastSixMonthsCrack,
		FieldHowOftenUsedLastSixMonthsCocaine, FieldHowOftenUsedLastSixMonthsMisusedPrescribedDrugs,
		FieldHowOftenUsedLastSixMonthsBenzodiazepines, FieldHowOftenUsedLastSixMonthsAmphetamines,
		FieldHowOftenUsedLastSixMonthsHallucinogenics, FieldHowOftenUsedLastSixMonthsEcstasy,
		FieldHowOftenUsedLastSixMonthsCannabis, FieldHowOftenUsedLastSixMonthsSolvents,
		FieldHowOftenUsedLastSixMonthsSteroids, FieldHowOftenUsedLastSixMonthsSpice,
		FieldHowOftenUsedLastSixMonthsOtherDrug,
	}
	lastUsed := []Field{
		FieldDrugLastUsedAmphetamines, FieldDrugLastUsedBenzodiazepines, FieldDrugLastUsedCannabis,
		FieldDrugLastUsedCocaine, FieldDrugLastUsedCrack, FieldDrugLastUsedEcstasy,
		FieldDrugLastUsedHallucinogenics, FieldDrugLastUsedHeroin, FieldDrugLastUsedMethadoneNotPrescribed,
		FieldDrugLastUsedMisusedPrescribedDrugs, FieldDrugLastUsedOtherOpiates, FieldDrugLastUsedSolvents,
		FieldDrugLastUsedSteroids, FieldDrugLastUsedSpice, FieldDrugLastUsedOtherDrug,
	}
	switch {
	case anyOf(usage, func(field Field) bool { return isUsing(p, field, []Value{ValueDaily, ValueWeekly}) }):
		return "2"
	case anyOf(usage, func(field Field) bool { return isUsing(p, field, []Value{ValueMonthly, ValueOccasionally}) }):
		return "0"
	case anyOf(lastUsed, func(field Field) bool { return isLastSix(p, field) }):
		return "M"
	case anyOf(lastUsed, func(field Field) bool { return isMoreThanSix(p, field) }):
		return "0"
	}
	return nil
}

func drugsQ6(p *Provider) any {
	injected := []Field{
		FieldDrugsInjectedHeroin, FieldDrugsInjectedMethadoneNotPrescribed, FieldDrugsInjectedOtherOpiates,
		FieldDrugsInjectedCrack, FieldDrugsInjectedCocaine, FieldDrugsInjectedMisusedPrescribedDrugs,
		FieldDrugsInjectedBenzodiazepines, FieldDrugsInjectedAmphetamines, FieldDrugsInjectedSteroids,
		FieldDrugsInjectedOtherDrug,
	}
	injectable := []Value{
		ValueAmphetamines, ValueBenzodiazepines, ValueCocaine, ValueCrack, ValueHeroin,
		ValueMethadoneNotPrescribed, ValueMisusedPrescribedDrugs, ValueOtherOpiates, ValueSteroids, ValueOtherDrug,
	}
	switch {
	case anyOf(injected, func(field Field) bool { return containsLastSix(p, field) }):
		return "2"
	case anyOf(injected, func(field Field) bool { return containsMoreThanSix(p, field) }) ||
		anyOf(injectable, func(drug Value) bool { return hasInjected(p, drug) }):
		return "1"
	}
	return "0"
}
