// Code generated from the Kotlin service's Codes.kt. DO NOT EDIT.

// Package codes has the field, option and collection names the OASys mapping uses. The names
// must match the form config exactly.
package codes

import "strings"

// Field is a field in the form config, keyed there by its lowercased name.
type Field string

// Lower is the key of the field in the form config.
func (f Field) Lower() string { return strings.ToLower(string(f)) }

// Value is an option of a field in the form config.
type Value string

// Collection is a collection in the form config.
type Collection string

const (
	CollectionOffenceAnalysisVictim Collection = "OFFENCE_ANALYSIS_VICTIM"
)

const (
	FieldAccommodationPractitionerAnalysisRiskOfReoffending                                       Field = "ACCOMMODATION_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING"
	FieldAccommodationPractitionerAnalysisRiskOfReoffendingNoDetails                              Field = "ACCOMMODATION_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_NO_DETAILS"
	FieldAccommodationPractitionerAnalysisRiskOfReoffendingYesDetails                             Field = "ACCOMMODATION_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_YES_DETAILS"
	FieldAccommodationPractitionerAnalysisRiskOfSeriousHarm                                       Field = "ACCOMMODATION_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM"
	FieldAccommodationPractitionerAnalysisRiskOfSeriousHarmNoDetails                              Field = "ACCOMMODATION_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_NO_DETAILS"
	FieldAccommodationPractitionerAnalysisRiskOfSeriousHarmYesDetails                             Field = "ACCOMMODATION_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_YES_DETAILS"
	FieldAccommodationPractitionerAnalysisStrengthsOrProtectiveFactors                            Field = "ACCOMMODATION_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS"
	FieldAccommodationPractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails                   Field = "ACCOMMODATION_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_NO_DETAILS"
	FieldAccommodationPractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails                  Field = "ACCOMMODATION_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_YES_DETAILS"
	FieldAccommodationSectionComplete                                                             Field = "ACCOMMODATION_SECTION_COMPLETE"
	FieldAlcoholEvidenceOfExcessDrinking                                                          Field = "ALCOHOL_EVIDENCE_OF_EXCESS_DRINKING"
	FieldAlcoholFrequency                                                                         Field = "ALCOHOL_FREQUENCY"
	FieldAlcoholUnits                                                                             Field = "ALCOHOL_UNITS"
	FieldAlcoholUse                                                                               Field = "ALCOHOL_USE"
	FieldAlcoholUsePractitionerAnalysisRiskOfReoffending                                          Field = "ALCOHOL_USE_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING"
	FieldAlcoholUsePractitionerAnalysisRiskOfReoffendingNoDetails                                 Field = "ALCOHOL_USE_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_NO_DETAILS"
	FieldAlcoholUsePractitionerAnalysisRiskOfReoffendingYesDetails                                Field = "ALCOHOL_USE_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_YES_DETAILS"
	FieldAlcoholUsePractitionerAnalysisRiskOfSeriousHarm                                          Field = "ALCOHOL_USE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM"
	FieldAlcoholUsePractitionerAnalysisRiskOfSeriousHarmNoDetails                                 Field = "ALCOHOL_USE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_NO_DETAILS"
	FieldAlcoholUsePractitionerAnalysisRiskOfSeriousHarmYesDetails                                Field = "ALCOHOL_USE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_YES_DETAILS"
	FieldAlcoholUsePractitionerAnalysisStrengthsOrProtectiveFactors                               Field = "ALCOHOL_USE_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS"
	FieldAlcoholUsePractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails                      Field = "ALCOHOL_USE_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_NO_DETAILS"
	FieldAlcoholUsePractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails                     Field = "ALCOHOL_USE_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_YES_DETAILS"
	FieldAlcoholUseSectionComplete                                                                Field = "ALCOHOL_USE_SECTION_COMPLETE"
	FieldCurrentAccommodation                                                                     Field = "CURRENT_ACCOMMODATION"
	FieldDrugLastUsedAmphetamines                                                                 Field = "DRUG_LAST_USED_AMPHETAMINES"
	FieldDrugLastUsedBenzodiazepines                                                              Field = "DRUG_LAST_USED_BENZODIAZEPINES"
	FieldDrugLastUsedCannabis                                                                     Field = "DRUG_LAST_USED_CANNABIS"
	FieldDrugLastUsedCocaine                                                                      Field = "DRUG_LAST_USED_COCAINE"
	FieldDrugLastUsedCrack                                                                        Field = "DRUG_LAST_USED_CRACK"
	FieldDrugLastUsedEcstasy                                                                      Field = "DRUG_LAST_USED_ECSTASY"
	FieldDrugLastUsedHallucinogenics                                                              Field = "DRUG_LAST_USED_HALLUCINOGENICS"
	FieldDrugLastUsedHeroin                                                                       Field = "DRUG_LAST_USED_HEROIN"
	FieldDrugLastUsedMethadoneNotPrescribed                                                       Field = "DRUG_LAST_USED_METHADONE_NOT_PRESCRIBED"
	FieldDrugLastUsedMisusedPrescribedDrugs                                                       Field = "DRUG_LAST_USED_MISUSED_PRESCRIBED_DRUGS"
	FieldDrugLastUsedOtherDrug                                                                    Field = "DRUG_LAST_USED_OTHER_DRUG"
	FieldDrugLastUsedOtherOpiates                                                                 Field = "DRUG_LAST_USED_OTHER_OPIATES"
	FieldDrugLastUsedSolvents                                                                     Field = "DRUG_LAST_USED_SOLVENTS"
	FieldDrugLastUsedSpice                                                                        Field = "DRUG_LAST_USED_SPICE"
	FieldDrugLastUsedSteroids                                                                     Field = "DRUG_LAST_USED_STEROIDS"
	FieldDrugUsageAmphetamines                                                                    Field = "DRUG_USAGE_AMPHETAMINES"
	FieldDrugUsageBenzodiazepines                                                                 Field = "DRUG_USAGE_BENZODIAZEPINES"
	FieldDrugUsageCannabis                                                                        Field = "DRUG_USAGE_CANNABIS"
	FieldDrugUsageCocaine                                                                         Field = "DRUG_USAGE_COCAINE"
	FieldDrugUsageCrack                                                                           Field = "DRUG_USAGE_CRACK"
	FieldDrugUsageEcstasy                                                                         Field = "DRUG_USAGE_ECSTASY"
	FieldDrugUsageHallucinogenics                                                                 Field = "DRUG_USAGE_HALLUCINOGENICS"
	FieldDrugUsageHeroin                                                                          Field = "DRUG_USAGE_HEROIN"
	FieldDrugUsageMethadoneNotPrescribed                                                          Field = "DRUG_USAGE_METHADONE_NOT_PRESCRIBED"
	FieldDrugUsageMisusedPrescribedDrugs                                                          Field = "DRUG_USAGE_MISUSED_PRESCRIBED_DRUGS"
	FieldDrugUsageOtherDrug                                                                       Field = "DRUG_USAGE_OTHER_DRUG"
	FieldDrugUsageOtherOpiates                                                                    Field = "DRUG_USAGE_OTHER_OPIATES"
	FieldDrugUsageSolvents                                                                        Field = "DRUG_USAGE_SOLVENTS"
	FieldDrugUsageSpice                                                                           Field = "DRUG_USAGE_SPICE"
	FieldDrugUsageSteroids                                                                        Field = "DRUG_USAGE_STEROIDS"
	FieldDrugUse                                                                                  Field = "DRUG_USE"
	FieldDrugUsePractitionerAnalysisRiskOfReoffending                                             Field = "DRUG_USE_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING"
	FieldDrugUsePractitionerAnalysisRiskOfReoffendingNoDetails                                    Field = "DRUG_USE_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_NO_DETAILS"
	FieldDrugUsePractitionerAnalysisRiskOfReoffendingYesDetails                                   Field = "DRUG_USE_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_YES_DETAILS"
	FieldDrugUsePractitionerAnalysisRiskOfSeriousHarm                                             Field = "DRUG_USE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM"
	FieldDrugUsePractitionerAnalysisRiskOfSeriousHarmNoDetails                                    Field = "DRUG_USE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_NO_DETAILS"
	FieldDrugUsePractitionerAnalysisRiskOfSeriousHarmYesDetails                                   Field = "DRUG_USE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_YES_DETAILS"
	FieldDrugUsePractitionerAnalysisStrengthsOrProtectiveFactors                                  Field = "DRUG_USE_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS"
	FieldDrugUsePractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails                         Field = "DRUG_USE_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_NO_DETAILS"
	FieldDrugUsePractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails                        Field = "DRUG_USE_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_YES_DETAILS"
	FieldDrugUseSectionComplete                                                                   Field = "DRUG_USE_SECTION_COMPLETE"
	FieldDrugUseTypeOtherDrugDetails                                                              Field = "DRUG_USE_TYPE_OTHER_DRUG_DETAILS"
	FieldDrugsInjected                                                                            Field = "DRUGS_INJECTED"
	FieldDrugsInjectedAmphetamines                                                                Field = "DRUGS_INJECTED_AMPHETAMINES"
	FieldDrugsInjectedBenzodiazepines                                                             Field = "DRUGS_INJECTED_BENZODIAZEPINES"
	FieldDrugsInjectedCocaine                                                                     Field = "DRUGS_INJECTED_COCAINE"
	FieldDrugsInjectedCrack                                                                       Field = "DRUGS_INJECTED_CRACK"
	FieldDrugsInjectedHeroin                                                                      Field = "DRUGS_INJECTED_HEROIN"
	FieldDrugsInjectedMethadoneNotPrescribed                                                      Field = "DRUGS_INJECTED_METHADONE_NOT_PRESCRIBED"
	FieldDrugsInjectedMisusedPrescribedDrugs                                                      Field = "DRUGS_INJECTED_MISUSED_PRESCRIBED_DRUGS"
	FieldDrugsInjectedOtherDrug                                                                   Field = "DRUGS_INJECTED_OTHER_DRUG"
	FieldDrugsInjectedOtherOpiates                                                                Field = "DRUGS_INJECTED_OTHER_OPIATES"
	FieldDrugsInjectedSteroids                                                                    Field = "DRUGS_INJECTED_STEROIDS"
	FieldDrugsPractitionerAnalysisMotivatedToStop                                                 Field = "DRUGS_PRACTITIONER_ANALYSIS_MOTIVATED_TO_STOP"
	FieldEducationDifficulties                                                                    Field = "EDUCATION_DIFFICULTIES"
	FieldEducationDifficultiesNumeracySeverity                                                    Field = "EDUCATION_DIFFICULTIES_NUMERACY_SEVERITY"
	FieldEducationDifficultiesReadingSeverity                                                     Field = "EDUCATION_DIFFICULTIES_READING_SEVERITY"
	FieldEducationDifficultiesWritingSeverity                                                     Field = "EDUCATION_DIFFICULTIES_WRITING_SEVERITY"
	FieldEducationExperience                                                                      Field = "EDUCATION_EXPERIENCE"
	FieldEducationHighestLevelCompleted                                                           Field = "EDUCATION_HIGHEST_LEVEL_COMPLETED"
	FieldEducationProfessionalOrVocationalQualifications                                          Field = "EDUCATION_PROFESSIONAL_OR_VOCATIONAL_QUALIFICATIONS"
	FieldEducationProfessionalOrVocationalQualificationsYesDetails                                Field = "EDUCATION_PROFESSIONAL_OR_VOCATIONAL_QUALIFICATIONS_YES_DETAILS"
	FieldEducationTransferableSkills                                                              Field = "EDUCATION_TRANSFERABLE_SKILLS"
	FieldEmploymentEducationPractitionerAnalysisRiskOfReoffending                                 Field = "EMPLOYMENT_EDUCATION_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING"
	FieldEmploymentEducationPractitionerAnalysisRiskOfReoffendingNoDetails                        Field = "EMPLOYMENT_EDUCATION_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_NO_DETAILS"
	FieldEmploymentEducationPractitionerAnalysisRiskOfReoffendingYesDetails                       Field = "EMPLOYMENT_EDUCATION_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_YES_DETAILS"
	FieldEmploymentEducationPractitionerAnalysisRiskOfSeriousHarm                                 Field = "EMPLOYMENT_EDUCATION_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM"
	FieldEmploymentEducationPractitionerAnalysisRiskOfSeriousHarmNoDetails                        Field = "EMPLOYMENT_EDUCATION_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_NO_DETAILS"
	FieldEmploymentEducationPractitionerAnalysisRiskOfSeriousHarmYesDetails                       Field = "EMPLOYMENT_EDUCATION_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_YES_DETAILS"
	FieldEmploymentEducationPractitionerAnalysisStrengthsOrProtectiveFactors                      Field = "EMPLOYMENT_EDUCATION_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS"
	FieldEmploymentEducationPractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails             Field = "EMPLOYMENT_EDUCATION_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_NO_DETAILS"
	FieldEmploymentEducationPractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails            Field = "EMPLOYMENT_EDUCATION_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_YES_DETAILS"
	FieldEmploymentEducationSectionComplete                                                       Field = "EMPLOYMENT_EDUCATION_SECTION_COMPLETE"
	FieldEmploymentExperience                                                                     Field = "EMPLOYMENT_EXPERIENCE"
	FieldEmploymentHistory                                                                        Field = "EMPLOYMENT_HISTORY"
	FieldEmploymentStatus                                                                         Field = "EMPLOYMENT_STATUS"
	FieldEmploymentType                                                                           Field = "EMPLOYMENT_TYPE"
	FieldFamilyOrFriendsDetails                                                                   Field = "FAMILY_OR_FRIENDS_DETAILS"
	FieldFinanceIncome                                                                            Field = "FINANCE_INCOME"
	FieldFinanceMoneyManagement                                                                   Field = "FINANCE_MONEY_MANAGEMENT"
	FieldFinancePractitionerAnalysisRiskOfReoffending                                             Field = "FINANCE_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING"
	FieldFinancePractitionerAnalysisRiskOfReoffendingNoDetails                                    Field = "FINANCE_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_NO_DETAILS"
	FieldFinancePractitionerAnalysisRiskOfReoffendingYesDetails                                   Field = "FINANCE_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_YES_DETAILS"
	FieldFinancePractitionerAnalysisRiskOfSeriousHarm                                             Field = "FINANCE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM"
	FieldFinancePractitionerAnalysisRiskOfSeriousHarmNoDetails                                    Field = "FINANCE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_NO_DETAILS"
	FieldFinancePractitionerAnalysisRiskOfSeriousHarmYesDetails                                   Field = "FINANCE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_YES_DETAILS"
	FieldFinancePractitionerAnalysisStrengthsOrProtectiveFactors                                  Field = "FINANCE_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS"
	FieldFinancePractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails                         Field = "FINANCE_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_NO_DETAILS"
	FieldFinancePractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails                        Field = "FINANCE_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_YES_DETAILS"
	FieldFinanceSectionComplete                                                                   Field = "FINANCE_SECTION_COMPLETE"
	FieldHasBeenEmployed                                                                          Field = "HAS_BEEN_EMPLOYED"
	FieldHealthWellbeingAttemptedSuicideOrSuicidalThoughts                                        Field = "HEALTH_WELLBEING_ATTEMPTED_SUICIDE_OR_SUICIDAL_THOUGHTS"
	FieldHealthWellbeingAttitudeTowardsSelf                                                       Field = "HEALTH_WELLBEING_ATTITUDE_TOWARDS_SELF"
	FieldHealthWellbeingCopingDayToDayLife                                                        Field = "HEALTH_WELLBEING_COPING_DAY_TO_DAY_LIFE"
	FieldHealthWellbeingHeadInjuryOrIllness                                                       Field = "HEALTH_WELLBEING_HEAD_INJURY_OR_ILLNESS"
	FieldHealthWellbeingLearningDifficulties                                                      Field = "HEALTH_WELLBEING_LEARNING_DIFFICULTIES"
	FieldHealthWellbeingMentalHealthCondition                                                     Field = "HEALTH_WELLBEING_MENTAL_HEALTH_CONDITION"
	FieldHealthWellbeingPhysicalHealthCondition                                                   Field = "HEALTH_WELLBEING_PHYSICAL_HEALTH_CONDITION"
	FieldHealthWellbeingPractitionerAnalysisRiskOfReoffending                                     Field = "HEALTH_WELLBEING_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING"
	FieldHealthWellbeingPractitionerAnalysisRiskOfReoffendingNoDetails                            Field = "HEALTH_WELLBEING_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_NO_DETAILS"
	FieldHealthWellbeingPractitionerAnalysisRiskOfReoffendingYesDetails                           Field = "HEALTH_WELLBEING_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_YES_DETAILS"
	FieldHealthWellbeingPractitionerAnalysisRiskOfSeriousHarm                                     Field = "HEALTH_WELLBEING_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM"
	FieldHealthWellbeingPractitionerAnalysisRiskOfSeriousHarmNoDetails                            Field = "HEALTH_WELLBEING_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_NO_DETAILS"
	FieldHealthWellbeingPractitionerAnalysisRiskOfSeriousHarmYesDetails                           Field = "HEALTH_WELLBEING_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_YES_DETAILS"
	FieldHealthWellbeingPractitionerAnalysisStrengthsOrProtectiveFactors                          Field = "HEALTH_WELLBEING_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS"
	FieldHealthWellbeingPractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails                 Field = "HEALTH_WELLBEING_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_NO_DETAILS"
	FieldHealthWellbeingPractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails                Field = "HEALTH_WELLBEING_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_YES_DETAILS"
	FieldHealthWellbeingPsychiatricTreatment                                                      Field = "HEALTH_WELLBEING_PSYCHIATRIC_TREATMENT"
	FieldHealthWellbeingSectionComplete                                                           Field = "HEALTH_WELLBEING_SECTION_COMPLETE"
	FieldHealthWellbeingSelfHarmed                                                                Field = "HEALTH_WELLBEING_SELF_HARMED"
	FieldHowOftenUsedLastSixMonthsAmphetamines                                                    Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_AMPHETAMINES"
	FieldHowOftenUsedLastSixMonthsBenzodiazepines                                                 Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_BENZODIAZEPINES"
	FieldHowOftenUsedLastSixMonthsCannabis                                                        Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_CANNABIS"
	FieldHowOftenUsedLastSixMonthsCocaine                                                         Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_COCAINE"
	FieldHowOftenUsedLastSixMonthsCrack                                                           Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_CRACK"
	FieldHowOftenUsedLastSixMonthsEcstasy                                                         Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_ECSTASY"
	FieldHowOftenUsedLastSixMonthsHallucinogenics                                                 Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_HALLUCINOGENICS"
	FieldHowOftenUsedLastSixMonthsHeroin                                                          Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_HEROIN"
	FieldHowOftenUsedLastSixMonthsMethadoneNotPrescribed                                          Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_METHADONE_NOT_PRESCRIBED"
	FieldHowOftenUsedLastSixMonthsMisusedPrescribedDrugs                                          Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_MISUSED_PRESCRIBED_DRUGS"
	FieldHowOftenUsedLastSixMonthsOtherDrug                                                       Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_OTHER_DRUG"
	FieldHowOftenUsedLastSixMonthsOtherOpiates                                                    Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_OTHER_OPIATES"
	FieldHowOftenUsedLastSixMonthsSolvents                                                        Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_SOLVENTS"
	FieldHowOftenUsedLastSixMonthsSpice                                                           Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_SPICE"
	FieldHowOftenUsedLastSixMonthsSteroids                                                        Field = "HOW_OFTEN_USED_LAST_SIX_MONTHS_STEROIDS"
	FieldInjectingDrugAmphetamines                                                                Field = "INJECTING_DRUG_AMPHETAMINES"
	FieldInjectingDrugBenzodiazepines                                                             Field = "INJECTING_DRUG_BENZODIAZEPINES"
	FieldInjectingDrugCocaine                                                                     Field = "INJECTING_DRUG_COCAINE"
	FieldInjectingDrugCrack                                                                       Field = "INJECTING_DRUG_CRACK"
	FieldInjectingDrugHeroin                                                                      Field = "INJECTING_DRUG_HEROIN"
	FieldInjectingDrugMethadoneNotPrescribed                                                      Field = "INJECTING_DRUG_METHADONE_NOT_PRESCRIBED"
	FieldInjectingDrugMisusedPrescribedDrugs                                                      Field = "INJECTING_DRUG_MISUSED_PRESCRIBED_DRUGS"
	FieldInjectingDrugOtherDrug                                                                   Field = "INJECTING_DRUG_OTHER_DRUG"
	FieldInjectingDrugOtherOpiates                                                                Field = "INJECTING_DRUG_OTHER_OPIATES"
	FieldInjectingDrugPrescribedDrugs                                                             Field = "INJECTING_DRUG_PRESCRIBED_DRUGS"
	FieldInjectingDrugSteroids                                                                    Field = "INJECTING_DRUG_STEROIDS"
	FieldLivingWith                                                                               Field = "LIVING_WITH"
	FieldMotivatedStoppingDrugUse                                                                 Field = "MOTIVATED_STOPPING_DRUG_USE"
	FieldNoOffenceRiskDetails                                                                     Field = "NO_OFFENCE_RISK_DETAILS"
	FieldOffenceAnalysisAcceptResponsibility                                                      Field = "OFFENCE_ANALYSIS_ACCEPT_RESPONSIBILITY"
	FieldOffenceAnalysisAcceptResponsibilityNoDetails                                             Field = "OFFENCE_ANALYSIS_ACCEPT_RESPONSIBILITY_NO_DETAILS"
	FieldOffenceAnalysisAcceptResponsibilityYesDetails                                            Field = "OFFENCE_ANALYSIS_ACCEPT_RESPONSIBILITY_YES_DETAILS"
	FieldOffenceAnalysisDescriptionOfOffence                                                      Field = "OFFENCE_ANALYSIS_DESCRIPTION_OF_OFFENCE"
	FieldOffenceAnalysisElements                                                                  Field = "OFFENCE_ANALYSIS_ELEMENTS"
	FieldOffenceAnalysisEscalation                                                                Field = "OFFENCE_ANALYSIS_ESCALATION"
	FieldOffenceAnalysisHowManyInvolved                                                           Field = "OFFENCE_ANALYSIS_HOW_MANY_INVOLVED"
	FieldOffenceAnalysisImpactOnVictims                                                           Field = "OFFENCE_ANALYSIS_IMPACT_ON_VICTIMS"
	FieldOffenceAnalysisLeader                                                                    Field = "OFFENCE_ANALYSIS_LEADER"
	FieldOffenceAnalysisLeaderNoDetails                                                           Field = "OFFENCE_ANALYSIS_LEADER_NO_DETAILS"
	FieldOffenceAnalysisLeaderYesDetails                                                          Field = "OFFENCE_ANALYSIS_LEADER_YES_DETAILS"
	FieldOffenceAnalysisMotivations                                                               Field = "OFFENCE_ANALYSIS_MOTIVATIONS"
	FieldOffenceAnalysisMotivationsOtherDetails                                                   Field = "OFFENCE_ANALYSIS_MOTIVATIONS_OTHER_DETAILS"
	FieldOffenceAnalysisPatternsOfOffending                                                       Field = "OFFENCE_ANALYSIS_PATTERNS_OF_OFFENDING"
	FieldOffenceAnalysisPerpetratorOfDomesticAbuse                                                Field = "OFFENCE_ANALYSIS_PERPETRATOR_OF_DOMESTIC_ABUSE"
	FieldOffenceAnalysisPerpetratorOfDomesticAbuseType                                            Field = "OFFENCE_ANALYSIS_PERPETRATOR_OF_DOMESTIC_ABUSE_TYPE"
	FieldOffenceAnalysisReason                                                                    Field = "OFFENCE_ANALYSIS_REASON"
	FieldOffenceAnalysisRisk                                                                      Field = "OFFENCE_ANALYSIS_RISK"
	FieldOffenceAnalysisRiskNoDetails                                                             Field = "OFFENCE_ANALYSIS_RISK_NO_DETAILS"
	FieldOffenceAnalysisRiskYesDetails                                                            Field = "OFFENCE_ANALYSIS_RISK_YES_DETAILS"
	FieldOffenceAnalysisSectionComplete                                                           Field = "OFFENCE_ANALYSIS_SECTION_COMPLETE"
	FieldOffenceAnalysisVictimAge                                                                 Field = "OFFENCE_ANALYSIS_VICTIM_AGE"
	FieldOffenceAnalysisVictimOfDomesticAbuse                                                     Field = "OFFENCE_ANALYSIS_VICTIM_OF_DOMESTIC_ABUSE"
	FieldOffenceAnalysisVictimOfDomesticAbuseType                                                 Field = "OFFENCE_ANALYSIS_VICTIM_OF_DOMESTIC_ABUSE_TYPE"
	FieldOffenceAnalysisVictimRace                                                                Field = "OFFENCE_ANALYSIS_VICTIM_RACE"
	FieldOffenceAnalysisVictimRelationship                                                        Field = "OFFENCE_ANALYSIS_VICTIM_RELATIONSHIP"
	FieldOffenceAnalysisVictimRelationshipOtherDetails                                            Field = "OFFENCE_ANALYSIS_VICTIM_RELATIONSHIP_OTHER_DETAILS"
	FieldOffenceAnalysisVictimSex                                                                 Field = "OFFENCE_ANALYSIS_VICTIM_SEX"
	FieldOtherDrugName                                                                            Field = "OTHER_DRUG_NAME"
	FieldPastDrugUsageAmphetamines                                                                Field = "PAST_DRUG_USAGE_AMPHETAMINES"
	FieldPastDrugUsageBenzodiazepines                                                             Field = "PAST_DRUG_USAGE_BENZODIAZEPINES"
	FieldPastDrugUsageCannabis                                                                    Field = "PAST_DRUG_USAGE_CANNABIS"
	FieldPastDrugUsageCocaine                                                                     Field = "PAST_DRUG_USAGE_COCAINE"
	FieldPastDrugUsageCrack                                                                       Field = "PAST_DRUG_USAGE_CRACK"
	FieldPastDrugUsageEcstasy                                                                     Field = "PAST_DRUG_USAGE_ECSTASY"
	FieldPastDrugUsageHallucinogenics                                                             Field = "PAST_DRUG_USAGE_HALLUCINOGENICS"
	FieldPastDrugUsageHeroin                                                                      Field = "PAST_DRUG_USAGE_HEROIN"
	FieldPastDrugUsageMethadoneNotPrescribed                                                      Field = "PAST_DRUG_USAGE_METHADONE_NOT_PRESCRIBED"
	FieldPastDrugUsageMisusedPrescribedDrugs                                                      Field = "PAST_DRUG_USAGE_MISUSED_PRESCRIBED_DRUGS"
	FieldPastDrugUsageOtherDrug                                                                   Field = "PAST_DRUG_USAGE_OTHER_DRUG"
	FieldPastDrugUsageOtherOpiates                                                                Field = "PAST_DRUG_USAGE_OTHER_OPIATES"
	FieldPastDrugUsageSolvents                                                                    Field = "PAST_DRUG_USAGE_SOLVENTS"
	FieldPastDrugUsageSpice                                                                       Field = "PAST_DRUG_USAGE_SPICE"
	FieldPastDrugUsageSteroids                                                                    Field = "PAST_DRUG_USAGE_STEROIDS"
	FieldPastInjectingDrugAmphetamines                                                            Field = "PAST_INJECTING_DRUG_AMPHETAMINES"
	FieldPastInjectingDrugBenzodiazepines                                                         Field = "PAST_INJECTING_DRUG_BENZODIAZEPINES"
	FieldPastInjectingDrugCocaine                                                                 Field = "PAST_INJECTING_DRUG_COCAINE"
	FieldPastInjectingDrugCrack                                                                   Field = "PAST_INJECTING_DRUG_CRACK"
	FieldPastInjectingDrugHeroin                                                                  Field = "PAST_INJECTING_DRUG_HEROIN"
	FieldPastInjectingDrugMethadoneNotPrescribed                                                  Field = "PAST_INJECTING_DRUG_METHADONE_NOT_PRESCRIBED"
	FieldPastInjectingDrugMisusedPrescribedDrugs                                                  Field = "PAST_INJECTING_DRUG_MISUSED_PRESCRIBED_DRUGS"
	FieldPastInjectingDrugOtherDrug                                                               Field = "PAST_INJECTING_DRUG_OTHER_DRUG"
	FieldPastInjectingDrugOtherOpiates                                                            Field = "PAST_INJECTING_DRUG_OTHER_OPIATES"
	FieldPastInjectingDrugPrescribedDrugs                                                         Field = "PAST_INJECTING_DRUG_PRESCRIBED_DRUGS"
	FieldPastInjectingDrugSteroids                                                                Field = "PAST_INJECTING_DRUG_STEROIDS"
	FieldPersonalRelationshipsCommunityChildhood                                                  Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_CHILDHOOD"
	FieldPersonalRelationshipsCommunityChildhoodBehaviour                                         Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_CHILDHOOD_BEHAVIOUR"
	FieldPersonalRelationshipsCommunityCurrentRelationship                                        Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_CURRENT_RELATIONSHIP"
	FieldPersonalRelationshipsCommunityFamilyRelationship                                         Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_FAMILY_RELATIONSHIP"
	FieldPersonalRelationshipsCommunityImportantPeople                                            Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_IMPORTANT_PEOPLE"
	FieldPersonalRelationshipsCommunityIntimateRelationship                                       Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_INTIMATE_RELATIONSHIP"
	FieldPersonalRelationshipsCommunityParentalResponsibilities                                   Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_PARENTAL_RESPONSIBILITIES"
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfReoffending                      Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING"
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfReoffendingNoDetails             Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_NO_DETAILS"
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfReoffendingYesDetails            Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_YES_DETAILS"
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfSeriousHarm                      Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM"
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfSeriousHarmNoDetails             Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_NO_DETAILS"
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfSeriousHarmYesDetails            Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_YES_DETAILS"
	FieldPersonalRelationshipsCommunityPractitionerAnalysisStrengthsOrProtectiveFactors           Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS"
	FieldPersonalRelationshipsCommunityPractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails  Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_NO_DETAILS"
	FieldPersonalRelationshipsCommunityPractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_YES_DETAILS"
	FieldPersonalRelationshipsCommunitySectionComplete                                            Field = "PERSONAL_RELATIONSHIPS_COMMUNITY_SECTION_COMPLETE"
	FieldSuitableHousing                                                                          Field = "SUITABLE_HOUSING"
	FieldSuitableHousingLocation                                                                  Field = "SUITABLE_HOUSING_LOCATION"
	FieldTestField                                                                                Field = "TEST_FIELD"
	FieldThinkingBehavioursAttitudesConsequences                                                  Field = "THINKING_BEHAVIOURS_ATTITUDES_CONSEQUENCES"
	FieldThinkingBehavioursAttitudesCriminalBehaviour                                             Field = "THINKING_BEHAVIOURS_ATTITUDES_CRIMINAL_BEHAVIOUR"
	FieldThinkingBehavioursAttitudesEmotionalIntimacy                                             Field = "THINKING_BEHAVIOURS_ATTITUDES_EMOTIONAL_INTIMACY"
	FieldThinkingBehavioursAttitudesHostileOrientation                                            Field = "THINKING_BEHAVIOURS_ATTITUDES_HOSTILE_ORIENTATION"
	FieldThinkingBehavioursAttitudesImpulsiveBehaviour                                            Field = "THINKING_BEHAVIOURS_ATTITUDES_IMPULSIVE_BEHAVIOUR"
	FieldThinkingBehavioursAttitudesManipulativePredatoryBehaviour                                Field = "THINKING_BEHAVIOURS_ATTITUDES_MANIPULATIVE_PREDATORY_BEHAVIOUR"
	FieldThinkingBehavioursAttitudesOffenceRelatedSexualInterest                                  Field = "THINKING_BEHAVIOURS_ATTITUDES_OFFENCE_RELATED_SEXUAL_INTEREST"
	FieldThinkingBehavioursAttitudesOffendingActivities                                           Field = "THINKING_BEHAVIOURS_ATTITUDES_OFFENDING_ACTIVITIES"
	FieldThinkingBehavioursAttitudesPeerPressure                                                  Field = "THINKING_BEHAVIOURS_ATTITUDES_PEER_PRESSURE"
	FieldThinkingBehavioursAttitudesPeoplesViews                                                  Field = "THINKING_BEHAVIOURS_ATTITUDES_PEOPLES_VIEWS"
	FieldThinkingBehavioursAttitudesPositiveAttitude                                              Field = "THINKING_BEHAVIOURS_ATTITUDES_POSITIVE_ATTITUDE"
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfReoffending                         Field = "THINKING_BEHAVIOURS_ATTITUDES_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING"
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfReoffendingNoDetails                Field = "THINKING_BEHAVIOURS_ATTITUDES_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_NO_DETAILS"
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfReoffendingYesDetails               Field = "THINKING_BEHAVIOURS_ATTITUDES_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_YES_DETAILS"
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfSeriousHarm                         Field = "THINKING_BEHAVIOURS_ATTITUDES_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM"
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfSeriousHarmNoDetails                Field = "THINKING_BEHAVIOURS_ATTITUDES_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_NO_DETAILS"
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfSeriousHarmYesDetails               Field = "THINKING_BEHAVIOURS_ATTITUDES_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_YES_DETAILS"
	FieldThinkingBehavioursAttitudesPractitionerAnalysisStrengthsOrProtectiveFactors              Field = "THINKING_BEHAVIOURS_ATTITUDES_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS"
	FieldThinkingBehavioursAttitudesPractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails     Field = "THINKING_BEHAVIOURS_ATTITUDES_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_NO_DETAILS"
	FieldThinkingBehavioursAttitudesPractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails    Field = "THINKING_BEHAVIOURS_ATTITUDES_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_YES_DETAILS"
	FieldThinkingBehavioursAttitudesProblemSolving                                                Field = "THINKING_BEHAVIOURS_ATTITUDES_PROBLEM_SOLVING"
	FieldThinkingBehavioursAttitudesRiskSexualHarm                                                Field = "THINKING_BEHAVIOURS_ATTITUDES_RISK_SEXUAL_HARM"
	FieldThinkingBehavioursAttitudesSectionComplete                                               Field = "THINKING_BEHAVIOURS_ATTITUDES_SECTION_COMPLETE"
	FieldThinkingBehavioursAttitudesSexualPreoccupation                                           Field = "THINKING_BEHAVIOURS_ATTITUDES_SEXUAL_PREOCCUPATION"
	FieldThinkingBehavioursAttitudesStableBehaviour                                               Field = "THINKING_BEHAVIOURS_ATTITUDES_STABLE_BEHAVIOUR"
	FieldThinkingBehavioursAttitudesSupervision                                                   Field = "THINKING_BEHAVIOURS_ATTITUDES_SUPERVISION"
	FieldThinkingBehavioursAttitudesTemperManagement                                              Field = "THINKING_BEHAVIOURS_ATTITUDES_TEMPER_MANAGEMENT"
	FieldThinkingBehavioursAttitudesViolenceControllingBehaviour                                  Field = "THINKING_BEHAVIOURS_ATTITUDES_VIOLENCE_CONTROLLING_BEHAVIOUR"
	FieldTypeOfTemporaryAccommodation                                                             Field = "TYPE_OF_TEMPORARY_ACCOMMODATION"
	FieldYesOffenceRiskDetails                                                                    Field = "YES_OFFENCE_RISK_DETAILS"
)

