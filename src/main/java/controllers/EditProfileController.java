package controllers;

import dal.DoctorDAO;
import dal.PatientDAO;
import dal.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;
import models.Patient;
import models.User;
import vn.diabetes.auth.Passwords;
import vn.diabetes.validation.Validators;

@WebServlet(name = "EditProfileController", urlPatterns = {"/EditProfile", "/Settings"})
public class EditProfileController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = currentUser(request, response);
        if (currentUser == null) return;
        request.setAttribute("activeSetting", settingFromRequest(request));
        loadProfile(request, currentUser);
        request.getRequestDispatcher("views/editProfile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User currentUser = currentUser(request, response);
        if (currentUser == null) return;

        try {
            UserDAO users = new UserDAO();
            User storedUser = users.getById(currentUser.getUserId());
            if (storedUser == null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");

            String action = ControllerSupport.clean(request.getParameter("action"));
            if ("password".equals(action)) {
                changePassword(request, users, storedUser);
                request.setAttribute("successMessage", "Đã đổi mật khẩu thành công.");
                request.setAttribute("activeSetting", "security");
            } else if (action.isEmpty() || "profile".equals(action)) {
                updateProfile(request, users, storedUser);
                request.setAttribute("successMessage", "Đã cập nhật thông tin cá nhân.");
                request.setAttribute("activeSetting", "personal");
            } else {
                throw new IllegalArgumentException("Thao tác cài đặt không hợp lệ.");
            }

            User refreshed = users.getById(currentUser.getUserId());
            request.getSession().setAttribute("user", refreshed);
            request.setAttribute("profileUser", refreshed);
        } catch (IllegalArgumentException error) {
            request.setAttribute("errorMessage", error.getMessage());
            request.setAttribute("activeSetting",
                    "password".equals(request.getParameter("action")) ? "security" : "personal");
        } catch (Exception error) {
            getServletContext().log("Profile update failed for user " + currentUser.getUserId(), error);
            request.setAttribute("errorMessage", "Không thể cập nhật cài đặt. Vui lòng thử lại.");
            request.setAttribute("activeSetting",
                    "password".equals(request.getParameter("action")) ? "security" : "personal");
        }

        loadProfile(request, currentUser);
        request.getRequestDispatcher("views/editProfile.jsp").forward(request, response);
    }

    private void updateProfile(HttpServletRequest request, UserDAO users, User user)
            throws Exception {
        String username = ControllerSupport.clean(request.getParameter("username"));
        if (!username.matches("^[A-Za-z0-9._-]{4,50}$")) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập phải có 4–50 ký tự và không chứa khoảng trắng.");
        }
        if (!username.equalsIgnoreCase(user.getUsername()) && users.usernameExists(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng.");
        }

        String fullName = Validators.fullName(request.getParameter("fullName"));
        String phone = optionalPhone(request.getParameter("phone"));
        String email = Validators.email(request.getParameter("email"), false);
        String cccd = optionalIdentity(request.getParameter("cccd"));
        String gender = optionalGender(request.getParameter("gender"));
        String address = Validators.address(request.getParameter("address"));
        LocalDate dob = Validators.dateOfBirth(request.getParameter("dob"), false);

        user.setUsername(username);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setDob(dob == null ? null : Date.valueOf(dob));
        user.setGender(gender);
        user.setAddress(address);
        user.setCccd(cccd);
        users.updateProfile(user);

        if ("PATIENT".equalsIgnoreCase(user.getRole())) {
            PatientDAO patients = new PatientDAO();
            Patient patient = patients.getByUserId(user.getUserId());
            if (patient != null) {
                patient.setFullName(fullName);
                patient.setPhone(phone);
                patient.setDateOfBirth(dob);
                patient.setGender(gender);
                patient.setAddress(address);
                patient.setNationalId(cccd);
                patients.updateBasicProfile(patient);
            }
        }
    }

    private void changePassword(HttpServletRequest request, UserDAO users, User user)
            throws Exception {
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        if (!Passwords.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
        newPassword = Validators.password(newPassword, "Mật khẩu mới");
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp.");
        }
        if (Passwords.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPassword(newPassword);
        users.updateProfile(user);
    }

    private User currentUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) response.sendRedirect(request.getContextPath() + "/Login");
        return user;
    }

    private String settingFromRequest(HttpServletRequest request) {
        String section = ControllerSupport.clean(request.getParameter("section"));
        return switch (section) {
            case "profile", "personal", "professional" -> "personal";
            case "password", "security" -> "security";
            case "logout", "session" -> "session";
            default -> "menu";
        };
    }

    private void loadProfile(HttpServletRequest request, User fallback) {
        request.setAttribute("maxDOB", LocalDate.now().toString());
        User profileUser = (User) request.getAttribute("profileUser");
        if (profileUser == null) {
            try {
                User loaded = new UserDAO().getById(fallback.getUserId());
                profileUser = loaded == null ? fallback : loaded;
                request.setAttribute("profileUser", profileUser);
            } catch (Exception error) {
                profileUser = fallback;
                request.setAttribute("profileUser", fallback);
                if (request.getAttribute("errorMessage") == null) {
                    request.setAttribute("errorMessage", "Không thể tải cài đặt tài khoản.");
                }
            }
        }

        if ("DOCTOR".equalsIgnoreCase(profileUser.getRole())) {
            try {
                request.setAttribute("doctor", new DoctorDAO().getByUserId(profileUser.getUserId()));
            } catch (Exception error) {
                getServletContext().log(
                        "Unable to load professional profile for doctor user " + profileUser.getUserId(), error);
                request.setAttribute("professionalError",
                        "Không thể tải hồ sơ hành nghề. Vui lòng thử lại sau.");
            }
        }
    }

    private String optionalPhone(String value) {
        String cleaned = ControllerSupport.clean(value);
        if (!cleaned.isEmpty() && !cleaned.matches("^(0|\\+84)[0-9]{9}$")) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam.");
        }
        return cleaned;
    }

    private String optionalIdentity(String value) {
        String cleaned = ControllerSupport.clean(value);
        if (!cleaned.isEmpty() && !cleaned.matches("^(?:[0-9]{9}|[0-9]{12})$")) {
            throw new IllegalArgumentException("CCCD/CMND phải gồm 9 hoặc 12 chữ số.");
        }
        return cleaned;
    }

    private String optionalGender(String value) {
        String cleaned = ControllerSupport.clean(value);
        if (!cleaned.isEmpty() && !Set.of("Nam", "Nữ", "Khác").contains(cleaned)) {
            throw new IllegalArgumentException("Giới tính không hợp lệ.");
        }
        return cleaned;
    }
}
