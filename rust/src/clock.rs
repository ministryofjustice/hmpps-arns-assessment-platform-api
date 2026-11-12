use chrono::{Duration, Utc};

use crate::{
    datetime::LocalDateTime,
    error::{Error, Result},
};

/// The clock for a single request. Requests may be backdated with the `backdateTo` query
/// parameter, which shifts the whole clock (including `now()`) back to that point in time.
#[derive(Clone, Copy, Debug)]
pub struct Clock {
    offset: Duration,
    request_date_time: LocalDateTime,
}

impl Clock {
    pub fn system() -> Self {
        Self::with_offset(Duration::zero())
    }

    pub fn backdated_to(backdate_to: LocalDateTime) -> Result<Self> {
        let target = backdate_to.to_database_precision();
        let offset = target.to_utc() - Utc::now();

        if offset > Duration::zero() {
            return Err(Error::invalid_backdate(format!(
                "Invalid backdateTo parameter '{target}' - the clock cannot be moved forward"
            )));
        }

        Ok(Self::with_offset(offset))
    }

    fn with_offset(offset: Duration) -> Self {
        Self {
            offset,
            request_date_time: LocalDateTime::from_utc(Utc::now() + offset).to_database_precision(),
        }
    }

    pub fn now(&self) -> LocalDateTime {
        LocalDateTime::from_utc(Utc::now() + self.offset).to_database_precision()
    }

    /// Fixed for the lifetime of the request, so every event a request creates shares a timestamp.
    pub fn request_date_time(&self) -> LocalDateTime {
        self.request_date_time
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn backdated_clock_runs_from_the_given_time() {
        let target: LocalDateTime = "2025-01-01T09:00:00".parse().unwrap();
        let clock = Clock::backdated_to(target).unwrap();

        assert!((clock.request_date_time().0 - target.0).num_seconds().abs() < 1);
        assert!(clock.now() >= target);
        assert!(clock.now() < target.minus_days(-1));
    }

    #[test]
    fn cannot_move_the_clock_forward() {
        let future = LocalDateTime::from_utc(Utc::now() + Duration::days(1));
        assert!(Clock::backdated_to(future).is_err());
    }
}
