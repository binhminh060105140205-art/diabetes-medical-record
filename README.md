# Diabetes Medical Record — Spring Boot migration

> Production target is now **Aiven PostgreSQL + Render Docker**. Follow `DEPLOY.md`. The SQL Server instructions and files below are retained only for local legacy-data migration.

Project hợp nhất ba snapshot NetBeans/Tomcat thành một Spring Boot monolith chạy Java 21 và embedded Tomcat.

## Trạng thái hiện tại

- Baseline được chọn: `projectnew/projectnew/Claude1` (50 Java, 23 JSP), vì đây là superset của `FIX3` (45 Java) và snapshot `Claude1` còn lại (46 Java).
- Đã hợp nhất model, DAO, servlet, JSP, CSS/JS và các SQL migration vào một Maven project.
- Luồng `/Login` đã migrate hoàn chỉnh sang Spring MVC theo `LoginController -> AuthenticationService -> UserRepository`.
- 21 endpoint còn lại đang chạy qua compatibility layer `@ServletComponentScan` trên embedded Tomcat. Đây là chủ ý để giữ nguyên chức năng trong lúc migrate từng lát dọc, không phải kiến trúc đích.
- JSP được giữ tạm thời nên artifact là executable WAR. Có thể chạy bằng `java -jar`; không cần cài Tomcat ngoài.

## Chạy trong VS Code

Yêu cầu: JDK 21 (LTS) và Extension Pack for Java. Sao chép `.env.example` thành `.env`, điền thông tin SQL Server, rồi đặt các biến môi trường tương ứng trong terminal.

```powershell
$env:DB_URL="jdbc:sqlserver://localhost;databaseName=DiabetesMedicalRecordDB;encrypt=true;trustServerCertificate=true"
$env:DB_USERNAME="sa"
$env:DB_PASSWORD="your-password"
./mvnw.cmd clean package
java -jar target/diabetes-medical-record.war
```

Mở `http://localhost:8080/`. Không commit `.env`, mật khẩu database hoặc API key.

## Cấu trúc

```text
src/main/java/
  vn/diabetes/                  # Spring Boot application và code đã migrate
    auth/                       # controller/service/repository mẫu
  controllers/                 # servlet legacy, migrate lần lượt
  dal/                         # JDBC DAO legacy, migrate lần lượt
  models/                      # domain model dùng chung
  util/, viewmodels/
src/main/webapp/
  views/                       # JSP giữ tạm để bảo toàn UI
  static/                      # CSS/JS
database/                      # script SQL từ các snapshot
```

## Khởi tạo database

Script chuẩn để đồng bộ database với code hiện tại là `database/integrate-legacy-database.sql`.

- Nếu đã có database cũ và dữ liệu thật: chỉ chạy `integrate-legacy-database.sql`. Script giữ dữ liệu, thêm bảng/cột còn thiếu và chuẩn hóa các mã role/status.
- Nếu tạo môi trường demo mới: chạy `full-schema-and-seed.sql`, sau đó chạy `integrate-legacy-database.sql`.
- Script tích hợp có transaction, có thể chạy lại, và cuối script trả danh sách 11 bảng với `is_ready = 1`.

Script export chứa đường dẫn file `.mdf/.ldf` theo máy đã tạo bản backup (`MSSQL15.MSSQLSERVER01`). Trước khi chạy trên SQL Server khác, sửa hai giá trị `FILENAME` ở đầu file cho đúng thư mục DATA của instance hiện tại, hoặc tạo database thủ công rồi chạy phần bắt đầu từ `USE [DiabetesMedicalRecordDB]`.

Không chạy `upgrade_v3.sql` vì script legacy đó xóa bảng `AIWarnings` trong khi code hợp nhất vẫn sử dụng bảng này. `post-full-schema-compatibility.sql`, `migration.sql`, `upgrade_v3.sql` và `AddDataCCCD.sql` chỉ giữ để tham khảo lịch sử; `integrate-legacy-database.sql` đã thay thế các bước này. `SQLQuery1.sql` là dataset cũ; không nạp cùng dữ liệu mẫu trong `full-schema-and-seed.sql`.

## Chiến lược merge tránh conflict

