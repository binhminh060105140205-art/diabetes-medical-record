# DiaCare

Đồ án quản lý phòng khám tiểu đường bằng Java 17, Spring Boot, JSP và PostgreSQL.

## Chức năng

- Quản trị tài khoản và hồ sơ bệnh nhân.
- Phân quyền Quản trị viên, Nhân viên tiếp nhận, Bác sĩ và Bệnh nhân.
- Lịch hẹn, đổi/hủy lịch, vắng hẹn, ghi nhận đến khám và hàng đợi.
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
DB_POOL_MIN_IDLE=0
DB_STATEMENT_TIMEOUT_MS=10000
DEVICE_API_KEY=replace-with-a-long-random-value
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=Admin@123
BOOTSTRAP_ADMIN_NAME=Quản trị hệ thống
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_STARTTLS=true
MAIL_FROM_NAME=DiaCare
SPRING_LAZY_INITIALIZATION=true
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

## Gửi thư điện tử thật

Hệ thống gửi thông tin đăng nhập khi Quản trị viên hoặc Nhân viên tiếp nhận tạo tài khoản. Với Gmail:

1. Bật xác minh hai bước cho tài khoản Google dùng để gửi thư.
2. Tạo Mật khẩu ứng dụng trong phần Bảo mật của tài khoản Google.
3. Gán địa chỉ Gmail vào `MAIL_USERNAME` và Mật khẩu ứng dụng 16 ký tự vào `MAIL_PASSWORD`.
4. Giữ `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587` và `MAIL_STARTTLS=true`.

Không dùng mật khẩu Gmail thông thường. Thông báo trên giao diện chỉ xác nhận thư đã được đưa vào hàng đợi; kết quả gửi thực tế được ghi trong nhật ký máy chủ.

## Deploy Render

Kho mã đã có `Dockerfile` và `render.yaml`. Trên Render cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `BOOTSTRAP_ADMIN_PASSWORD`, `DEVICE_API_KEY`, `MAIL_USERNAME` và `MAIL_PASSWORD`.

Để triển khai nhanh, đặt `Health Check Path` của dịch vụ thành `/health`. Đường dẫn này chỉ xác nhận ứng dụng đã khởi động; `/ready` vẫn được giữ để kiểm tra riêng kết nối PostgreSQL khi cần chẩn đoán. Với dịch vụ Render đã tạo trước đó, kiểm tra lại mục Settings vì thay đổi trong `render.yaml` chỉ có hiệu lực khi Blueprint được đồng bộ.

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