const (
	ValueAddictionsOrPerceivedNeeds                             Value = "ADDICTIONS_OR_PERCEIVED_NEEDS"
	ValueAge0To4Years                                           Value = "AGE_0_TO_4_YEARS"
	ValueAge12To15Years                                         Value = "AGE_12_TO_15_YEARS"
	ValueAge16To17Years                                         Value = "AGE_16_TO_17_YEARS"
	ValueAge18To20Years                                         Value = "AGE_18_TO_20_YEARS"
	ValueAge21To25Years                                         Value = "AGE_21_TO_25_YEARS"
	ValueAge26To49Years                                         Value = "AGE_26_TO_49_YEARS"
	ValueAge50To64Years                                         Value = "AGE_50_TO_64_YEARS"
	ValueAge5To11Years                                          Value = "AGE_5_TO_11_YEARS"
	ValueAge65AndOver                                           Value = "AGE_65_AND_OVER"
	ValueAmphetamines                                           Value = "AMPHETAMINES"
	ValueAnyOtherEthnicGroup                                    Value = "ANY_OTHER_ETHNIC_GROUP"
	ValueApprenticeship                                         Value = "APPRENTICESHIP"
	ValueArab                                                   Value = "ARAB"
	ValueArson                                                  Value = "ARSON"
	ValueAsianOrAsianBritishAnyOtherAsianBackground             Value = "ASIAN_OR_ASIAN_BRITISH_ANY_OTHER_ASIAN_BACKGROUND"
	ValueAsianOrAsianBritishBangladeshi                         Value = "ASIAN_OR_ASIAN_BRITISH_BANGLADESHI"
	ValueAsianOrAsianBritishChinese                             Value = "ASIAN_OR_ASIAN_BRITISH_CHINESE"
	ValueAsianOrAsianBritishIndian                              Value = "ASIAN_OR_ASIAN_BRITISH_INDIAN"
	ValueAsianOrAsianBritishPakistani                           Value = "ASIAN_OR_ASIAN_BRITISH_PAKISTANI"
	ValueBad                                                    Value = "BAD"
	ValueBenzodiazepines                                        Value = "BENZODIAZEPINES"
	ValueBlackOrBlackBritishAfrican                             Value = "BLACK_OR_BLACK_BRITISH_AFRICAN"
	ValueBlackOrBlackBritishAnyOtherBlackBackground             Value = "BLACK_OR_BLACK_BRITISH_ANY_OTHER_BLACK_BACKGROUND"
	ValueBlackOrBlackBritishCaribbean                           Value = "BLACK_OR_BLACK_BRITISH_CARIBBEAN"
	ValueCarersAllowance                                        Value = "CARERS_ALLOWANCE"
	ValueChildParentalResponsibilities                          Value = "CHILD_PARENTAL_RESPONSIBILITIES"
	ValueCocaine                                                Value = "COCAINE"
	ValueConcernsHappyRelationship                              Value = "CONCERNS_HAPPY_RELATIONSHIP"
	ValueCrack                                                  Value = "CRACK"
	ValueCriminalJusticeStaff                                   Value = "CRIMINAL_JUSTICE_STAFF"
	ValueCurrentlyUnavailableForWork                            Value = "CURRENTLY_UNAVAILABLE_FOR_WORK"
	ValueDaily                                                  Value = "DAILY"
	ValueDisabilityBenefits                                     Value = "DISABILITY_BENEFITS"
	ValueDomesticAbuse                                          Value = "DOMESTIC_ABUSE"
	ValueElevenTo15                                             Value = "ELEVEN_TO_15"
	ValueEmotionalState                                         Value = "EMOTIONAL_STATE"
	ValueEmployed                                               Value = "EMPLOYED"
	ValueEmployment                                             Value = "EMPLOYMENT"
	ValueEntryLevel                                             Value = "ENTRY_LEVEL"
	ValueExcessiveOrSadisticViolence                            Value = "EXCESSIVE_OR_SADISTIC_VIOLENCE"
	ValueFairlyBad                                              Value = "FAIRLY_BAD"
	ValueFairlyGood                                             Value = "FAIRLY_GOOD"
	ValueFamily                                                 Value = "FAMILY"
	ValueFamilyMember                                           Value = "FAMILY_MEMBER"
	ValueFamilyMemberAndIntimatePartner                         Value = "FAMILY_MEMBER_AND_INTIMATE_PARTNER"
	ValueFamilyOrFriends                                        Value = "FAMILY_OR_FRIENDS"
	ValueFemale                                                 Value = "FEMALE"
	ValueFinancialMotivation                                    Value = "FINANCIAL_MOTIVATION"
	ValueFive                                                   Value = "FIVE"
	ValueFour                                                   Value = "FOUR"
	ValueFullMotivation                                         Value = "FULL_MOTIVATION"
	ValueFullTime                                               Value = "FULL_TIME"
	ValueGood                                                   Value = "GOOD"
	ValueHappyRelationship                                      Value = "HAPPY_RELATIONSHIP"
	ValueHatredOfIdentifiableGroups                             Value = "HATRED_OF_IDENTIFIABLE_GROUPS"
	ValueHeroin                                                 Value = "HEROIN"
	ValueIntersex                                               Value = "INTERSEX"
	ValueIntimatePartner                                        Value = "INTIMATE_PARTNER"
	ValueLastSix                                                Value = "LAST_SIX"
	ValueLessThan4TimesAWeek                                    Value = "LESS_THAN_4_TIMES_A_WEEK"
	ValueLevel1                                                 Value = "LEVEL_1"
	ValueLevel2                                                 Value = "LEVEL_2"
	ValueLevel3                                                 Value = "LEVEL_3"
	ValueLevel4                                                 Value = "LEVEL_4"
	ValueLevel5                                                 Value = "LEVEL_5"
	ValueLevel6                                                 Value = "LEVEL_6"
	ValueLevel7                                                 Value = "LEVEL_7"
	ValueLevel8                                                 Value = "LEVEL_8"
	ValueLimitedProblemSolving                                  Value = "LIMITED_PROBLEM_SOLVING"
	ValueMale                                                   Value = "MALE"
	ValueMethadoneNotPrescribed                                 Value = "METHADONE_NOT_PRESCRIBED"
	ValueMisusedPrescribedDrugs                                 Value = "MISUSED_PRESCRIBED_DRUGS"
	ValueMixedAnyOtherMixedOrMultipleEthnicBackgroundBackground Value = "MIXED_ANY_OTHER_MIXED_OR_MULTIPLE_ETHNIC_BACKGROUND_BACKGROUND"
	ValueMixedChildhood                                         Value = "MIXED_CHILDHOOD"
	ValueMixedRelationship                                      Value = "MIXED_RELATIONSHIP"
	ValueMixedWhiteAndAsian                                     Value = "MIXED_WHITE_AND_ASIAN"
	ValueMixedWhiteAndBlackAfrican                              Value = "MIXED_WHITE_AND_BLACK_AFRICAN"
	ValueMixedWhiteAndBlackCaribbean                            Value = "MIXED_WHITE_AND_BLACK_CARIBBEAN"
	ValueMonthly                                                Value = "MONTHLY"
	ValueMoreThan15                                             Value = "MORE_THAN_15"
	ValueMoreThan4TimesAWeek                                    Value = "MORE_THAN_4_TIMES_A_WEEK"
	ValueMoreThanSix                                            Value = "MORE_THAN_SIX"
	ValueMostlyNegative                                         Value = "MOSTLY_NEGATIVE"
	ValueMostlyPositive                                         Value = "MOSTLY_POSITIVE"
	ValueMotivated                                              Value = "MOTIVATED"
	ValueMultipleTimesAMonth                                    Value = "MULTIPLE_TIMES_A_MONTH"
	ValueNegative                                               Value = "NEGATIVE"
	ValueNegativeAttitudeAndConcerns                            Value = "NEGATIVE_ATTITUDE_AND_CONCERNS"
	ValueNegativeAttitudeNoConcerns                             Value = "NEGATIVE_ATTITUDE_NO_CONCERNS"
	ValueNegativeChildhood                                      Value = "NEGATIVE_CHILDHOOD"
	ValueNo                                                     Value = "NO"
	ValueNone                                                   Value = "NONE"
	ValueNoneOfThese                                            Value = "NONE_OF_THESE"
	ValueNotSure                                                Value = "NOT_SURE"
	ValueNoAccommodation                                        Value = "NO_ACCOMMODATION"
	ValueNoEvidence                                             Value = "NO_EVIDENCE"
	ValueNoMoney                                                Value = "NO_MONEY"
	ValueNoMotivation                                           Value = "NO_MOTIVATION"
	ValueNoOffenceRelatedSexualInterest                         Value = "NO_OFFENCE_RELATED_SEXUAL_INTEREST"
	ValueNoOffendingActivities                                  Value = "NO_OFFENDING_ACTIVITIES"
	ValueNoSupervision                                          Value = "NO_SUPERVISION"
	ValueNoViolence                                             Value = "NO_VIOLENCE"
	ValueNumeracy                                               Value = "NUMERACY"
	ValueOccasionally                                           Value = "OCCASIONALLY"
	ValueOffending                                              Value = "OFFENDING"
	ValueOnceAMonthOrLess                                       Value = "ONCE_A_MONTH_OR_LESS"
	ValueOne                                                    Value = "ONE"
	ValueOther                                                  Value = "OTHER"
	ValueOtherDrug                                              Value = "OTHER_DRUG"
	ValueOtherFamilyMember                                      Value = "OTHER_FAMILY_MEMBER"
	ValueOtherOpiates                                           Value = "OTHER_OPIATES"
	ValuePartialMotivation                                      Value = "PARTIAL_MOTIVATION"
	ValuePartner                                                Value = "PARTNER"
	ValuePartnerIntimateRelationship                            Value = "PARTNER_INTIMATE_RELATIONSHIP"
	ValuePartTime                                               Value = "PART_TIME"
	ValuePendingTreatment                                       Value = "PENDING_TREATMENT"
	ValuePension                                                Value = "PENSION"
	ValuePeriodsOfInstability                                   Value = "PERIODS_OF_INSTABILITY"
	ValuePhysicalDamageToProperty                               Value = "PHYSICAL_DAMAGE_TO_PROPERTY"
	ValuePopChildOrStepChild                                    Value = "POP_CHILD_OR_STEP_CHILD"
	ValuePopExPartner                                           Value = "POP_EX_PARTNER"
	ValuePopParentOrStepParent                                  Value = "POP_PARENT_OR_STEP_PARENT"
	ValuePopPartner                                             Value = "POP_PARTNER"
	ValuePositive                                               Value = "POSITIVE"
	ValuePositiveAndNegative                                    Value = "POSITIVE_AND_NEGATIVE"
	ValuePositiveAndNegativeRelationships                       Value = "POSITIVE_AND_NEGATIVE_RELATIONSHIPS"
	ValuePositiveChildhood                                      Value = "POSITIVE_CHILDHOOD"
	ValuePressurisedByOthers                                    Value = "PRESSURISED_BY_OTHERS"
	ValueReading                                                Value = "READING"
	ValueRetired                                                Value = "RETIRED"
	ValueSelfEmployed                                           Value = "SELF_EMPLOYED"
	ValueSettled                                                Value = "SETTLED"
	ValueSexualElement                                          Value = "SEXUAL_ELEMENT"
	ValueSexualMotivation                                       Value = "SEXUAL_MOTIVATION"
	ValueShortTerm                                              Value = "SHORT_TERM"
	ValueSignificantDifficulties                                Value = "SIGNIFICANT_DIFFICULTIES"
	ValueSixTo10                                                Value = "SIX_TO_10"
	ValueSome                                                   Value = "SOME"
	ValueSometimes                                              Value = "SOMETIMES"
	ValueSometimesOffendingActivities                           Value = "SOMETIMES_OFFENDING_ACTIVITIES"
	ValueSomeDifficulties                                       Value = "SOME_DIFFICULTIES"
	ValueSomeMotivation                                         Value = "SOME_MOTIVATION"
	ValueSomeNegativeAspects                                    Value = "SOME_NEGATIVE_ASPECTS"
	ValueSomeOffenceRelatedSexualInterest                       Value = "SOME_OFFENCE_RELATED_SEXUAL_INTEREST"
	ValueStable                                                 Value = "STABLE"
	ValueStableRelationship                                     Value = "STABLE_RELATIONSHIP"
	ValueStableRelationships                                    Value = "STABLE_RELATIONSHIPS"
	ValueSteroids                                               Value = "STEROIDS"
	ValueStranger                                               Value = "STRANGER"
	ValueStudentLoan                                            Value = "STUDENT_LOAN"
	ValueTemporary                                              Value = "TEMPORARY"
	ValueTemporaryOrCasual                                      Value = "TEMPORARY_OR_CASUAL"
	ValueThree                                                  Value = "THREE"
	ValueThrillSeeking                                          Value = "THRILL_SEEKING"
	ValueTwo                                                    Value = "TWO"
	ValueUnemployedLookingForWork                               Value = "UNEMPLOYED_LOOKING_FOR_WORK"
	ValueUnemployedNotLookingForWork                            Value = "UNEMPLOYED_NOT_LOOKING_FOR_WORK"
	ValueUnhappyRelationship                                    Value = "UNHAPPY_RELATIONSHIP"
	ValueUnits10OrMore                                          Value = "UNITS_10_OR_MORE"
	ValueUnits1To2                                              Value = "UNITS_1_TO_2"
	ValueUnits3To4                                              Value = "UNITS_3_TO_4"
	ValueUnits5To6                                              Value = "UNITS_5_TO_6"
	ValueUnits7To9                                              Value = "UNITS_7_TO_9"
	ValueUnknown                                                Value = "UNKNOWN"
	ValueUnstable                                               Value = "UNSTABLE"
	ValueUnstableRelationship                                   Value = "UNSTABLE_RELATIONSHIP"
	ValueUnstableRelationships                                  Value = "UNSTABLE_RELATIONSHIPS"
	ValueUnsureSupervision                                      Value = "UNSURE_SUPERVISION"
	ValueUndeclared                                             Value = "Undeclared"
	ValueVictimTargeted                                         Value = "VICTIM_TARGETED"
	ValueViolenceOrCoercion                                     Value = "VIOLENCE_OR_COERCION"
	ValueWeapon                                                 Value = "WEAPON"
	ValueWeekly                                                 Value = "WEEKLY"
	ValueWhiteAnyOtherWhiteBackground                           Value = "WHITE_ANY_OTHER_WHITE_BACKGROUND"
	ValueWhiteEnglishWelshScottishNorthernIrishOrBritish        Value = "WHITE_ENGLISH_WELSH_SCOTTISH_NORTHERN_IRISH_OR_BRITISH"
	ValueWhiteGypsyOrIrishTraveller                             Value = "WHITE_GYPSY_OR_IRISH_TRAVELLER"
	ValueWhiteIrish                                             Value = "WHITE_IRISH"
	ValueWhiteRoma                                              Value = "WHITE_ROMA"
	ValueWorkRelatedBenefits                                    Value = "WORK_RELATED_BENEFITS"
	ValueWriting                                                Value = "WRITING"
	ValueYes                                                    Value = "YES"
	ValueYesInThePast                                           Value = "YES_IN_THE_PAST"
	ValueYesNotInLastThreeMonths                                Value = "YES_NOT_IN_LAST_THREE_MONTHS"
	ValueYesOffenceRelatedSexualInterest                        Value = "YES_OFFENCE_RELATED_SEXUAL_INTEREST"
	ValueYesOffendingActivities                                 Value = "YES_OFFENDING_ACTIVITIES"
	ValueYesOngoing                                             Value = "YES_ONGOING"
	ValueYesOngoingSevere                                       Value = "YES_ONGOING_SEVERE"
	ValueYesPositive                                            Value = "YES_POSITIVE"
	ValueYesSignificantDifficulties                             Value = "YES_SIGNIFICANT_DIFFICULTIES"
	ValueYesSomeDifficulties                                    Value = "YES_SOME_DIFFICULTIES"
	ValueYesSomeSkills                                          Value = "YES_SOME_SKILLS"
	ValueYesSupervision                                         Value = "YES_SUPERVISION"
	ValueYesViolence                                            Value = "YES_VIOLENCE"
	ValueYesWithinLastThreeMonths                               Value = "YES_WITHIN_LAST_THREE_MONTHS"
	ValueYesWithConcerns                                        Value = "YES_WITH_CONCERNS"
	ValueYesWithEvidence                                        Value = "YES_WITH_EVIDENCE"
	ValueYesWithSomeEvidence                                    Value = "YES_WITH_SOME_EVIDENCE"
)

