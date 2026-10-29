use serde::{Deserialize, Serialize};
use sqlx::PgConnection;
use utoipa::ToSchema;

use super::{formconfig::FormConfig, oasys};
use crate::{
    aggregate::AggregateType,
    clock::Clock,
    command::Command,
    error::Result,
    event::{AssessmentPropertiesUpdatedEvent, Event, bus},
    model::{Answers, Value},
    persistence::{PersistenceContext, entity::EventEntity},
};

/// Store the OASys equivalent of the assessment's answers in its `oasys_equivalent` property.
#[derive(Clone, Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct UpdateOasysDataMapping {
    pub form_config: FormConfig,
}

pub async fn update_oasys_data_mapping(
    hook: &UpdateOasysDataMapping,
    command: &Command,
    context: &mut PersistenceContext,
    conn: &mut PgConnection,
    clock: &Clock,
) -> Result<()> {
    let assessment = context.find_assessment(conn, command.assessment_uuid()).await?;
    let state = context
        .aggregate_state_mut(conn, clock, &assessment, AggregateType::AssessmentAggregate, None)
        .await?;
    let oasys_equivalent = oasys::oasys_equivalent(state.latest().data.assessment(), &hook.form_config)?;

    let user = context.find_user_details(conn, command.user()).await?;
    let event = EventEntity::new(
        user,
        assessment,
        Event::AssessmentPropertiesUpdatedEvent(AssessmentPropertiesUpdatedEvent {
            added: Answers::from([(
                "oasys_equivalent".to_owned(),
                Value::single(serde_json::to_string(&oasys_equivalent)?),
            )]),
            removed: Vec::new(),
        }),
        clock.request_date_time(),
        command.autosaved(),
    );

    // The mapping isn't something the user did, so it isn't added to the timeline.
    bus::handle(context, conn, clock, event).await?;
    Ok(())
}
