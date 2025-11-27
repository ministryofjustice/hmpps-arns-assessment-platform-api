ALTER TABLE event ADD COLUMN parent_uuid UUID NULL;
CREATE INDEX idx_event_parent_uuid ON event(parent_uuid);
ALTER TABLE event ADD CONSTRAINT fk_event_parent FOREIGN KEY (parent_uuid) REFERENCES event (uuid) ON DELETE CASCADE;
