# Deploy QLHS

Frontend trên Vercel, API trên Render, MySQL trên Aiven Free. Làm đúng thứ tự: database → API → giao diện → gắn CORS.

Local không đổi: `SPRING_PROFILES_ACTIVE=local` vẫn dùng MySQL máy bạn và seed demo trong `be/src/main/resources/db/dev`.

## Việc đã chuẩn bị trong code

- Profile `prod` không chạy seed demo (tài khoản `admin` / `password` chỉ có ở local).
- Lần khởi động đầu, nếu có `APP_ADMIN_USERNAME` và `APP_ADMIN_PASSWORD`, API tự tạo admin.
- API nghe cổng `PORT` do Render cấp.
- JDBC production bắt buộc `sslMode=REQUIRED` (Aiven từ chối kết nối không SSL).
- Pool kết nối tối đa 5, phù hợp gói Aiven Free.
- Swagger tắt trên production. Health check: `GET /actuator/health`.
- Log production chỉ ra console (Render đọc được).
- CORS nhận origin Vercel, kể cả pattern `https://*.vercel.app`.
- Frontend build trên Vercel bằng Nitro preset `vercel`. Cookie đăng nhập đặt cờ `Secure` khi chạy HTTPS.

## 0. Đẩy code lên Git

Render và Vercel đều deploy từ Git. Tạo repo (GitHub hoặc GitLab), commit các thay đổi production, rồi push. Không commit file `.env`.

Trên PowerShell, tạo khóa JWT (dài hơn 32 ký tự) và mật khẩu admin. Lưu lại, sẽ dán vào Render:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

## 1. Aiven MySQL Free

