//! Naive ("local") date-times, matching how the Kotlin service used `java.time.LocalDateTime`.
//!
//! The API exchanges timestamps without an offset, interpreted in the service's time zone
//! (`Europe/London` in every deployed environment), while the database stores `TIMESTAMPTZ`.
//! Conversions between the two always go through [`LocalDateTime::to_utc`] / [`LocalDateTime::from_utc`].

use std::{fmt, str::FromStr, sync::OnceLock};

use chrono::{DateTime, Duration, LocalResult, NaiveDate, NaiveDateTime, NaiveTime, Offset, TimeZone, Timelike, Utc};
use chrono_tz::Tz;
use serde::{Deserialize, Deserializer, Serialize, Serializer};

static ZONE: OnceLock<Tz> = OnceLock::new();

/// Set the zone used to interpret local date-times. Only the first call has any effect.
pub fn init_zone(zone: Tz) {
    let _ = ZONE.set(zone);
}

pub fn zone() -> Tz {
    *ZONE.get_or_init(|| chrono_tz::Europe::London)
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct LocalDateTime(pub NaiveDateTime);

impl LocalDateTime {
    pub fn new(date: NaiveDate, time: NaiveTime) -> Self {
        Self(date.and_time(time))
    }

    pub fn from_utc(instant: DateTime<Utc>) -> Self {
        Self(instant.with_timezone(&zone()).naive_local())
    }

    /// Resolve to an instant the same way `java.time` does: an overlap picks the earlier offset,
    /// and a time inside a gap is moved forward by the length of the gap.
    pub fn to_utc(self) -> DateTime<Utc> {
        let zone = zone();
        match zone.from_local_datetime(&self.0) {
            LocalResult::Single(instant) => instant.with_timezone(&Utc),
            LocalResult::Ambiguous(earlier, _) => earlier.with_timezone(&Utc),
            LocalResult::None => {
                let offset_before_gap = zone
                    .offset_from_utc_datetime(&(self.0 - Duration::days(1)))
                    .fix()
                    .local_minus_utc();
                Utc.from_utc_datetime(&(self.0 - Duration::seconds(offset_before_gap.into())))
            }
        }
    }

    /// PostgreSQL persists timestamps at microsecond precision, rounding fractional
    /// microseconds to the nearest microsecond.
    pub fn to_database_precision(self) -> Self {
        let rounded = self.0 + Duration::nanoseconds(500);
        let micros_only = rounded.nanosecond() / 1_000 * 1_000;
        Self(rounded.with_nanosecond(micros_only).unwrap_or(rounded))
    }

    pub fn start_of_day(date: NaiveDate) -> Self {
        Self::new(date, NaiveTime::MIN)
    }

    /// Equivalent of `LocalDate.atTime(LocalTime.MAX)`.
    pub fn end_of_day(date: NaiveDate) -> Self {
        Self::new(
            date,
            NaiveTime::from_hms_nano_opt(23, 59, 59, 999_999_999).expect("valid time"),
        )
    }

    pub fn minus_days(self, days: i64) -> Self {
        Self(self.0 - Duration::days(days))
    }

    /// Mirrors `LocalDateTime.toString()`, which drops zero seconds and prints the fraction in
    /// groups of three digits. Only used where the Kotlin service stored that representation.
    pub fn to_java_string(self) -> String {
        let mut out = self.0.format("%Y-%m-%dT%H:%M").to_string();
        let (seconds, nanos) = (self.0.second(), self.0.nanosecond());
        if seconds == 0 && nanos == 0 {
            return out;
        }
        out.push_str(&format!(":{seconds:02}"));
        if nanos == 0 {
        } else if nanos % 1_000_000 == 0 {
            out.push_str(&format!(".{:03}", nanos / 1_000_000));
        } else if nanos % 1_000 == 0 {
            out.push_str(&format!(".{:06}", nanos / 1_000));
        } else {
            out.push_str(&format!(".{nanos:09}"));
        }
        out
    }
}

/// ISO-8601 without an offset, with the fraction trimmed of trailing zeros, as Jackson writes it.
impl fmt::Display for LocalDateTime {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0.format("%Y-%m-%dT%H:%M:%S"))?;
        let nanos = self.0.nanosecond();
        if nanos > 0 {
            let fraction = format!("{nanos:09}");
            write!(f, ".{}", fraction.trim_end_matches('0'))?;
        }
        Ok(())
    }
}

#[derive(Debug, thiserror::Error)]
#[error("Invalid date-time '{0}', expected an ISO-8601 local date-time such as 2025-01-01T09:00:00")]
pub struct ParseLocalDateTimeError(String);

impl FromStr for LocalDateTime {
    type Err = ParseLocalDateTimeError;

