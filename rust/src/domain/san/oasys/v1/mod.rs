//! Version 1.0 of the SAN form.

mod accommodation;
mod alcohol_misuse;
mod attitudes;
mod drugs;
mod education;
mod emotional_wellbeing;
mod financial_management;
mod lifestyle_associates;
mod new_sections;
mod offence_analysis;
mod predictors;
mod relationships;
mod thinking_behaviours;

use super::{
    Mapped, NULL, Section,
    answers::AnswersProvider,
    codes::{Field, Value},
    optional_text, text, when_value,
};
use crate::error::{Error, Result};

pub(super) const SECTIONS: &[Section] = &[
    accommodation::fields,
    alcohol_misuse::fields,
    attitudes::fields,
    drugs::fields,
    education::fields,
    emotional_wellbeing::fields,
    financial_management::fields,
    lifestyle_associates::fields,
    new_sections::fields,
    offence_analysis::fields,
    predictors::fields,
    relationships::fields,
    thinking_behaviours::fields,
];

/// `YES` or `NO` when the field has that answer.
fn yes_no(ap: &AnswersProvider, field: Field) -> Mapped {
    Ok(when_value!(ap, ap.answer(field)?.value()?, {
        YES => text("YES"),
        NO => text("NO"),
        _ => NULL,
    }))
}

/// The single answer to the field as-is.
fn value_of(ap: &AnswersProvider, field: Field) -> Mapped {
    Ok(optional_text(ap.answer(field)?.value()?))
}

/// `YES` or `NO` for whether a checkbox field has the option ticked, or null when unanswered.
fn ticked(ap: &AnswersProvider, field: Field, option: Value) -> Mapped {
    Ok(match ap.answer(field)?.values()? {
        Some(values) if ap.contains(&values, option)? => text("YES"),
        Some(_) => text("NO"),
        None => NULL,
    })
}