1. Đăng ký tại [https://aiven.io](https://aiven.io).
2. **Create service** → **MySQL** → plan **Free**.
3. Cloud region gần Việt Nam nếu có (Singapore). Tên service ví dụ `qlhs-mysql`.
4. Đợi trạng thái **Running**.
5. Mở service → **Networking** / **IP filtering** → thêm `0.0.0.0/0`.

   Render Free không có IP ra cố định. Nếu không mở dải này, API sẽ lỗi `Communications link failure`. Database vẫn được bảo vệ bằng mật khẩu và SSL. Không dùng mật khẩu yếu.
6. Tab **Overview** (hoặc **Connection information**), ghi lại:

   | Mục | Ví dụ |
   |-----|--------|
   | Host | `qlhs-mysql-xxx.aivencloud.com` |
   | Port | số cổng riêng, không phải lúc nào cũng `3306` |
   | User | `avnadmin` |
   | Password | mật khẩu Aiven cấp |
   | Database | `defaultdb` |

   Dùng `defaultdb` cho gói Free. Không cần tạo database `qlhs`.
7. Ghép URL JDBC (một dòng, không xuống dòng, không khoảng trắng):

```text
jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&tcpKeepAlive=true
```

Thay `HOST` và `PORT` bằng giá trị thật. Giữ nguyên `sslMode=REQUIRED`.

## 2. Render (API)

1. Đăng ký [https://render.com](https://render.com), kết nối Git.
2. **New** → **Blueprint** và chọn repo (file `render.yaml` ở thư mục gốc), hoặc **New Web Service** thủ công với các mục sau:

   | Mục | Giá trị |
   |-----|---------|
   | Root Directory | `be` |
   | Runtime | Docker |
   | Dockerfile | `be/Dockerfile` |
   | Region | Singapore |
   | Instance type | Free |
   | Health Check Path | `/actuator/health` |

3. **Environment** — dán đúng các biến. Không thêm dấu ngoặc, không thêm dấu `/` cuối URL.

   | Biến | Giá trị |
   |------|---------|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `APP_MAIL_ENABLED` | `false` |
   | `SPRING_DATASOURCE_URL` | URL JDBC ở bước 1 |
   | `SPRING_DATASOURCE_USERNAME` | `avnadmin` |
   | `SPRING_DATASOURCE_PASSWORD` | mật khẩu Aiven |
   | `JWT_SIGNER_KEY` | chuỗi vừa tạo, tối thiểu 32 ký tự |
   | `APP_ADMIN_USERNAME` | ví dụ `admin` |
   | `APP_ADMIN_PASSWORD` | mật khẩu mới, tối thiểu 8 ký tự, không dùng `password` |
   | `APP_ADMIN_EMAIL` | email của bạn |
   | `APP_ADMIN_FULL_NAME` | `Quản trị hệ thống` |
   | `CORS_ALLOWED_ORIGINS` | tạm `https://tam.vercel.app`, sửa lại sau khi có URL Vercel |
   | `APP_FRONTEND_URL` | cùng origin Vercel, không có `/` cuối |
   | `FRONTEND_RESET_PASSWORD_URL` | `https://ten-app.vercel.app/auth/reset-password` |

   Khi muốn cả URL preview của Vercel gọi được API, thêm:

   | Biến | Giá trị |
   |------|---------|
   | `CORS_ALLOWED_ORIGIN_PATTERNS` | `https://*.vercel.app` |

4. Deploy. Lần đầu mất khoảng 5–10 phút (tải Gradle, build image).
5. Mở **Logs**. Khi thành công sẽ thấy Flyway chạy `V1`, `V3`, `V4` (không có `V2`) và dòng `Đã tạo tài khoản admin`.
6. Mở `https://<tên-service>.onrender.com/actuator/health`. Kết quả là `{"status":"UP"}`.

Không đặt `SERVER_PORT`. Render tự cấp `PORT`.

Sau khi đăng nhập được, vào Render → Environment → xóa `APP_ADMIN_PASSWORD` rồi save. Lần deploy sau không tạo lại user và không còn mật khẩu trong biến môi trường.

## 3. Vercel (giao diện)

1. Đăng ký [https://vercel.com](https://vercel.com), **Add New Project**, chọn cùng repo.
2. **Root Directory**: `fe`.
3. Framework Preset: **Nuxt.js**. Node.js 20.
4. Environment Variable:

   | Biến | Giá trị |
   |------|---------|
   | `NUXT_PUBLIC_API_BASE` | `https://<tên-service>.onrender.com` |

   Không có dấu `/` cuối. Bật cho **Production**. Nếu dùng Preview, bật luôn cho Preview.
5. Deploy. URL sẽ dạng `https://qlhs-xxxx.vercel.app`.
6. Quay lại Render, sửa:

   - `CORS_ALLOWED_ORIGINS` = đúng URL Vercel (ví dụ `https://qlhs-xxxx.vercel.app`)
   - `APP_FRONTEND_URL` = cùng URL đó
   - `FRONTEND_RESET_PASSWORD_URL` = URL đó + `/auth/reset-password`

   Save để Render deploy lại.
7. Mở site Vercel, đăng nhập bằng `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`.
8. Vào mục đổi mật khẩu và đổi ngay.

Nếu trình duyệt báo lỗi CORS, origin trên Render và URL trên thanh địa chỉ phải giống nhau từng ký tự (`https`, không `www`, không dấu `/` cuối).

## 4. Kiểm tra

- `https://<api>.onrender.com/actuator/health` → `UP`.
- `https://<api>.onrender.com/swagger-ui.html` → không mở được (đã tắt).
- Đăng nhập trên Vercel, tạo một năm học và một lớp.
- Tải lại trang: vẫn đăng nhập.
- Đợi khoảng 15 phút không dùng, bấm lại: request đầu có thể mất 30–60 giây vì Render Free ngủ. Đó là giới hạn gói Free, không phải lỗi app.

## 5. Khi deploy lỗi

| Triệu chứng | Cách xử lý |
|-------------|------------|
| `Communications link failure` | Chưa mở IP `0.0.0.0/0` trên Aiven, hoặc sai host/port. |
| `Access denied for user` | Sai user/password, hoặc database trong URL không phải `defaultdb`. |
| `SSL connection required` / `Connections using insecure transport` | URL JDBC thiếu `sslMode=REQUIRED`. |
| `Could not resolve placeholder 'SPRING_DATASOURCE_URL'` | Thiếu biến trên Render, hoặc chưa đặt `SPRING_PROFILES_ACTIVE=prod`. |
| `JWT_SIGNER_KEY` / signer key | Khóa ngắn hơn 32 ký tự hoặc để trống. |
| Health `DOWN` | Xem log Flyway. Schema lỗi thì API không nhận traffic. |
| Login trên Vercel báo không kết nối / CORS | `NUXT_PUBLIC_API_BASE` sai, hoặc `CORS_ALLOWED_ORIGINS` chưa đúng URL Vercel. Sửa env rồi **redeploy cả hai** (Vercel cần build lại vì `NUXT_PUBLIC_API_BASE` được gắn lúc build). |
| Build Render hết RAM | Đổi instance sang Starter. App chạy được trên Free nhưng build Docker đôi khi cần nhiều RAM hơn. |
| Đã có admin mà muốn tạo lại | Đổi `APP_ADMIN_USERNAME` sang tên chưa tồn tại, hoặc xóa user cũ trong Aiven rồi deploy lại. |

Xem log SQL/schema trên Render. Aiven Console có tab **Logs** và **Query** nếu cần kiểm tra bảng `flyway_schema_history`.

## 6. Giới hạn gói Free

- Render Free ngủ sau khoảng 15 phút không có request. Lượt đầu sau đó chậm.
- Aiven Free giới hạn dung lượng và số kết nối. App để pool tối đa 5.
- Vercel Hobby đủ cho Nuxt SSR của project này.
- Gói Free của Aiven thường không có backup tự động. Trước khi thử migration mới, xuất dữ liệu từ console Aiven.

## 7. Cập nhật bản sau

Push lên nhánh đang nối với Render và Vercel. Cả hai tự build lại.

Migration mới đặt trong `be/src/main/resources/db/migration` với số phiên bản lớn hơn `V4` (ví dụ `V5__...sql`). Không sửa file `V1`–`V4` đã chạy, vì Flyway sẽ từ chối checksum khác. Seed demo chỉ để trong `db/dev`.
