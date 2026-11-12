use std::{cell::RefCell, sync::LazyLock};

use regex::Regex;

use super::codes::{Collection, Field, Value};
use crate::{
    aggregate::AssessmentAggregate,
    domain::san::formconfig::{FieldType, FormConfig},
    error::{Error, Result},
    model::{self, Answers},
};

static HYPHENS: LazyLock<Regex> = LazyLock::new(|| Regex::new("[–—-]+").unwrap());
static APOSTROPHES: LazyLock<Regex> = LazyLock::new(|| Regex::new("[`‘’]|&#96;").unwrap());
static QUOTES: LazyLock<Regex> = LazyLock::new(|| Regex::new("[“”]").unwrap());

fn invalid_mapping(message: String) -> Error {
    Error::Internal(message)
}

/// An answer read from the assessment, shaped by the field's type in the form config.
#[derive(Debug, PartialEq)]
pub enum Answer {
    Single(Option<String>),
    Multiple(Option<Vec<String>>),
    Collection(Vec<Answers>),
}

impl Answer {
    fn kind(&self) -> &'static str {
        match self {
            Self::Single(_) => "SingleValueAnswer",
            Self::Multiple(_) => "MultipleValuesAnswer",
            Self::Collection(_) => "CollectionAnswer",
        }
    }

    pub fn value(self) -> Result<Option<String>> {
        match self {
            Self::Single(value) => Ok(value),
            other => Err(invalid_mapping(format!(
                "Invalid use of '.value' on a {}",
                other.kind()
            ))),
        }
    }

    pub fn values(self) -> Result<Option<Vec<String>>> {
        match self {
            Self::Multiple(values) => Ok(values),
            other => Err(invalid_mapping(format!(
                "Invalid use of '.values' on a {}",
                other.kind()
            ))),
        }
    }

    pub fn collection(self) -> Result<Vec<Answers>> {
        match self {
            Self::Collection(items) => Ok(items),
            other => Err(invalid_mapping(format!(
                "Invalid use of '.collection' on a {}",
                other.kind()
            ))),
        }
    }
}

/// Reads answers for the mapping, validating every field and option against the form config.
///
/// Option lookups ([`AnswersProvider::get`]) are validated against the field most recently read,
/// so mappings must read a field before comparing its options, as the Kotlin mapping did.
pub struct AnswersProvider<'a> {
    assessment: &'a AssessmentAggregate,
    config: &'a FormConfig,
    context: RefCell<Option<String>>,
}

impl<'a> AnswersProvider<'a> {
    pub fn new(assessment: &'a AssessmentAggregate, config: &'a FormConfig) -> Self {
        Self {
            assessment,
            config,
            context: RefCell::new(None),
        }
    }

    pub fn set_context(&self, field: Field) {
        *self.context.borrow_mut() = Some(field.lower());
    }

    pub fn answer(&self, field: Field) -> Result<Answer> {
        self.set_context(field);
        let key = field.lower();
        let field_config = self.config.fields.get(&key).ok_or_else(|| {
            invalid_mapping(format!(
                "Field {key} does not exist in form config version {}",
                self.config.version
            ))
        })?;

        let answer = self
            .assessment
            .answers
            .get(&field_config.code)
            .or_else(|| self.assessment.properties.get(&field_config.code));

        Ok(match field_config.field_type {
            FieldType::CHECKBOX => Answer::Multiple(match answer {
                Some(model::Value::Multi { values }) => {
                    let values: Vec<String> = values.iter().map(|value| normalise_for_oasys(value)).collect();
                    Some(if values == [""] { Vec::new() } else { values })
                }
                _ => None,
            }),
            _ => Answer::Single(match answer {
                Some(model::Value::Single { value }) => Some(normalise_for_oasys(value)),
                _ => None,
            }),
        })
    }

    pub fn answer_collection(&self, collection: Collection) -> Result<Answer> {
        *self.context.borrow_mut() = None;

        if !self
            .config
            .fields
            .values()
            .any(|field| field.collection == collection.name())
        {
            return Err(invalid_mapping(format!(
                "Collection {} does not exist in form config version {}",
                collection.name(),
                self.config.version
            )));
        }

        Ok(Answer::Collection(
            self.assessment
                .collections
                .iter()
                .find(|candidate| candidate.name == collection.name())
                .map(|found| found.items.iter().map(|item| item.answers.clone()).collect())
                .unwrap_or_default(),
        ))
    }

