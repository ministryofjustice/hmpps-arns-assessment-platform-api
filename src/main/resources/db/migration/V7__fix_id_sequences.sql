------ EVENT ------

SELECT setval(
               'event_sequence',
               (SELECT MAX(id) FROM event),
               true
       );

------ TIMELINE ------

SELECT setval(
               'timeline_sequence',
               (SELECT MAX(id) FROM timeline),
               true
       );

------ AGGREGATE ------

SELECT setval(
               'aggregate_sequence',
               (SELECT MAX(id) FROM aggregate),
               true
       );

------ USER_DETAILS ------

SELECT setval(
               'user_details_sequence',
               (SELECT MAX(id) FROM user_details),
               true
       );

------ ASSESSMENT_IDENTIFIER ------

SELECT setval(
               'assessment_identifier_sequence',
               (SELECT MAX(id) FROM assessment_identifier),
               true
       );

------ ASSESSMENT ------

SELECT setval(
               'assessment_sequence',
               (SELECT MAX(id) FROM assessment),
               true
       );
