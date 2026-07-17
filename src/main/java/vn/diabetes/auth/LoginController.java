package vn.diabetes.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public String form(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user == null ? "forward:/views/Login.jsp" : redirectFor(user);
    }

    @PostMapping("/Login")
    public String login(@RequestParam String username,
                        @RequestParam(defaultValue = "") String password,
                        HttpSession session,
                        HttpServletResponse response,
                        Model model) {
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
            session.setAttribute("user", result.user());
            return redirectFor(result.user());
        }
        model.addAttribute("err", result.error());
        model.addAttribute("username", username);
        return "forward:/views/Login.jsp";
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
