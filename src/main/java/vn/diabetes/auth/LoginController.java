package vn.diabetes.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import dal.LoginHistoryDAO;
import models.User;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private final AuthenticationService authentication;

    public LoginController(AuthenticationService authentication) {
        this.authentication = authentication;
    }

    @GetMapping("/Login")
    public String form(HttpSession session, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        User user = (User) session.getAttribute("user");
        return user == null ? "forward:/views/Login.jsp" : redirectFor(user);
    }

    @PostMapping("/Login")
    public String login(@RequestParam(defaultValue = "") String username,
                        @RequestParam(defaultValue = "") String password,
                        HttpServletRequest request,
                        HttpSession session,
                        HttpServletResponse response,
                        Model model) {
        response.setHeader("Cache-Control", "no-store");
        AuthenticationService.LoginResult result;
        try {
            result = authentication.login(username, password);
        } catch (DataAccessException error) {
            LOGGER.log(Level.SEVERE, "Database unavailable during login", error);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            model.addAttribute("err", "Cơ sở dữ liệu đang khởi động hoặc mất kết nối. Vui lòng thử lại sau ít phút.");
            model.addAttribute("username", username);
            return "forward:/views/Login.jsp";
        }
        if (result.successful()) {
            request.changeSessionId();
            session.setAttribute("user", result.user());
            recordLogin(request, session, result.user());
            return redirectFor(result.user());
        }
        model.addAttribute("err", result.error());
        model.addAttribute("username", username);
        if (result.locked()) {
            model.addAttribute("lockUntil", result.lockUntil().toEpochMilli());
        }
        return "forward:/views/Login.jsp";
    }

    private void recordLogin(HttpServletRequest request, HttpSession session, User user) {
        try {
            new LoginHistoryDAO().record(user.getUserId(), user.getUsername(), user.getFullName(),
                    user.getRole(), "LOGIN", clientIp(request), request.getHeader("User-Agent"),
                    session.getId());
        } catch (RuntimeException error) {
            LOGGER.log(Level.WARNING, "Không thể ghi lịch sử đăng nhập", error);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
    }

    private String redirectFor(User user) {
        if (user.getRole() == null) return "redirect:/Login";
        return switch (user.getRole().toUpperCase()) {
            case "ADMIN" -> "redirect:/AdminDashboard";
            case "STAFF" -> "redirect:/StaffDashboard";
            case "DOCTOR" -> "redirect:/DoctorDashboard";
            case "PATIENT" -> "redirect:/PatientDashboard";
            default -> "redirect:/Login";
        };
    }
}
