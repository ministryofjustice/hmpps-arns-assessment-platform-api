ALTER TABLE event
    ALTER COLUMN id DROP IDENTITY IF EXISTS;

CREATE SEQUENCE event_sequence
    START WITH 1
    INCREMENT BY 100;

ALTER SEQUENCE event_sequence
    OWNED BY event.id;

ALTER TABLE event
    ALTER COLUMN id SET DEFAULT nextval('event_sequence');

SELECT setval(
               'event_sequence',
               COALESCE((SELECT MAX(id) FROM event), 0) + 1,
               false
       );

ALTER TABLE timeline
    ALTER COLUMN id DROP IDENTITY IF EXISTS;

CREATE SEQUENCE timeline_sequence
    START WITH 1
    INCREMENT BY 100;

ALTER SEQUENCE timeline_sequence
    OWNED BY timeline.id;

ALTER TABLE timeline
    ALTER COLUMN id SET DEFAULT nextval('timeline_sequence');

SELECT setval(
               'timeline_sequence',
               COALESCE((SELECT MAX(id) FROM timeline), 0) + 1,
               false
       );

ALTER TABLE aggregate
    ALTER COLUMN id DROP IDENTITY IF EXISTS;

CREATE SEQUENCE aggregate_sequence
    START WITH 1
    INCREMENT BY 100;

ALTER SEQUENCE aggregate_sequence
    OWNED BY aggregate.id;

ALTER TABLE aggregate
    ALTER COLUMN id SET DEFAULT nextval('aggregate_sequence');

SELECT setval(
               'aggregate_sequence',
               COALESCE((SELECT MAX(id) FROM aggregate), 0) + 1,
               false
       );

