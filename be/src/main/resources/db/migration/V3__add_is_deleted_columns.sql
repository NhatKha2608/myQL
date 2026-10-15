ALTER TABLE academic_years
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE classroom_students
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE attendance_records
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE score_rules
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE score_transactions
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE subjects
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE academic_records
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE timetable_entries
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE announcements
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE audit_logs
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE password_reset_tokens
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE refresh_tokens
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;
