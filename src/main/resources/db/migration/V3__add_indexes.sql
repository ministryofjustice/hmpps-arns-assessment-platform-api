-- =========================
-- AGGREGATE
-- assessment_uuid AND data->>'type' AND events_to
-- =========================

CREATE INDEX IF NOT EXISTS idx_aggregate_assessment_type_events
    ON aggregate (
                  assessment_uuid,
                  (data ->> 'type'),
                  events_to
        );


-- =========================
-- ASSESSMENT_IDENTIFIER
-- assessment_uuid AND identifier_type AND created_at
-- =========================

CREATE INDEX IF NOT EXISTS idx_ai_assessment_identifier_created
    ON assessment_identifier (
                              assessment_uuid,
                              identifier_type,
                              created_at
        );


-- =========================
-- ASSESSMENT
-- type AND created_at
-- =========================

CREATE INDEX IF NOT EXISTS idx_assessment_type_created
    ON assessment (
                   type,
                   created_at
        );


-- =========================
-- EVENT
-- assessment_uuid AND created_at
-- =========================

CREATE INDEX IF NOT EXISTS idx_event_assessment_created
    ON event (
              assessment_uuid,
              created_at
        );


-- =========================
-- TIMELINE
-- Single-column indexes
-- =========================

CREATE INDEX IF NOT EXISTS idx_timeline_assessment_uuid
    ON timeline (assessment_uuid);

CREATE INDEX IF NOT EXISTS idx_timeline_user_details_uuid
    ON timeline (user_details_uuid);

CREATE INDEX IF NOT EXISTS idx_timeline_created_at
    ON timeline (created_at);

CREATE INDEX IF NOT EXISTS idx_timeline_event_type
    ON timeline (event_type);

CREATE INDEX IF NOT EXISTS idx_timeline_custom_type
    ON timeline (custom_type);