    /// The option's name, provided it is an option of the field most recently read.
    pub fn get(&self, value: Value) -> Result<String> {
        let context = self.context.borrow().clone().ok_or_else(|| {
            invalid_mapping("Cannot obtain values without a field context. Call answer() first".to_owned())
        })?;
        let name = value.name();

        let is_option = self
            .config
            .fields
            .get(&context)
            .is_some_and(|field| field.options.iter().any(|option| option.value.as_deref() == Some(name)));
        if !is_option {
            return Err(invalid_mapping(format!(
                "{name} is not a valid option for field {context} in form config version {}",
                self.config.version
            )));
        }

        Ok(name.to_owned())
    }

    /// Whether `subject` is the given option, validating the option first.
    pub fn is(&self, subject: &Option<String>, value: Value) -> Result<bool> {
        let option = self.get(value)?;
        Ok(subject.as_deref() == Some(option.as_str()))
    }

    /// Whether `values` contains the given option, validating the option first.
    pub fn contains(&self, values: &[String], value: Value) -> Result<bool> {
        let option = self.get(value)?;
        Ok(values.contains(&option))
    }
}

/// Matches the HMPO form wizard default formatters used by the original SAN service. The UI
/// stores backticks HTML-encoded, so the encoded form is matched too.
fn normalise_for_oasys(value: &str) -> String {
    let value = HYPHENS.replace_all(value.trim(), "-");
    let value = APOSTROPHES.replace_all(&value, "'");
    QUOTES.replace_all(&value, "\"").into_owned()
}

#[cfg(test)]
pub(crate) mod tests {
    use super::*;
    use crate::domain::san::formconfig::{FieldConfig, FieldOption};

    pub fn field(code: &str, field_type: FieldType, options: &[&str]) -> FieldConfig {
        FieldConfig {
            code: code.to_owned(),
            options: options
                .iter()
                .map(|value| FieldOption {
                    value: Some((*value).to_owned()),
                })
                .collect(),
            field_type,
            section: String::new(),
            collection: String::new(),
        }
    }

    pub fn config(fields: Vec<(Field, FieldConfig)>) -> FormConfig {
        FormConfig {
            version: "v1.0".to_owned(),
            fields: fields
                .into_iter()
                .map(|(field, config)| (field.lower(), config))
                .collect(),
        }
    }

    #[test]
    fn reads_and_normalises_answers() {
        let mut assessment = AssessmentAggregate::default();
        assessment
            .answers
            .insert("details".to_owned(), model::Value::single("  It’s a “test” — really  "));
        assessment
            .properties
            .insert("multi".to_owned(), model::Value::multi([""]));
        let config = config(vec![
            (Field::OTHER_DRUG_NAME, field("details", FieldType::TEXT, &[])),
            (Field::DRUGS_INJECTED, field("multi", FieldType::CHECKBOX, &[])),
        ]);
        let ap = AnswersProvider::new(&assessment, &config);

        assert_eq!(
            ap.answer(Field::OTHER_DRUG_NAME).unwrap().value().unwrap().as_deref(),
            Some("It's a \"test\" - really")
        );
        assert_eq!(
            ap.answer(Field::DRUGS_INJECTED).unwrap().values().unwrap(),
            Some(vec![])
        );
        assert!(ap.answer(Field::DRUGS_INJECTED).unwrap().value().is_err());
    }

    #[test]
    fn validates_fields_and_options_against_the_config() {
        let assessment = AssessmentAggregate::default();
        let config = config(vec![(
            Field::DRUG_USE,
            field("drug_use", FieldType::RADIO, &["YES", "NO"]),
        )]);
        let ap = AnswersProvider::new(&assessment, &config);

        assert!(ap.get(Value::YES).is_err(), "no field has been read yet");
        assert!(ap.answer(Field::ALCOHOL_USE).is_err());

        ap.answer(Field::DRUG_USE).unwrap();
        assert_eq!(ap.get(Value::YES).unwrap(), "YES");
        let error = ap.get(Value::SOMETIMES).unwrap_err();
        assert_eq!(
            error.to_string(),
            "SOMETIMES is not a valid option for field drug_use in form config version v1.0"
        );
    }
}
