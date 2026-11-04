use crate::{
    domain::san::oasys::{FieldsToMap, answers::AnswersProvider},
    error::Result,
};

/// Nothing is mapped for predictors yet.
pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(Vec::new())
}
