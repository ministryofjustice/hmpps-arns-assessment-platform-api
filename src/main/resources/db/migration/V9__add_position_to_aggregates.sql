ALTER TABLE aggregate ADD COLUMN position INTEGER;

WITH ordered AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY assessment_uuid, data_type
            ORDER BY id
            ) - 1 AS pos
    FROM aggregate
)
UPDATE aggregate a
SET position = o.pos
FROM ordered o
WHERE a.id = o.id;

ALTER TABLE aggregate ALTER COLUMN position SET NOT NULL;

ALTER TABLE aggregate
    ADD CONSTRAINT uq_aggregate_assessment_data_type_position
        UNIQUE (assessment_uuid, data_type, position);
