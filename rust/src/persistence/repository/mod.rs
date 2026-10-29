//! Database access, one module per table. Every function takes the connection to run on, so the
//! caller decides the transaction.

pub mod aggregate;
pub mod assessment;
pub mod event;
pub mod timeline;
pub mod user_details;

use std::str::FromStr;

use crate::error::{Error, Result};

/// Parse an enum stored as text.
fn parse_column<T>(value: &str) -> Result<T>
where
    T: FromStr,
    T::Err: std::fmt::Display,
{
    value.parse().map_err(|err: T::Err| Error::Internal(err.to_string()))
}
