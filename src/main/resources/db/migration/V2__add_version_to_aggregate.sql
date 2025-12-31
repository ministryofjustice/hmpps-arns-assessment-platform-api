ALTER TABLE aggregate ADD COLUMN version BIGINT;

UPDATE aggregate a
SET version = ranked.version
    FROM (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY assessment_uuid
            ORDER BY events_to, id
        ) - 1 AS version
    FROM aggregate
) ranked
WHERE a.id = ranked.id
  AND a.version IS NULL;

ALTER TABLE aggregate ALTER COLUMN version SET NOT NULL;
ALTER TABLE aggregate ADD CONSTRAINT aggregate_version_check CHECK (version >= 0);
