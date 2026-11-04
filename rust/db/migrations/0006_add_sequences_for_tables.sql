------ EVENT ------

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

------ TIMELINE ------

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

------ AGGREGATE ------

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

------ USER_DETAILS ------

ALTER TABLE user_details
    ALTER COLUMN id DROP IDENTITY IF EXISTS;

CREATE SEQUENCE user_details_sequence
    START WITH 1
    INCREMENT BY 100;

ALTER SEQUENCE user_details_sequence
    OWNED BY user_details.id;

ALTER TABLE user_details
    ALTER COLUMN id SET DEFAULT nextval('user_details_sequence');

SELECT setval(
               'user_details_sequence',
               COALESCE((SELECT MAX(id) FROM user_details), 0) + 1,
               false
       );

------ ASSESSMENT_IDENTIFIER ------

ALTER TABLE assessment_identifier
    ALTER COLUMN id DROP IDENTITY IF EXISTS;

CREATE SEQUENCE assessment_identifier_sequence
    START WITH 1
    INCREMENT BY 100;

ALTER SEQUENCE assessment_identifier_sequence
    OWNED BY assessment_identifier.id;

ALTER TABLE assessment_identifier
    ALTER COLUMN id SET DEFAULT nextval('assessment_identifier_sequence');

SELECT setval(
               'assessment_identifier_sequence',
               COALESCE((SELECT MAX(id) FROM assessment_identifier), 0) + 1,
               false
       );

------ ASSESSMENT ------

ALTER TABLE assessment
    ALTER COLUMN id DROP IDENTITY IF EXISTS;

CREATE SEQUENCE assessment_sequence
    START WITH 1
    INCREMENT BY 100;

ALTER SEQUENCE assessment_sequence
    OWNED BY assessment.id;

ALTER TABLE assessment
    ALTER COLUMN id SET DEFAULT nextval('assessment_sequence');

SELECT setval(
               'assessment_sequence',
               COALESCE((SELECT MAX(id) FROM assessment), 0) + 1,
               false
       );
