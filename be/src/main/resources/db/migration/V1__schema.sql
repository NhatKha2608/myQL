CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    username        VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_username (username),
    INDEX idx_users_role (role),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE academic_years (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_academic_years_name (name),
    CONSTRAINT chk_academic_years_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classrooms (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    grade_level          INT NOT NULL,
    academic_year_id     BIGINT NOT NULL,
    homeroom_teacher_id  BIGINT NOT NULL,
    description          TEXT,
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted           TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_classrooms_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_classrooms_teacher FOREIGN KEY (homeroom_teacher_id) REFERENCES users(id),
    INDEX idx_classrooms_teacher (homeroom_teacher_id),
    INDEX idx_classrooms_year (academic_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE students (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_code    VARCHAR(50) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    date_of_birth   DATE,
    gender          VARCHAR(20),
    avatar_url      VARCHAR(500),
    address         VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_students_code (student_code),
    INDEX idx_students_name (full_name),
    INDEX idx_students_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classroom_students (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id    BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    enrolled_at     DATE NOT NULL,
    left_at         DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cs_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_cs_student FOREIGN KEY (student_id) REFERENCES students(id),
    INDEX idx_cs_classroom (classroom_id),
    INDEX idx_cs_student (student_id),
    INDEX idx_cs_active (classroom_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendance_records (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id     BIGINT NOT NULL,
    student_id       BIGINT NOT NULL,
    attendance_date  DATE NOT NULL,
    status           VARCHAR(30) NOT NULL,
    note             VARCHAR(500),
    recorded_by      BIGINT NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_att_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_att_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_att_user FOREIGN KEY (recorded_by) REFERENCES users(id),
    UNIQUE KEY uk_attendance (classroom_id, student_id, attendance_date),
    INDEX idx_att_date (classroom_id, attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE score_rules (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id    BIGINT NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    point_value     INT NOT NULL,
    category        VARCHAR(50),
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sr_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_sr_user FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_sr_classroom (classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE score_transactions (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id            BIGINT NOT NULL,
    student_id              BIGINT NOT NULL,
    score_rule_id           BIGINT,
    points_delta            INT NOT NULL,
    reason                  VARCHAR(500) NOT NULL,
    note                    VARCHAR(500),
    transaction_date        DATE NOT NULL,
    created_by              BIGINT NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reversed_transaction_id BIGINT,
    CONSTRAINT fk_st_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_st_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_st_rule FOREIGN KEY (score_rule_id) REFERENCES score_rules(id),
    CONSTRAINT fk_st_user FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_st_reversed FOREIGN KEY (reversed_transaction_id) REFERENCES score_transactions(id),
    INDEX idx_st_classroom_student (classroom_id, student_id),
    INDEX idx_st_date (transaction_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subjects (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_active   TINYINT(1) NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_subjects_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE academic_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id    BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    subject_id      BIGINT NOT NULL,
    evaluation_period VARCHAR(50) NOT NULL,
    evaluation_type VARCHAR(50) NOT NULL,
    score_value     DECIMAL(5,2),
    grade_level_text VARCHAR(50),
    comment_text    TEXT,
    evaluated_at    DATE NOT NULL,
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ar_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_ar_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_ar_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_ar_user FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_ar_student (student_id),
    INDEX idx_ar_classroom (classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE timetable_entries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id    BIGINT NOT NULL,
    subject_id      BIGINT NOT NULL,
    day_of_week     TINYINT NOT NULL,
    period_number   INT NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    room            VARCHAR(100),
    note            VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tt_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_tt_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    INDEX idx_tt_classroom (classroom_id, day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE announcements (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id    BIGINT NOT NULL,
    title           VARCHAR(255) NOT NULL,
    content         TEXT NOT NULL,
    published_at    TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ann_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_ann_user FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_ann_classroom (classroom_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT,
    action_type     VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       BIGINT,
    detail          TEXT,
    success         TINYINT(1) NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token_hash      VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    used_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_prt_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token_hash      VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    revoked_at      TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_rt_hash (token_hash),
    INDEX idx_rt_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