1. Không chép đè ba source tree. Lập inventory theo đường dẫn tương đối và endpoint.
2. Chọn bản superset 50 file làm baseline; file chỉ xuất hiện ở snapshot khác được thêm có chủ đích.
3. Với class trùng tên, so sánh theo hành vi: URL mapping, SQL, thuộc tính request/session và JSP đích. Một class canonical duy nhất được giữ; phần logic khác biệt được đưa vào service, không đổi tên thành `Foo2`.
4. Với DAO trùng, gom query theo aggregate (`User`, `Patient`, `MedicalRecord`). Một query chỉ có một repository sở hữu. Transaction nhiều bảng đặt ở service với `@Transactional`.
5. Migrate theo lát dọc và test URL cũ sau mỗi lát; URL/form field/session key được giữ cho tới khi UI đã chuyển xong.

## Mapping Servlet/JSP sang Spring Boot

| Legacy | Đích | Quy tắc |
|---|---|---|
| `HttpServlet#doGet` | `@GetMapping` | trả view hoặc JSON |
| `HttpServlet#doPost` | `@PostMapping` | bind `@RequestParam`/DTO, validate tại biên |
| `request.setAttribute` | `Model.addAttribute` | giữ nguyên tên attribute trong giai đoạn JSP |
| `sendRedirect` | `redirect:/...` | giữ nguyên URL public |
| `RequestDispatcher.forward` | view name hoặc `forward:` | không gọi servlet API từ service |
| `new XxxDAO()` | constructor injection | controller chỉ gọi service |
| JDBC thủ công | `JdbcClient` repository | dùng DataSource/Hikari của Spring |
| `HttpSession` | `HttpSession` ở controller | giữ key `user`, sau đó có thể nâng cấp Spring Security |
| JSP | Thymeleaf | chuyển sau khi controller/service ổn định |
| endpoint thiết bị | `@RestController` | JSON, status code rõ ràng |

## Thứ tự migrate thực tế

1. **Auth** — đã làm: `/Login`; tiếp theo `/Logout` và interceptor kiểm tra role.
2. **Patient** — `PatientList`, `PatientForm`, `PatientHistory`, `PatientDashboard`; tạo `PatientService` và `PatientRepository`. Tác vụ tạo `Users` + `Patients` phải có `@Transactional`.
3. **Medical record** — `MedicalRecordForm`, `RecordDetail`, dashboard bác sĩ.
4. **Device data** — chuyển upload thành `@RestController`, DTO và HTTP status; giữ URL `/api/device-data/upload`.
5. **Admin/doctor profile** — file upload lưu ngoài source tree qua `UPLOAD_DIR`.
6. **AI advice** — API key chỉ đọc từ biến môi trường/server; không nhận hoặc render key ở browser.
7. Sau mỗi nhóm controller, bỏ `@WebServlet` tương ứng. Khi không còn servlet legacy, xóa `@ServletComponentScan`.
8. Chuyển JSP từng trang sang `src/main/resources/templates/*.html` bằng Thymeleaf. Khi hết JSP, đổi packaging `war` thành `jar` và bỏ Jasper/JSTL.

## Session và authentication

Trong giai đoạn tương thích, session key `user` và timeout 30 phút được giữ nguyên. Role check cần gom vào một `HandlerInterceptor`, thay vì lặp trong từng controller. Không lưu password/API key trong session. Database hiện dùng password dạng plain text để tương thích dữ liệu cũ; bước nâng cấp an toàn là thêm BCrypt, migrate khi user đăng nhập thành công, rồi xóa so sánh plain text sau khi tất cả tài khoản đã đổi.

## Checklist nghiệm thu mỗi lát dọc

- [ ] URL và HTTP method cũ vẫn hoạt động.
- [ ] Form field, request attribute và session key không đổi ngoài kế hoạch.
- [ ] Controller không tạo DAO trực tiếp.
- [ ] Business rule nằm trong service; SQL chỉ nằm trong repository.
- [ ] Multi-table write có `@Transactional` và rollback test.
- [ ] Kiểm tra quyền cho ADMIN/STAFF/DOCTOR/PATIENT.
- [ ] Test success, validation error, unauthorized, record-not-found và database error.
- [ ] Không có secret trong source/log/HTML.
- [ ] `./mvnw.cmd test` và `./mvnw.cmd clean package` thành công.
- [ ] Smoke test các dashboard và file upload trên embedded server.

## Quyết định đơn giản hóa

Không tách microservice, không đưa JPA vào giữa chừng, không đổi schema khi chưa cần. `JdbcClient` phù hợp nhất vì SQL legacy đã rõ và giảm rủi ro thay đổi hành vi. Thymeleaf được migrate sau controller/service để tránh cùng lúc sửa cả backend lẫn toàn bộ giao diện.
