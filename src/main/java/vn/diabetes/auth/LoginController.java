package vn.diabetes.auth;

import jakarta.servlet.http.HttpSession;
import models.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
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
                        Model model) {
        AuthenticationService.LoginResult result = authentication.login(username, password);
        if (result.successful()) {
            session.setAttribute("user", result.user());
            return redirectFor(result.user());
        }
        model.addAttribute("err", result.error());
        model.addAttribute("lockUntil", result.lockUntil());
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
