# QLHS — Quản lý lớp học tiểu học

Hệ thống full-stack cho giáo viên chủ nhiệm: điểm danh, thi đua, học sinh, thời khóa biểu, thông báo và báo cáo.

## Cấu trúc

- `be/` — Spring Boot 3.4, Java 17, MySQL, Flyway, JWT
- `fe/` — Nuxt 4, Vue 3, TypeScript, Tailwind, Pinia
- `docs/ERD.md` — sơ đồ quan hệ database

## Yêu cầu

- JDK 22 (cùng toolchain với `sos-api`; Maven cần JDK, không chỉ JRE)
- Node.js 20+
- MySQL 8.x

## Chạy local

### Database

```sql
CREATE DATABASE qlhs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'qlhs'@'%' IDENTIFIED BY 'your_password';
GRANT ALL ON qlhs.* TO 'qlhs'@'%';
```

### Backend

Cấu hình theo cùng mô hình **`sos-api`**: `application.properties` + profile **`local`** (`application-local.properties`).

Mặc định local: MySQL `root` / `12345678`, database `qlhs` (tự tạo nếu chưa có), port `8080`, bind `0.0.0.0`.

```powershell
cd be
copy .env.example .env
.\gradlew.bat bootRun
```

Build & test: `.\gradlew.bat build` hoặc `.\gradlew.bat test`

Production: `SPRING_PROFILES_ACTIVE=prod` và đặt `SPRING_DATASOURCE_*`, `JWT_SIGNER_KEY` (không dùng file local).

API: `http://localhost:8080`  
Swagger: `http://localhost:8080/swagger-ui.html`

### Frontend

```powershell
cd fe
copy .env.example .env
npm install
npm run dev
```

UI: `http://localhost:3000`

### Tài khoản demo (Flyway V2, môi trường dev)

| Tài khoản | Mật khẩu   | Vai trò  |
|-----------|------------|----------|
| `admin`   | `password` | ADMIN    |
| `gvl01`   | `password` | TEACHER  |

> Hash BCrypt trong seed tương ứng mật khẩu `password`. Đổi mật khẩu trước khi triển khai production.

## Docker Compose (MySQL)

```powershell
docker compose up -d
```

Xem `docker-compose.yml` — không commit mật khẩu thật vào Git.

## Kiểm thử

```powershell
cd be
.\gradlew.bat test
```

```powershell
cd fe
npm run build
```

## Triển khai production

Frontend Vercel, backend Render, MySQL Aiven. Làm lần lượt theo [DEPLOY.md](DEPLOY.md).

Production không nạp seed demo (`db/dev`). Tài khoản admin được tạo từ `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` ở lần khởi động đầu.
