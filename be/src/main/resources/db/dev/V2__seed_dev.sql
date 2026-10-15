-- Dữ liệu demo (chỉ dùng môi trường dev). Mật khẩu đăng nhập: password (BCrypt)
INSERT INTO academic_years (name, start_date, end_date, status)
VALUES ('2025-2026', '2025-09-05', '2026-05-31', 'ACTIVE');

INSERT INTO users (full_name, email, username, password_hash, role, status, is_deleted)
VALUES
('Quản trị hệ thống', 'admin@qlhs.local', 'admin',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 'ACTIVE', 0),
('Nguyễn Thị Lan', 'lan.nguyen@qlhs.local', 'gvl01',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TEACHER', 'ACTIVE', 0);

INSERT INTO classrooms (name, grade_level, academic_year_id, homeroom_teacher_id, description, status, is_deleted)
VALUES ('3A1', 3, 1, 2, 'Lớp 3A1', 'ACTIVE', 0);

INSERT INTO students (student_code, full_name, date_of_birth, gender, status, is_deleted)
VALUES
('HS2025001', 'Trần Minh An', '2017-03-12', 'MALE', 'ACTIVE', 0),
('HS2025002', 'Lê Bảo Ngọc', '2017-07-22', 'FEMALE', 'ACTIVE', 0),
('HS2025003', 'Phạm Đức Huy', '2017-01-05', 'MALE', 'ACTIVE', 0);

INSERT INTO classroom_students (classroom_id, student_id, enrolled_at, status)
VALUES (1, 1, '2025-09-05', 'ACTIVE'), (1, 2, '2025-09-05', 'ACTIVE'), (1, 3, '2025-09-05', 'ACTIVE');

INSERT INTO subjects (code, name, description, is_active)
VALUES
('TOAN', 'Toán', 'Môn Toán', 1),
('TIENGVIET', 'Tiếng Việt', 'Môn Tiếng Việt', 1),
('KHOAHOC', 'Khoa học', 'Môn Khoa học', 1);

INSERT INTO score_rules (classroom_id, name, description, point_value, category, is_active, created_by)
VALUES
(1, 'Giúp bạn học tập', 'Chủ động giúp bạn trong giờ học', 2, 'POSITIVE', 1, 2),
(1, 'Vi phạm nội quy', 'Không tuân thủ nội quy lớp', -2, 'NEGATIVE', 1, 2);
