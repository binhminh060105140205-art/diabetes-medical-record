package vn.diabetes.ai;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.NoSuchElementException;
import models.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PatientAdviceController {
    private static final String CONSENT_SESSION_KEY = "patientAiConsent";
    private final PatientAdviceService service;

    public PatientAdviceController(PatientAdviceService service) { this.service = service; }

    @PostMapping("/api/patient/ai-advice")
    public ResponseEntity<?> advice(HttpSession session, @RequestBody(required = false) AdviceRequest request) {
        // Nút trên trang "Sức khỏe hôm nay" đi vào đây: kiểm tra phiên đăng nhập và quyền PATIENT trước khi đọc dữ liệu.
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Vui lòng đăng nhập lại."));
        if (!"PATIENT".equals(user.getRole())) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Chức năng chỉ dành cho bệnh nhân."));
        boolean consented = Boolean.TRUE.equals(session.getAttribute(CONSENT_SESSION_KEY));
        if (request != null && request.consent()) {
            session.setAttribute(CONSENT_SESSION_KEY, true);
            consented = true;
        }
        if (!consented) return ResponseEntity.badRequest().body(Map.of(
                "error", "Bạn cần đồng ý sử dụng dữ liệu sức khỏe đã ẩn danh cho lần đăng nhập này."));
        try {
            // Service lấy snapshot, áp dụng bộ luật an toàn và chỉ gọi nguồn AI khi cấu hình cho phép.
            return ResponseEntity.ok(service.getDailyAdvice(user.getUserId()));
        } catch (NoSuchElementException error) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", error.getMessage()));
        }
    }

    public record AdviceRequest(boolean consent) {}
}