// Fields lists every field, for looking fields up by name.
var Fields = []Field{
	FieldAccommodationPractitionerAnalysisRiskOfReoffending,
	FieldAccommodationPractitionerAnalysisRiskOfReoffendingNoDetails,
	FieldAccommodationPractitionerAnalysisRiskOfReoffendingYesDetails,
	FieldAccommodationPractitionerAnalysisRiskOfSeriousHarm,
	FieldAccommodationPractitionerAnalysisRiskOfSeriousHarmNoDetails,
	FieldAccommodationPractitionerAnalysisRiskOfSeriousHarmYesDetails,
	FieldAccommodationPractitionerAnalysisStrengthsOrProtectiveFactors,
	FieldAccommodationPractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails,
	FieldAccommodationPractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails,
	FieldAccommodationSectionComplete,
	FieldAlcoholEvidenceOfExcessDrinking,
	FieldAlcoholFrequency,
	FieldAlcoholUnits,
	FieldAlcoholUse,
	FieldAlcoholUsePractitionerAnalysisRiskOfReoffending,
	FieldAlcoholUsePractitionerAnalysisRiskOfReoffendingNoDetails,
	FieldAlcoholUsePractitionerAnalysisRiskOfReoffendingYesDetails,
	FieldAlcoholUsePractitionerAnalysisRiskOfSeriousHarm,
	FieldAlcoholUsePractitionerAnalysisRiskOfSeriousHarmNoDetails,
	FieldAlcoholUsePractitionerAnalysisRiskOfSeriousHarmYesDetails,
	FieldAlcoholUsePractitionerAnalysisStrengthsOrProtectiveFactors,
	FieldAlcoholUsePractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails,
	FieldAlcoholUsePractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails,
	FieldAlcoholUseSectionComplete,
	FieldCurrentAccommodation,
	FieldDrugLastUsedAmphetamines,
	FieldDrugLastUsedBenzodiazepines,
	FieldDrugLastUsedCannabis,
	FieldDrugLastUsedCocaine,
	FieldDrugLastUsedCrack,
	FieldDrugLastUsedEcstasy,
	FieldDrugLastUsedHallucinogenics,
	FieldDrugLastUsedHeroin,
	FieldDrugLastUsedMethadoneNotPrescribed,
	FieldDrugLastUsedMisusedPrescribedDrugs,
	FieldDrugLastUsedOtherDrug,
	FieldDrugLastUsedOtherOpiates,
	FieldDrugLastUsedSolvents,
	FieldDrugLastUsedSpice,
	FieldDrugLastUsedSteroids,
	FieldDrugUsageAmphetamines,
	FieldDrugUsageBenzodiazepines,
	FieldDrugUsageCannabis,
	FieldDrugUsageCocaine,
	FieldDrugUsageCrack,
	FieldDrugUsageEcstasy,
	FieldDrugUsageHallucinogenics,
	FieldDrugUsageHeroin,
	FieldDrugUsageMethadoneNotPrescribed,
	FieldDrugUsageMisusedPrescribedDrugs,
	FieldDrugUsageOtherDrug,
	FieldDrugUsageOtherOpiates,
	FieldDrugUsageSolvents,
	FieldDrugUsageSpice,
	FieldDrugUsageSteroids,
	FieldDrugUse,
	FieldDrugUsePractitionerAnalysisRiskOfReoffending,
	FieldDrugUsePractitionerAnalysisRiskOfReoffendingNoDetails,
	FieldDrugUsePractitionerAnalysisRiskOfReoffendingYesDetails,
	FieldDrugUsePractitionerAnalysisRiskOfSeriousHarm,
	FieldDrugUsePractitionerAnalysisRiskOfSeriousHarmNoDetails,
	FieldDrugUsePractitionerAnalysisRiskOfSeriousHarmYesDetails,
	FieldDrugUsePractitionerAnalysisStrengthsOrProtectiveFactors,
	FieldDrugUsePractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails,
	FieldDrugUsePractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails,
	FieldDrugUseSectionComplete,
	FieldDrugUseTypeOtherDrugDetails,
	FieldDrugsInjected,
	FieldDrugsInjectedAmphetamines,
	FieldDrugsInjectedBenzodiazepines,
	FieldDrugsInjectedCocaine,
	FieldDrugsInjectedCrack,
	FieldDrugsInjectedHeroin,
	FieldDrugsInjectedMethadoneNotPrescribed,
	FieldDrugsInjectedMisusedPrescribedDrugs,
	FieldDrugsInjectedOtherDrug,
	FieldDrugsInjectedOtherOpiates,
	FieldDrugsInjectedSteroids,
	FieldDrugsPractitionerAnalysisMotivatedToStop,
	FieldEducationDifficulties,
	FieldEducationDifficultiesNumeracySeverity,
	FieldEducationDifficultiesReadingSeverity,
	FieldEducationDifficultiesWritingSeverity,
	FieldEducationExperience,
	FieldEducationHighestLevelCompleted,
	FieldEducationProfessionalOrVocationalQualifications,
	FieldEducationProfessionalOrVocationalQualificationsYesDetails,
	FieldEducationTransferableSkills,
	FieldEmploymentEducationPractitionerAnalysisRiskOfReoffending,
	FieldEmploymentEducationPractitionerAnalysisRiskOfReoffendingNoDetails,
	FieldEmploymentEducationPractitionerAnalysisRiskOfReoffendingYesDetails,
	FieldEmploymentEducationPractitionerAnalysisRiskOfSeriousHarm,
	FieldEmploymentEducationPractitionerAnalysisRiskOfSeriousHarmNoDetails,
	FieldEmploymentEducationPractitionerAnalysisRiskOfSeriousHarmYesDetails,
	FieldEmploymentEducationPractitionerAnalysisStrengthsOrProtectiveFactors,
	FieldEmploymentEducationPractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails,
	FieldEmploymentEducationPractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails,
	FieldEmploymentEducationSectionComplete,
	FieldEmploymentExperience,
	FieldEmploymentHistory,
	FieldEmploymentStatus,
	FieldEmploymentType,
	FieldFamilyOrFriendsDetails,
	FieldFinanceIncome,
	FieldFinanceMoneyManagement,
	FieldFinancePractitionerAnalysisRiskOfReoffending,
	FieldFinancePractitionerAnalysisRiskOfReoffendingNoDetails,
	FieldFinancePractitionerAnalysisRiskOfReoffendingYesDetails,
	FieldFinancePractitionerAnalysisRiskOfSeriousHarm,
	FieldFinancePractitionerAnalysisRiskOfSeriousHarmNoDetails,
	FieldFinancePractitionerAnalysisRiskOfSeriousHarmYesDetails,
	FieldFinancePractitionerAnalysisStrengthsOrProtectiveFactors,
	FieldFinancePractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails,
	FieldFinancePractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails,
	FieldFinanceSectionComplete,
	FieldHasBeenEmployed,
	FieldHealthWellbeingAttemptedSuicideOrSuicidalThoughts,
	FieldHealthWellbeingAttitudeTowardsSelf,
	FieldHealthWellbeingCopingDayToDayLife,
	FieldHealthWellbeingHeadInjuryOrIllness,
	FieldHealthWellbeingLearningDifficulties,
	FieldHealthWellbeingMentalHealthCondition,
	FieldHealthWellbeingPhysicalHealthCondition,
	FieldHealthWellbeingPractitionerAnalysisRiskOfReoffending,
	FieldHealthWellbeingPractitionerAnalysisRiskOfReoffendingNoDetails,
	FieldHealthWellbeingPractitionerAnalysisRiskOfReoffendingYesDetails,
	FieldHealthWellbeingPractitionerAnalysisRiskOfSeriousHarm,
	FieldHealthWellbeingPractitionerAnalysisRiskOfSeriousHarmNoDetails,
	FieldHealthWellbeingPractitionerAnalysisRiskOfSeriousHarmYesDetails,
	FieldHealthWellbeingPractitionerAnalysisStrengthsOrProtectiveFactors,
	FieldHealthWellbeingPractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails,
	FieldHealthWellbeingPractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails,
	FieldHealthWellbeingPsychiatricTreatment,
	FieldHealthWellbeingSectionComplete,
	FieldHealthWellbeingSelfHarmed,
	FieldHowOftenUsedLastSixMonthsAmphetamines,
	FieldHowOftenUsedLastSixMonthsBenzodiazepines,
	FieldHowOftenUsedLastSixMonthsCannabis,
	FieldHowOftenUsedLastSixMonthsCocaine,
	FieldHowOftenUsedLastSixMonthsCrack,
	FieldHowOftenUsedLastSixMonthsEcstasy,
	FieldHowOftenUsedLastSixMonthsHallucinogenics,
	FieldHowOftenUsedLastSixMonthsHeroin,
	FieldHowOftenUsedLastSixMonthsMethadoneNotPrescribed,
	FieldHowOftenUsedLastSixMonthsMisusedPrescribedDrugs,
	FieldHowOftenUsedLastSixMonthsOtherDrug,
	FieldHowOftenUsedLastSixMonthsOtherOpiates,
	FieldHowOftenUsedLastSixMonthsSolvents,
	FieldHowOftenUsedLastSixMonthsSpice,
	FieldHowOftenUsedLastSixMonthsSteroids,
	FieldInjectingDrugAmphetamines,
	FieldInjectingDrugBenzodiazepines,
	FieldInjectingDrugCocaine,
	FieldInjectingDrugCrack,
	FieldInjectingDrugHeroin,
	FieldInjectingDrugMethadoneNotPrescribed,
	FieldInjectingDrugMisusedPrescribedDrugs,
	FieldInjectingDrugOtherDrug,
	FieldInjectingDrugOtherOpiates,
	FieldInjectingDrugPrescribedDrugs,
	FieldInjectingDrugSteroids,
	FieldLivingWith,
	FieldMotivatedStoppingDrugUse,
	FieldNoOffenceRiskDetails,
	FieldOffenceAnalysisAcceptResponsibility,
	FieldOffenceAnalysisAcceptResponsibilityNoDetails,
	FieldOffenceAnalysisAcceptResponsibilityYesDetails,
	FieldOffenceAnalysisDescriptionOfOffence,
	FieldOffenceAnalysisElements,
	FieldOffenceAnalysisEscalation,
	FieldOffenceAnalysisHowManyInvolved,
	FieldOffenceAnalysisImpactOnVictims,
	FieldOffenceAnalysisLeader,
	FieldOffenceAnalysisLeaderNoDetails,
	FieldOffenceAnalysisLeaderYesDetails,
	FieldOffenceAnalysisMotivations,
	FieldOffenceAnalysisMotivationsOtherDetails,
	FieldOffenceAnalysisPatternsOfOffending,
	FieldOffenceAnalysisPerpetratorOfDomesticAbuse,
	FieldOffenceAnalysisPerpetratorOfDomesticAbuseType,
	FieldOffenceAnalysisReason,
	FieldOffenceAnalysisRisk,
	FieldOffenceAnalysisRiskNoDetails,
	FieldOffenceAnalysisRiskYesDetails,
	FieldOffenceAnalysisSectionComplete,
	FieldOffenceAnalysisVictimAge,
	FieldOffenceAnalysisVictimOfDomesticAbuse,
	FieldOffenceAnalysisVictimOfDomesticAbuseType,
	FieldOffenceAnalysisVictimRace,
	FieldOffenceAnalysisVictimRelationship,
	FieldOffenceAnalysisVictimRelationshipOtherDetails,
	FieldOffenceAnalysisVictimSex,
	FieldOtherDrugName,
	FieldPastDrugUsageAmphetamines,
	FieldPastDrugUsageBenzodiazepines,
	FieldPastDrugUsageCannabis,
	FieldPastDrugUsageCocaine,
	FieldPastDrugUsageCrack,
	FieldPastDrugUsageEcstasy,
	FieldPastDrugUsageHallucinogenics,
	FieldPastDrugUsageHeroin,
	FieldPastDrugUsageMethadoneNotPrescribed,
	FieldPastDrugUsageMisusedPrescribedDrugs,
	FieldPastDrugUsageOtherDrug,
	FieldPastDrugUsageOtherOpiates,
	FieldPastDrugUsageSolvents,
	FieldPastDrugUsageSpice,
	FieldPastDrugUsageSteroids,
	FieldPastInjectingDrugAmphetamines,
	FieldPastInjectingDrugBenzodiazepines,
	FieldPastInjectingDrugCocaine,
	FieldPastInjectingDrugCrack,
	FieldPastInjectingDrugHeroin,
	FieldPastInjectingDrugMethadoneNotPrescribed,
	FieldPastInjectingDrugMisusedPrescribedDrugs,
	FieldPastInjectingDrugOtherDrug,
	FieldPastInjectingDrugOtherOpiates,
	FieldPastInjectingDrugPrescribedDrugs,
	FieldPastInjectingDrugSteroids,
	FieldPersonalRelationshipsCommunityChildhood,
	FieldPersonalRelationshipsCommunityChildhoodBehaviour,
	FieldPersonalRelationshipsCommunityCurrentRelationship,
	FieldPersonalRelationshipsCommunityFamilyRelationship,
	FieldPersonalRelationshipsCommunityImportantPeople,
	FieldPersonalRelationshipsCommunityIntimateRelationship,
	FieldPersonalRelationshipsCommunityParentalResponsibilities,
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfReoffending,
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfReoffendingNoDetails,
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfReoffendingYesDetails,
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfSeriousHarm,
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfSeriousHarmNoDetails,
	FieldPersonalRelationshipsCommunityPractitionerAnalysisRiskOfSeriousHarmYesDetails,
	FieldPersonalRelationshipsCommunityPractitionerAnalysisStrengthsOrProtectiveFactors,
	FieldPersonalRelationshipsCommunityPractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails,
	FieldPersonalRelationshipsCommunityPractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails,
	FieldPersonalRelationshipsCommunitySectionComplete,
	FieldSuitableHousing,
	FieldSuitableHousingLocation,
	FieldTestField,
	FieldThinkingBehavioursAttitudesConsequences,
	FieldThinkingBehavioursAttitudesCriminalBehaviour,
	FieldThinkingBehavioursAttitudesEmotionalIntimacy,
	FieldThinkingBehavioursAttitudesHostileOrientation,
	FieldThinkingBehavioursAttitudesImpulsiveBehaviour,
	FieldThinkingBehavioursAttitudesManipulativePredatoryBehaviour,
	FieldThinkingBehavioursAttitudesOffenceRelatedSexualInterest,
	FieldThinkingBehavioursAttitudesOffendingActivities,
	FieldThinkingBehavioursAttitudesPeerPressure,
	FieldThinkingBehavioursAttitudesPeoplesViews,
	FieldThinkingBehavioursAttitudesPositiveAttitude,
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfReoffending,
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfReoffendingNoDetails,
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfReoffendingYesDetails,
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfSeriousHarm,
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfSeriousHarmNoDetails,
	FieldThinkingBehavioursAttitudesPractitionerAnalysisRiskOfSeriousHarmYesDetails,
	FieldThinkingBehavioursAttitudesPractitionerAnalysisStrengthsOrProtectiveFactors,
	FieldThinkingBehavioursAttitudesPractitionerAnalysisStrengthsOrProtectiveFactorsNoDetails,
	FieldThinkingBehavioursAttitudesPractitionerAnalysisStrengthsOrProtectiveFactorsYesDetails,
	FieldThinkingBehavioursAttitudesProblemSolving,
	FieldThinkingBehavioursAttitudesRiskSexualHarm,
	FieldThinkingBehavioursAttitudesSectionComplete,
	FieldThinkingBehavioursAttitudesSexualPreoccupation,
	FieldThinkingBehavioursAttitudesStableBehaviour,
	FieldThinkingBehavioursAttitudesSupervision,
	FieldThinkingBehavioursAttitudesTemperManagement,
	FieldThinkingBehavioursAttitudesViolenceControllingBehaviour,
	FieldTypeOfTemporaryAccommodation,
	FieldYesOffenceRiskDetails,
}