/// The practitioner analysis questions at the end of each section.
struct PractitionerAnalysis(&'static str);

impl PractitionerAnalysis {
    fn field(&self, suffix: &str) -> Result<Field> {
        let name = format!("{}_PRACTITIONER_ANALYSIS_{suffix}", self.0);
        Field::from_name(&name).ok_or_else(|| Error::Internal(format!("No field named {name}")))
    }

    fn details(&self, ap: &AnswersProvider, suffix: &str) -> Result<Option<String>> {
        ap.answer(self.field(suffix)?)?.value()
    }

    fn labelled_notes(
        &self,
        ap: &AnswersProvider,
        area: &str,
        yes_label: &str,
        no_label: &str,
    ) -> Result<Option<String>> {
        let yes_details = self.details(ap, &format!("{area}_YES_DETAILS"))?;
        let no_details = self.details(ap, &format!("{area}_NO_DETAILS"))?;

        Ok(match (not_blank(yes_details), not_blank(no_details)) {
            (Some(details), _) => Some(format!("{yes_label} - {details}")),
            (None, Some(details)) => Some(format!("{no_label} - {details}")),
            (None, None) => None,
        })
    }

    fn notes(&self, ap: &AnswersProvider) -> Mapped {
        let notes = [
            self.labelled_notes(
                ap,
                "STRENGTHS_OR_PROTECTIVE_FACTORS",
                "Strengths and protective factor notes",
                "Area not linked to strengths and positive factors notes",
            )?,
            self.labelled_notes(
                ap,
                "RISK_OF_SERIOUS_HARM",
                "Area linked to serious harm notes",
                "Area not linked to serious harm notes",
            )?,
            self.labelled_notes(
                ap,
                "RISK_OF_REOFFENDING",
                "Risk of reoffending notes",
                "Area not linked to reoffending notes",
            )?,
        ];
        let joined = notes.into_iter().flatten().collect::<Vec<_>>().join("\n");
        Ok(if joined.is_empty() { NULL } else { text(&joined) })
    }

    fn risk_of_serious_harm(&self, ap: &AnswersProvider) -> Mapped {
        yes_no(ap, self.field("RISK_OF_SERIOUS_HARM")?)
    }

    fn risk_of_reoffending(&self, ap: &AnswersProvider) -> Mapped {
        yes_no(ap, self.field("RISK_OF_REOFFENDING")?)
    }

    fn strengths_or_protective_factors(&self, ap: &AnswersProvider) -> Mapped {
        yes_no(ap, self.field("STRENGTHS_OR_PROTECTIVE_FACTORS")?)
    }
}

fn not_blank(value: Option<String>) -> Option<String> {
    value.filter(|value| !value.trim().is_empty())
}

#[cfg(test)]
pub(crate) mod tests {
    use serde_json::json;

    use super::*;
    use crate::{
        aggregate::AssessmentAggregate,
        domain::san::{
            formconfig::{FieldConfig, FieldType, FormConfig},
            oasys::answers::tests::field,
        },
        model,
    };

    /// A form config with every field the v1 mapping reads, accepting every option, so each
    /// test only has to set the answers it cares about.
    pub fn full_config() -> FormConfig {
        let options: Vec<&str> = Value::ALL_NAMES.to_vec();
        let checkboxes = [
            Field::DRUGS_INJECTED,
            Field::EDUCATION_DIFFICULTIES,
            Field::FINANCE_INCOME,
            Field::LIVING_WITH,
            Field::OFFENCE_ANALYSIS_ELEMENTS,
            Field::OFFENCE_ANALYSIS_MOTIVATIONS,
            Field::PERSONAL_RELATIONSHIPS_COMMUNITY_IMPORTANT_PEOPLE,
        ];
        let mut fields: Vec<(String, FieldConfig)> = Field::ALL_NAMES
            .iter()
            .map(|name| {
                let field_value = Field::from_name(name).unwrap();
                let field_type = if checkboxes.contains(&field_value) || name.starts_with("DRUGS_INJECTED_") {
                    FieldType::CHECKBOX
                } else {
                    FieldType::RADIO
                };
                (name.to_lowercase(), field(&name.to_lowercase(), field_type, &options))
            })
            .collect();
        let mut victims = field("victims", FieldType::TEXT, &[]);
        victims.collection = "OFFENCE_ANALYSIS_VICTIM".to_owned();
        fields.push(("victim_collection_field".to_owned(), victims));

        FormConfig {
            version: "v1.0".to_owned(),
            fields: fields.into_iter().collect(),
        }
    }

    pub fn answer(assessment: &mut AssessmentAggregate, field: Field, value: model::Value) {
        assessment.answers.insert(field.lower(), value);
    }

    #[test]
    fn practitioner_analysis_notes_prefer_yes_details() {
        let mut assessment = AssessmentAggregate::default();
        answer(
            &mut assessment,
            Field::FINANCE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_YES_DETAILS,
            model::Value::single("harm"),
        );
        answer(
            &mut assessment,
            Field::FINANCE_PRACTITIONER_ANALYSIS_RISK_OF_SERIOUS_HARM_NO_DETAILS,
            model::Value::single("no harm"),
        );
        answer(
            &mut assessment,
            Field::FINANCE_PRACTITIONER_ANALYSIS_RISK_OF_REOFFENDING_NO_DETAILS,
            model::Value::single("  "),
        );
        answer(
            &mut assessment,
            Field::FINANCE_PRACTITIONER_ANALYSIS_STRENGTHS_OR_PROTECTIVE_FACTORS_NO_DETAILS,
            model::Value::single("none"),
        );
        let config = full_config();
        let ap = AnswersProvider::new(&assessment, &config);

        assert_eq!(
            PractitionerAnalysis("FINANCE").notes(&ap).unwrap(),
            json!(
                "Area not linked to strengths and positive factors notes - none\nArea linked to serious harm notes - harm"
            )
        );
        assert_eq!(PractitionerAnalysis("ACCOMMODATION").notes(&ap).unwrap(), NULL);
    }
}
