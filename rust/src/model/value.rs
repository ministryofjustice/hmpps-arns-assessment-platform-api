use indexmap::IndexMap;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// An answer or property value.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(tag = "type")]
pub enum Value {
    #[serde(rename = "Single")]
    Single { value: String },
    #[serde(rename = "Multi")]
    Multi { values: Vec<String> },
}

impl Value {
    pub fn single(value: impl Into<String>) -> Self {
        Self::Single { value: value.into() }
    }

    pub fn multi<I: IntoIterator<Item = S>, S: Into<String>>(values: I) -> Self {
        Self::Multi {
            values: values.into_iter().map(Into::into).collect(),
        }
    }
}

/// Answers and properties, keyed by field code and kept in insertion order.
pub type Answers = IndexMap<String, Value>;

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn uses_the_kotlin_type_names() {
        assert_eq!(
            serde_json::to_value(Value::single("YES")).unwrap(),
            json!({"type": "Single", "value": "YES"})
        );
        assert_eq!(
            serde_json::from_value::<Value>(json!({"type": "Multi", "values": ["A", "B"]})).unwrap(),
            Value::multi(["A", "B"])
        );
    }
}
