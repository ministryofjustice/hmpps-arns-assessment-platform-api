UPDATE event child
    SET created_at = parent.created_at
    FROM event parent
    WHERE child.parent_uuid = parent.uuid;

ALTER TABLE event DROP COLUMN parent_uuid;

DELETE FROM event WHERE data_type = 'GroupEvent';
