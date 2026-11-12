//! Aggregates are the current state of an assessment, derived by replaying its events. They are
//! persisted as versioned snapshots so reads don't need to replay the whole event stream.

pub mod assessment;

use indexmap::IndexMap;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

pub use assessment::AssessmentAggregate;

use crate::{clock::Clock, datetime::LocalDateTime};

/// Once an aggregate has had this many events applied, further events go into a new snapshot.
pub const SNAPSHOT_THRESHOLD: i64 = 50;

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub enum AggregateType {
    AssessmentAggregate,
}

impl AggregateType {
    /// Matches the `data_type` column, which is generated from the `type` property of the JSON.
    pub fn name(self) -> &'static str {
        match self {
            Self::AssessmentAggregate => "AssessmentAggregate",
        }
    }

    pub fn blank(self) -> Aggregate {
        match self {
            Self::AssessmentAggregate => Aggregate::AssessmentAggregate(AssessmentAggregate::default()),
        }
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, ToSchema)]
#[serde(tag = "type")]
pub enum Aggregate {
    AssessmentAggregate(AssessmentAggregate),
}

impl Aggregate {
    pub fn aggregate_type(&self) -> AggregateType {
        match self {
            Self::AssessmentAggregate(_) => AggregateType::AssessmentAggregate,
        }
    }

    pub fn assessment(&self) -> &AssessmentAggregate {
        let Self::AssessmentAggregate(assessment) = self;
        assessment
    }

    pub fn assessment_mut(&mut self) -> &mut AssessmentAggregate {
        let Self::AssessmentAggregate(assessment) = self;
        assessment
    }
}

#[derive(Clone, Debug)]
pub struct AggregateEntity {
    pub id: Option<i64>,
    pub uuid: Uuid,
    pub position: Option<i32>,
    /// Optimistic lock version.
    pub version: i64,
    pub updated_at: LocalDateTime,
    pub events_from: LocalDateTime,
    pub events_to: LocalDateTime,
    pub events_applied: i64,
    pub assessment_uuid: Uuid,
    pub data: Aggregate,
}

impl AggregateEntity {
    pub fn new(
        assessment_uuid: Uuid,
        data: Aggregate,
        events_from: LocalDateTime,
        events_to: LocalDateTime,
        updated_at: LocalDateTime,
    ) -> Self {
        Self {
            id: None,
            uuid: Uuid::new_v4(),
            position: None,
            version: 0,
            updated_at,
            events_from,
            events_to,
            events_applied: 0,
            assessment_uuid,
            data,
        }
    }

    /// A new, unsaved snapshot starting from this one's data.
    pub fn snapshot(&self, clock: &Clock) -> Self {
        Self::new(
            self.assessment_uuid,
            self.data.clone(),
            self.events_from,
            self.events_to,
            clock.now(),
        )
    }

    /// Record that an event has been applied.
    pub fn applied(&mut self, events_to: LocalDateTime, clock: &Clock) {
        self.events_to = events_to;
        self.updated_at = clock.now();
        self.events_applied += 1;
    }
}

/// The snapshots of one aggregate type for one assessment that are loaded or created in a unit of
/// work. The last one is the latest.
#[derive(Clone, Debug)]
pub struct AggregateState {
    pub aggregates: Vec<AggregateEntity>,
}

impl AggregateState {
    pub fn new(aggregate: AggregateEntity) -> Self {
        Self {
            aggregates: vec![aggregate],
        }
    }

    pub fn aggregate_type(&self) -> AggregateType {
        self.latest().data.aggregate_type()
    }

    pub fn latest(&self) -> &AggregateEntity {
        self.aggregates.last().expect("aggregate state is never empty")
    }

    /// The snapshot to apply the next event to, starting a new one if the latest is full.
    pub fn for_write(&mut self, clock: &Clock) -> &mut AggregateEntity {
        if self.latest().events_applied >= SNAPSHOT_THRESHOLD {
            let next = self.latest().snapshot(clock);
            self.aggregates.push(next);
        }
        self.aggregates.last_mut().expect("aggregate state is never empty")
    }
}

/// Aggregate state for one assessment, by aggregate type.
pub type State = IndexMap<AggregateType, AggregateState>;

/// Aggregate state for many assessments, by assessment UUID.
pub type StateCollection = IndexMap<Uuid, State>;
