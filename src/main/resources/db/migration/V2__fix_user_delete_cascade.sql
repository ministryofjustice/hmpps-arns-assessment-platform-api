ALTER TABLE timeline
    DROP CONSTRAINT fk_timeline_user_details,
    ADD CONSTRAINT fk_timeline_user_details
        FOREIGN KEY (user_details_uuid)
            REFERENCES user_details (uuid)
            ON DELETE RESTRICT;
