ALTER TABLE classroom_students
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

UPDATE classroom_students
SET sort_order = id;