// Values lists every option.
var Values = []Value{
	ValueAddictionsOrPerceivedNeeds,
	ValueAge0To4Years,
	ValueAge12To15Years,
	ValueAge16To17Years,
	ValueAge18To20Years,
	ValueAge21To25Years,
	ValueAge26To49Years,
	ValueAge50To64Years,
	ValueAge5To11Years,
	ValueAge65AndOver,
	ValueAmphetamines,
	ValueAnyOtherEthnicGroup,
	ValueApprenticeship,
	ValueArab,
	ValueArson,
	ValueAsianOrAsianBritishAnyOtherAsianBackground,
	ValueAsianOrAsianBritishBangladeshi,
	ValueAsianOrAsianBritishChinese,
	ValueAsianOrAsianBritishIndian,
	ValueAsianOrAsianBritishPakistani,
	ValueBad,
	ValueBenzodiazepines,
	ValueBlackOrBlackBritishAfrican,
	ValueBlackOrBlackBritishAnyOtherBlackBackground,
	ValueBlackOrBlackBritishCaribbean,
	ValueCarersAllowance,
	ValueChildParentalResponsibilities,
	ValueCocaine,
	ValueConcernsHappyRelationship,
	ValueCrack,
	ValueCriminalJusticeStaff,
	ValueCurrentlyUnavailableForWork,
	ValueDaily,
	ValueDisabilityBenefits,
	ValueDomesticAbuse,
	ValueElevenTo15,
	ValueEmotionalState,
	ValueEmployed,
	ValueEmployment,
	ValueEntryLevel,
	ValueExcessiveOrSadisticViolence,
	ValueFairlyBad,
	ValueFairlyGood,
	ValueFamily,
	ValueFamilyMember,
	ValueFamilyMemberAndIntimatePartner,
	ValueFamilyOrFriends,
	ValueFemale,
	ValueFinancialMotivation,
	ValueFive,
	ValueFour,
	ValueFullMotivation,
	ValueFullTime,
	ValueGood,
	ValueHappyRelationship,
	ValueHatredOfIdentifiableGroups,
	ValueHeroin,
	ValueIntersex,
	ValueIntimatePartner,
	ValueLastSix,
	ValueLessThan4TimesAWeek,
	ValueLevel1,
	ValueLevel2,
	ValueLevel3,
	ValueLevel4,
	ValueLevel5,
	ValueLevel6,
	ValueLevel7,
	ValueLevel8,
	ValueLimitedProblemSolving,
	ValueMale,
	ValueMethadoneNotPrescribed,
	ValueMisusedPrescribedDrugs,
	ValueMixedAnyOtherMixedOrMultipleEthnicBackgroundBackground,
	ValueMixedChildhood,
	ValueMixedRelationship,
	ValueMixedWhiteAndAsian,
	ValueMixedWhiteAndBlackAfrican,
	ValueMixedWhiteAndBlackCaribbean,
	ValueMonthly,
	ValueMoreThan15,
	ValueMoreThan4TimesAWeek,
	ValueMoreThanSix,
	ValueMostlyNegative,
	ValueMostlyPositive,
	ValueMotivated,
	ValueMultipleTimesAMonth,
	ValueNegative,
	ValueNegativeAttitudeAndConcerns,
	ValueNegativeAttitudeNoConcerns,
	ValueNegativeChildhood,
	ValueNo,
	ValueNone,
	ValueNoneOfThese,
	ValueNotSure,
	ValueNoAccommodation,
	ValueNoEvidence,
	ValueNoMoney,
	ValueNoMotivation,
	ValueNoOffenceRelatedSexualInterest,
	ValueNoOffendingActivities,
	ValueNoSupervision,
	ValueNoViolence,
	ValueNumeracy,
	ValueOccasionally,
	ValueOffending,
	ValueOnceAMonthOrLess,
	ValueOne,
	ValueOther,
	ValueOtherDrug,
	ValueOtherFamilyMember,
	ValueOtherOpiates,
	ValuePartialMotivation,
	ValuePartner,
	ValuePartnerIntimateRelationship,
	ValuePartTime,
	ValuePendingTreatment,
	ValuePension,
	ValuePeriodsOfInstability,
	ValuePhysicalDamageToProperty,
	ValuePopChildOrStepChild,
	ValuePopExPartner,
	ValuePopParentOrStepParent,
	ValuePopPartner,
	ValuePositive,
	ValuePositiveAndNegative,
	ValuePositiveAndNegativeRelationships,
	ValuePositiveChildhood,
	ValuePressurisedByOthers,
	ValueReading,
	ValueRetired,
	ValueSelfEmployed,
	ValueSettled,
	ValueSexualElement,
	ValueSexualMotivation,
	ValueShortTerm,
	ValueSignificantDifficulties,
	ValueSixTo10,
	ValueSome,
	ValueSometimes,
	ValueSometimesOffendingActivities,
	ValueSomeDifficulties,
	ValueSomeMotivation,
	ValueSomeNegativeAspects,
	ValueSomeOffenceRelatedSexualInterest,
	ValueStable,
	ValueStableRelationship,
	ValueStableRelationships,
	ValueSteroids,
	ValueStranger,
	ValueStudentLoan,
	ValueTemporary,
	ValueTemporaryOrCasual,
	ValueThree,
	ValueThrillSeeking,
	ValueTwo,
	ValueUnemployedLookingForWork,
	ValueUnemployedNotLookingForWork,
	ValueUnhappyRelationship,
	ValueUnits10OrMore,
	ValueUnits1To2,
	ValueUnits3To4,
	ValueUnits5To6,
	ValueUnits7To9,
	ValueUnknown,
	ValueUnstable,
	ValueUnstableRelationship,
	ValueUnstableRelationships,
	ValueUnsureSupervision,
	ValueUndeclared,
	ValueVictimTargeted,
	ValueViolenceOrCoercion,
	ValueWeapon,
	ValueWeekly,
	ValueWhiteAnyOtherWhiteBackground,
	ValueWhiteEnglishWelshScottishNorthernIrishOrBritish,
	ValueWhiteGypsyOrIrishTraveller,
	ValueWhiteIrish,
	ValueWhiteRoma,
	ValueWorkRelatedBenefits,
	ValueWriting,
	ValueYes,
	ValueYesInThePast,
	ValueYesNotInLastThreeMonths,
	ValueYesOffenceRelatedSexualInterest,
	ValueYesOffendingActivities,
	ValueYesOngoing,
	ValueYesOngoingSevere,
	ValueYesPositive,
	ValueYesSignificantDifficulties,
	ValueYesSomeDifficulties,
	ValueYesSomeSkills,
	ValueYesSupervision,
	ValueYesViolence,
	ValueYesWithinLastThreeMonths,
	ValueYesWithConcerns,
	ValueYesWithEvidence,
	ValueYesWithSomeEvidence,
}