    /// Accepts what Jackson accepted: optional seconds and fraction, and a trailing `Z` which is ignored.
    fn from_str(value: &str) -> Result<Self, Self::Err> {
        let trimmed = value.strip_suffix('Z').unwrap_or(value);
        trimmed
            .parse::<NaiveDateTime>()
            .or_else(|_| NaiveDateTime::parse_from_str(trimmed, "%Y-%m-%dT%H:%M"))
            .map(Self)
            .map_err(|_| ParseLocalDateTimeError(value.to_owned()))
    }
}

impl Serialize for LocalDateTime {
    fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        serializer.collect_str(self)
    }
}

impl<'de> Deserialize<'de> for LocalDateTime {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        let raw = String::deserialize(deserializer)?;
        raw.parse().map_err(serde::de::Error::custom)
    }
}

impl utoipa::PartialSchema for LocalDateTime {
    fn schema() -> utoipa::openapi::RefOr<utoipa::openapi::schema::Schema> {
        utoipa::openapi::ObjectBuilder::new()
            .schema_type(utoipa::openapi::schema::Type::String)
            .format(Some(utoipa::openapi::SchemaFormat::KnownFormat(
                utoipa::openapi::KnownFormat::DateTime,
            )))
            .description(Some("Local date-time without an offset, e.g. 2025-01-01T09:00:00"))
            .into()
    }
}

impl utoipa::ToSchema for LocalDateTime {
    fn name() -> std::borrow::Cow<'static, str> {
        "LocalDateTime".into()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn ldt(value: &str) -> LocalDateTime {
        value.parse().unwrap()
    }

    #[test]
    fn formats_like_jackson() {
        assert_eq!(ldt("2025-01-01T09:00:00").to_string(), "2025-01-01T09:00:00");
        assert_eq!(ldt("2025-01-01T09:00:00.120").to_string(), "2025-01-01T09:00:00.12");
        assert_eq!(
            ldt("2025-01-01T09:00:00.123456").to_string(),
            "2025-01-01T09:00:00.123456"
        );
    }

    #[test]
    fn parses_optional_seconds_and_trailing_z() {
        assert_eq!(ldt("2025-01-01T09:00"), ldt("2025-01-01T09:00:00"));
        assert_eq!(ldt("2025-01-01T09:00:00Z"), ldt("2025-01-01T09:00:00"));
        assert!("2025-01-01".parse::<LocalDateTime>().is_err());
    }

    #[test]
    fn rounds_to_database_precision() {
        assert_eq!(
            ldt("2025-01-01T09:00:00.000000499").to_database_precision(),
            ldt("2025-01-01T09:00:00")
        );
        assert_eq!(
            ldt("2025-01-01T09:00:00.000000500").to_database_precision(),
            ldt("2025-01-01T09:00:00.000001")
        );
        assert_eq!(
            LocalDateTime::end_of_day(NaiveDate::from_ymd_opt(2025, 1, 1).unwrap()).to_database_precision(),
            ldt("2025-01-02T00:00:00")
        );
    }

    #[test]
    fn converts_using_london_time() {
        // GMT in winter, BST in summer
        assert_eq!(
            ldt("2025-01-01T09:00:00").to_utc().to_rfc3339(),
            "2025-01-01T09:00:00+00:00"
        );
        assert_eq!(
            ldt("2025-07-01T09:00:00").to_utc().to_rfc3339(),
            "2025-07-01T08:00:00+00:00"
        );
        let summer = ldt("2025-07-01T09:00:00");
        assert_eq!(LocalDateTime::from_utc(summer.to_utc()), summer);
    }

    #[test]
    fn resolves_dst_transitions_like_java_time() {
        // Clocks go forward at 01:00 on 30 March 2025, so 01:30 doesn't exist and becomes 02:30 BST
        assert_eq!(
            ldt("2025-03-30T01:30:00").to_utc().to_rfc3339(),
            "2025-03-30T01:30:00+00:00"
        );
        // Clocks go back at 02:00 on 26 October 2025, so 01:30 happens twice and the earlier (BST) wins
        assert_eq!(
            ldt("2025-10-26T01:30:00").to_utc().to_rfc3339(),
            "2025-10-26T00:30:00+00:00"
        );
    }

    #[test]
    fn matches_java_to_string() {
        assert_eq!(ldt("2025-01-01T09:00:00").to_java_string(), "2025-01-01T09:00");
        assert_eq!(ldt("2025-01-01T09:00:05").to_java_string(), "2025-01-01T09:00:05");
        assert_eq!(ldt("2025-01-01T09:00:00.1").to_java_string(), "2025-01-01T09:00:00.100");
        assert_eq!(
            ldt("2025-01-01T09:00:00.123456").to_java_string(),
            "2025-01-01T09:00:00.123456"
        );
    }
}
