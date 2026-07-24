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

## Chạy ứng dụng

```powershell
.\mvnw.cmd spring-boot:run
```

Mở ứng dụng tại http://localhost:8082.

- `/health`: xác nhận ứng dụng đã khởi động.
- `/ready`: xác nhận ứng dụng kết nối được SQL Server.

## Kiểm tra dự án

Chạy unit test:

```powershell
.\mvnw.cmd clean test
```

Build đầy đủ và precompile JSP:

```powershell
.\mvnw.cmd -Pprecompile-jsp clean verify
```

## Email

Để gửi email bằng Gmail, cấu hình `MAIL_USERNAME` và `MAIL_PASSWORD` bằng App Password. Không sử dụng mật khẩu Gmail thông thường.

## Tư vấn AI

Cấu hình `OPENAI_API_KEY` nếu muốn sử dụng tư vấn AI. Khi không có khóa API, ứng dụng vẫn dùng bộ quy tắc cục bộ.

## Upload

File tải lên được lưu trong thư mục `uploads`. Thư mục này được Git ignore để tránh đưa dữ liệu người dùng lên repository.
