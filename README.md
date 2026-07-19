# DiaCare

Đồ án quản lý phòng khám tiểu đường bằng Java 17, Spring Boot, JSP và PostgreSQL.

## Chức năng

- Quản trị tài khoản và hồ sơ bệnh nhân.
- Phân quyền Admin, Staff, Doctor và Patient.
- Lịch hẹn, đổi/hủy lịch, vắng hẹn, check-in và hàng đợi.
- Bệnh án, chỉ số lâm sàng, xét nghiệm và đơn thuốc cơ bản.
- Nhật ký sức khỏe của bệnh nhân; bác sĩ được phân công có thể xem.

## Chạy local

Tạo duy nhất file `.env` ở thư mục gốc:

```env
SERVER_PORT=8082
DB_URL=jdbc:postgresql://YOUR_HOST:YOUR_PORT/diabetes_medical_record?sslmode=require
DB_USERNAME=avnadmin
DB_PASSWORD=YOUR_DATABASE_PASSWORD
DB_POOL_SIZE=5
DEVICE_API_KEY=replace-with-a-long-random-value
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=Admin@123
BOOTSTRAP_ADMIN_NAME=System Administrator
MAIL_USERNAME=
MAIL_PASSWORD=
OPENAI_API_KEY=
OPENAI_MODEL=gpt-5.6-terra
OPENAI_TIMEOUT_SECONDS=20
UPLOAD_DIR=./uploads
```

```powershell
cd D:\SWP\diabetes-medical-record
.\mvnw.cmd spring-boot:run
```

Mở [http://localhost:8082](http://localhost:8082). Spring Boot tự đọc `.env`; Flyway tự cập nhật database từ `src/main/resources/db/migration`.

## Deploy Render

Repository đã có `Dockerfile` và `render.yaml`. Trên Render cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `BOOTSTRAP_ADMIN_PASSWORD` và `DEVICE_API_KEY`.

Để bật tư vấn sức khỏe hằng ngày, thêm `OPENAI_API_KEY` trong Environment của Render. Nếu để trống, hệ thống vẫn hoạt động và dùng bộ quy tắc an toàn tại máy chủ.

## Tư vấn sức khỏe AI và quyền riêng tư

- Chỉ gọi AI khi bệnh nhân chủ động tích xác nhận và nhấn nút nhận tư vấn.
- Dữ liệu gửi đi đã được tối thiểu hóa: nhóm tuổi, loại tiểu đường, nhóm điều trị, mục tiêu HbA1c và thống kê chỉ số gần đây.
- Không gửi họ tên, ngày sinh chính xác, số điện thoại, email, địa chỉ, CCCD, BHYT, username, mã bệnh nhân, tên/liều thuốc hoặc ghi chú thô.
- Request dùng `store: false`; lời khuyên cùng ngữ cảnh được cache trong ngày để giảm thời gian chờ và số lần gọi API.
- AI chỉ hỗ trợ giáo dục sức khỏe, không chẩn đoán và không tự thay đổi thuốc. Quy tắc an toàn của hệ thống luôn có quyền ưu tiên hơn nội dung do AI sinh.

## Cấu trúc

```text
src/main/java/controllers/       Servlet tương thích, chia theo từng màn hình
src/main/java/dal/               Truy vấn JDBC thuần
src/main/java/models/            Model dùng chung
src/main/java/vn/diabetes/auth/  Spring MVC đăng nhập hoàn chỉnh
src/main/java/vn/diabetes/config/Bridge tạm cho DAO legacy dùng Hikari
src/main/java/vn/diabetes/service/Service nghiệp vụ đang được migrate
src/main/java/vn/diabetes/validation/Quy tắc validate dùng chung
src/main/resources/db/migration/ Flyway schema/seed
src/main/webapp/views/           JSP
src/main/webapp/static/          CSS/JavaScript
src/test/java/                   Unit test service, validation và auth
```

`.env`, `target/` và `uploads/` không được đưa lên GitHub.
