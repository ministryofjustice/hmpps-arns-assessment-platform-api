ALTER TABLE event ADD COLUMN position INTEGER;
ALTER TABLE timeline ADD COLUMN position INTEGER;

WITH ordered AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY assessment_uuid
            ORDER BY id
            ) - 1 AS pos
    FROM event
)
UPDATE event e
SET position = o.pos
FROM ordered o
WHERE e.id = o.id;

WITH ordered AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY assessment_uuid
            ORDER BY id
            ) - 1 AS pos
    FROM timeline
)
UPDATE timeline t
SET position = o.pos
FROM ordered o
WHERE t.id = o.id;

ALTER TABLE event ALTER COLUMN position SET NOT NULL;
ALTER TABLE timeline ALTER COLUMN position SET NOT NULL;

ALTER TABLE event
    ADD CONSTRAINT uq_event_assessment_position
        UNIQUE (assessment_uuid, position);

ALTER TABLE timeline
    ADD CONSTRAINT uq_timeline_assessment_position
        UNIQUE (assessment_uuid, position);
