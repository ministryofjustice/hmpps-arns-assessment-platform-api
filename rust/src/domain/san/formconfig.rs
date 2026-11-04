use indexmap::IndexMap;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// The parts of a SAN form config the OASys mapping needs. Anything else in it is ignored.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
pub struct FormConfig {
    pub version: String,
    #[serde(default)]
    pub fields: IndexMap<String, FieldConfig>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
pub struct FieldConfig {
    pub code: String,
    #[serde(default)]
    pub options: Vec<FieldOption>,
    #[serde(default, rename = "type")]
    pub field_type: FieldType,
    #[serde(default)]
    pub section: String,
    #[serde(default)]
    pub collection: String,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize, ToSchema)]
pub struct FieldOption {
    #[serde(default)]
    pub value: Option<String>,
}

#[allow(clippy::upper_case_acronyms)]
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
pub enum FieldType {
    RADIO,
    CHECKBOX,
    #[default]
    TEXT,
    DATE,
    SELECT,
}
