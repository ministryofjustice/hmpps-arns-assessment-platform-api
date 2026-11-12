//! Maps a SAN assessment to the equivalent OASys question answers, for the OASys integration.

pub mod answers;
pub mod codes;
mod v1;

use serde_json::Value as JsonValue;

use self::answers::AnswersProvider;
use crate::{
    aggregate::AssessmentAggregate,
    domain::san::formconfig::FormConfig,
    error::{Error, Result},
    model::JsonMap,
};

/// The OASys answer for one question, or `null` when there isn't one.
type Mapped = Result<JsonValue>;
type MappingFn = Box<dyn Fn(&AnswersProvider) -> Mapped>;
/// OASys question codes and how to answer them, in output order.
type FieldsToMap = Vec<(String, MappingFn)>;
/// A section of the mapping. Fallible because listing the fields may read answers.
type Section = fn(&AnswersProvider) -> Result<FieldsToMap>;

/// Build [`FieldsToMap`] from `"question" => function or closure` pairs.
macro_rules! fields {
    ($($question:expr => $mapping:expr),* $(,)?) => {
        vec![$(($question.to_owned(), $crate::domain::san::oasys::mapping($mapping))),*]
    };
}
use fields;

/// Boxes a mapping. Taking it through a generic `Fn` bound lets closures infer a signature that
/// works for any [`AnswersProvider`].
fn mapping<F>(mapping: F) -> MappingFn
where
    F: Fn(&AnswersProvider) -> Mapped + 'static,
{
    Box::new(mapping)
}

/// The equivalent of a Kotlin `when (subject) { ap.get(A), ap.get(B) -> x ... else -> y }`.
///
/// Options are validated lazily and in order, stopping at the first match, against whichever
/// field was read most recently. That is what made the Kotlin mapping fail (or not) for options
/// missing from a form config, so it has to be preserved.
macro_rules! when_value {
    ($ap:expr, $subject:expr, { $($($option:ident)|+ => $result:expr,)* _ => $default:expr $(,)? }) => {{
        let subject: Option<String> = $subject;
        'when_value: {
            $(
                if false $(|| $ap.is(&subject, $crate::domain::san::oasys::codes::Value::$option)?)+ {
                    break 'when_value ($result);
                }
            )*
            $default
        }
    }};
}
use when_value;

fn text(value: &str) -> JsonValue {
    JsonValue::String(value.to_owned())
}

fn optional_text(value: Option<String>) -> JsonValue {
    value.map_or(JsonValue::Null, JsonValue::String)
}

const NULL: JsonValue = JsonValue::Null;

fn sections(form_version: &str) -> Result<&'static [Section]> {
    match form_version {
        "v1.0" => Ok(v1::SECTIONS),
        _ => Err(Error::Internal(format!(
            "No data mapping found for form version {form_version}"
        ))),
    }
}

