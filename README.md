# DiaCare

Đồ án quản lý phòng khám tiểu đường sử dụng Java 17, Spring Boot, JSP và Microsoft SQL Server.

## Chức năng chính

- Quản trị tài khoản và phân quyền ADMIN, STAFF, DOCTOR, PATIENT.
- Quản lý bệnh nhân, lịch hẹn, tiếp nhận và hàng đợi khám.
- Hồ sơ bệnh án, chỉ số sức khỏe, xét nghiệm và đơn thuốc.
- Nhật ký sức khỏe, cảnh báo và tư vấn hỗ trợ bệnh nhân.

## Yêu cầu

- JDK 17.
- Microsoft SQL Server 2019 trở lên.
- Maven Wrapper đã có sẵn trong dự án.

## Cấu hình SQL Server local

Tạo database và tài khoản SQL Server cho ứng dụng:

```sql
CREATE DATABASE SWP_DiabetesMedicalRecordDB;
GO

CREATE LOGIN swp_app
WITH PASSWORD = 'CHANGE_ME_STRONG_PASSWORD',
     CHECK_POLICY = ON,
     CHECK_EXPIRATION = OFF;
GO

USE SWP_DiabetesMedicalRecordDB;
CREATE USER swp_app FOR LOGIN swp_app WITH DEFAULT_SCHEMA = dbo;
ALTER ROLE db_datareader ADD MEMBER swp_app;
ALTER ROLE db_datawriter ADD MEMBER swp_app;
ALTER ROLE db_ddladmin ADD MEMBER swp_app;
GRANT VIEW DEFINITION TO swp_app;
GO
```

Sao chép file cấu hình mẫu:

```powershell
Copy-Item .env.example .env
```

Sau đó sửa `DB_PASSWORD` trong `.env` cho khớp với mật khẩu SQL Server vừa tạo. File `.env` đã được Git ignore và không được commit.

Flyway tự tạo bảng, khóa, constraint và index từ:

```text
src/main/resources/db/migration-sqlserver
```
... 
.env :
SERVER_PORT=8082
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=SWP_DiabetesMedicalRecordDB;encrypt=true;trustServerCertificate=true;loginTimeout=5
DB_USERNAME=swp_app
DB_PASSWORD=SwpLocal@123
DB_DRIVER=com.microsoft.sqlserver.jdbc.SQLServerDriver
DB_POOL_SIZE=5
DB_POOL_MIN_IDLE=1
FLYWAY_ENABLED=true
FLYWAY_LOCATIONS=classpath:db/migration-sqlserver
BOOTSTRAP_ADMIN_ENABLED=false
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=Admin@123
BOOTSTRAP_ADMIN_NAME=Quản trị hệ thống
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_STARTTLS=true
MAIL_FROM_NAME=DiaCare
OPENAI_API_KEY=
OPENAI_MODEL=gpt-5.6-terra
OPENAI_TIMEOUT_SECONDS=20
UPLOAD_DIR=./uploads
...
application.properties :
spring.application.name=diabetes-medical-record
spring.config.import=optional:file:.env[.properties]
server.port=8082
server.servlet.session.timeout=30m
server.servlet.session.tracking-modes=cookie
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
server.servlet.session.cookie.secure=${SESSION_COOKIE_SECURE:false}
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true
spring.web.locale=vi_VN
spring.web.locale-resolver=fixed
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
spring.datasource.url=${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=SWP_DiabetesMedicalRecordDB;encrypt=true;trustServerCertificate=true;loginTimeout=5}
spring.datasource.username=${DB_USERNAME:swp_app}
spring.datasource.password=${DB_PASSWORD:SwpLocal@123}
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=3000
spring.datasource.hikari.validation-timeout=3000
spring.datasource.hikari.data-source-properties.sendStringParametersAsUnicode=true
spring.jdbc.template.query-timeout=5s
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration-sqlserver
spring.flyway.baseline-on-migrate=false
spring.flyway.validate-on-migrate=true
app.upload-dir=./uploads
app.openai.api-key=${OPENAI_API_KEY:}
app.openai.model=${OPENAI_MODEL:gpt-5.6-terra}
app.openai.base-url=${OPENAI_BASE_URL:https://api.openai.com/v1/responses}
app.openai.timeout-seconds=${OPENAI_TIMEOUT_SECONDS:20}
## Chạy ứng dụng

```powershell
.\mvnw.cmd spring-boot:run
```

Mở ứng dụng tại http://localhost:8082.

## Kiểm tra dự án

Chạy unit test:

```powershell
.\mvnw.cmd clean test
```

Build đầy đủ:

```powershell
.\mvnw.cmd clean verify
```

## Email

Để gửi email bằng Gmail, cấu hình `MAIL_USERNAME` và `MAIL_PASSWORD` bằng App Password. Không sử dụng mật khẩu Gmail thông thường.

## Tư vấn AI

Cấu hình `OPENAI_API_KEY` nếu muốn sử dụng tư vấn AI. Khi không có khóa API, ứng dụng vẫn dùng bộ quy tắc cục bộ.

## Upload

File tải lên được lưu trong thư mục `uploads`. Thư mục này được Git ignore để tránh đưa dữ liệu người dùng lên repository.