/// The OASys equivalent of the assessment, keyed by OASys question code.
pub fn oasys_equivalent(assessment: &AssessmentAggregate, config: &FormConfig) -> Result<JsonMap> {
    let ap = AnswersProvider::new(assessment, config);
    let mut equivalent = JsonMap::new();

    for section in sections(&config.version)? {
        for (question, mapping) in section(&ap)? {
            equivalent.insert(question, mapping(&ap)?);
        }
    }

    Ok(equivalent)
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::{
        codes::{Field, Value},
        v1::tests::{answer, full_config},
        *,
    };
    use crate::model::{self, Answers, collection::tests::collection, collection::tests::item};

    fn single(value: &Value) -> model::Value {
        model::Value::single(value.name())
    }

    #[test]
    fn maps_every_section_in_order_including_unanswered_questions() {
        let equivalent = oasys_equivalent(&AssessmentAggregate::default(), &full_config()).unwrap();

        let keys: Vec<&str> = equivalent.keys().map(String::as_str).collect();
        assert_eq!(&keys[..3], ["o3-3", "o3-4", "o3-5"]);
        assert_eq!(keys.last(), Some(&"o11-99"));
        assert_eq!(equivalent["o3-3"], NULL);
        assert_eq!(equivalent["oAC_SAN_SECTION_COMP"], json!("NO"));
        assert_eq!(equivalent["o8-4"], json!("0"));
        assert_eq!(equivalent["o6-8"], json!("3"));
    }

    #[test]
    fn scores_alcohol_use() {
        let mut assessment = AssessmentAggregate::default();
        answer(
            &mut assessment,
            Field::ALCOHOL_USE,
            single(&Value::YES_WITHIN_LAST_THREE_MONTHS),
        );
        answer(
            &mut assessment,
            Field::ALCOHOL_FREQUENCY,
            single(&Value::MORE_THAN_4_TIMES_A_WEEK),
        );
        answer(&mut assessment, Field::ALCOHOL_UNITS, single(&Value::UNITS_10_OR_MORE));

        let equivalent = oasys_equivalent(&assessment, &full_config()).unwrap();

        assert_eq!(equivalent["o9-1"], json!("2"));
        assert_eq!(
            equivalent["o9-1-t"],
            json!("Drinks more than four times a week and consumes 10 or more units a day, when they drink.")
        );
        assert_eq!(equivalent["o9-2"], NULL);
    }

    #[test]
    fn maps_each_victim_and_checkbox_answers() {
        let mut assessment = AssessmentAggregate::default();
        answer(
            &mut assessment,
            Field::OFFENCE_ANALYSIS_ELEMENTS,
            model::Value::multi([Value::WEAPON.name(), Value::VICTIM_TARGETED.name()]),
        );
        let mut victim = item(uuid::Uuid::new_v4(), vec![]);
        victim.answers = Answers::from([
            (
                Field::OFFENCE_ANALYSIS_VICTIM_AGE.lower(),
                single(&Value::AGE_18_TO_20_YEARS),
            ),
            (
                Field::OFFENCE_ANALYSIS_VICTIM_RELATIONSHIP.lower(),
                single(&Value::STRANGER),
            ),
        ]);
        assessment.collections.push(collection(
            uuid::Uuid::new_v4(),
            "OFFENCE_ANALYSIS_VICTIM",
            vec![victim],
        ));

        let equivalent = oasys_equivalent(&assessment, &full_config()).unwrap();

        assert_eq!(equivalent["o2-2_V2_WEAPON"], json!("YES"));
        assert_eq!(equivalent["o2-2_V2_ARSON"], json!("NO"));
        assert_eq!(equivalent["o2-3"], json!("DIRECTCONT,STRANGERS"));
        assert_eq!(
            equivalent["victim0"],
            json!({"oAGE_OF_VICTIM_ELM": "4", "oGENDER_ELM": null, "oETHNIC_CATEGORY_ELM": null, "oVICTIM_RELATION_ELM": "0"})
        );
        let keys: Vec<&String> = equivalent.keys().collect();
        let victim_position = keys.iter().position(|key| *key == "victim0").unwrap();
        assert_eq!(
            keys[victim_position - 1],
            "o2-99",
            "victims follow the offence analysis questions"
        );
    }

    #[test]
    fn fails_for_options_missing_from_the_form_config_only_when_they_are_checked() {
        let mut config = full_config();
        config
            .fields
            .get_mut(&Field::TYPE_OF_TEMPORARY_ACCOMMODATION.lower())
            .unwrap()
            .options
            .retain(|option| option.value.as_deref() != Some("SHORT_TERM"));

        // SHORT_TERM is only checked for temporary accommodation...
        let mut no_accommodation = AssessmentAggregate::default();
        answer(
            &mut no_accommodation,
            Field::CURRENT_ACCOMMODATION,
            single(&Value::NO_ACCOMMODATION),
        );
        assert_eq!(
            oasys_equivalent(&no_accommodation, &config).unwrap()["o3-5"],
            json!("2")
        );

        // ...so only then does its absence from the config fail the mapping, as it did in Kotlin.
        let mut temporary = AssessmentAggregate::default();
        answer(&mut temporary, Field::CURRENT_ACCOMMODATION, single(&Value::TEMPORARY));
        let error = oasys_equivalent(&temporary, &config).unwrap_err();
        assert_eq!(
            error.to_string(),
            "SHORT_TERM is not a valid option for field type_of_temporary_accommodation in form config version v1.0"
        );
    }

    #[test]
    fn fails_for_unknown_form_versions() {
        let mut config = full_config();
        config.version = "v2.0".to_owned();

        let error = oasys_equivalent(&AssessmentAggregate::default(), &config).unwrap_err();
        assert_eq!(error.to_string(), "No data mapping found for form version v2.0");
    }
}
