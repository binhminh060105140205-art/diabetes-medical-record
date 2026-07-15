package controllers;

import dal.PatientRegistrationDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.regex.Pattern;

@WebServlet("/Register")
public class RegisterController extends HttpServlet {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{4,30}$");
    private static final Pattern PHONE = Pattern.compile("^(0[0-9]{9}|\\+84[0-9]{9})$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getSession(false) != null && req.getSession(false).getAttribute("user") != null) {
            resp.sendRedirect(req.getContextPath() + "/Login"); return;
        }
        req.getRequestDispatcher("views/Register.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String username = clean(req, "username"); String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword"); String fullName = clean(req, "fullName");
        String phone = clean(req, "phone"); String email = clean(req, "email");
        String dobText = clean(req, "dateOfBirth"); String gender = clean(req, "gender");
        String address = clean(req, "address"); String insurance = clean(req, "healthInsuranceNo");
        try {
            if (!USERNAME.matcher(username).matches()) throw new IllegalArgumentException("Tên đăng nhập gồm 4–30 chữ, số hoặc dấu gạch dưới.");
            if (password == null || password.length() < 8 || password.length() > 72)
                throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 72 ký tự.");
            if (!password.equals(confirm)) throw new IllegalArgumentException("Mật khẩu nhập lại không khớp.");
            if (fullName.length() < 2 || fullName.length() > 100) throw new IllegalArgumentException("Họ tên không hợp lệ.");
            if (!PHONE.matcher(phone).matches()) throw new IllegalArgumentException("Số điện thoại không đúng định dạng.");
            if (!EMAIL.matcher(email).matches() || email.length() > 100) throw new IllegalArgumentException("Email không hợp lệ.");
            if (!java.util.Set.of("Nam", "Nữ", "Khác").contains(gender)) throw new IllegalArgumentException("Giới tính không hợp lệ.");
            LocalDate dob = LocalDate.parse(dobText);
            if (dob.isAfter(LocalDate.now()) || dob.isBefore(LocalDate.of(1900,1,1)))
                throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
            if (address.length() > 255) throw new IllegalArgumentException("Địa chỉ tối đa 255 ký tự.");
            if (!insurance.isBlank() && !insurance.matches("^[A-Za-z0-9]{10,20}$"))
                throw new IllegalArgumentException("Số BHYT không đúng định dạng.");
            new PatientRegistrationDAO().register(username, password, fullName, phone, email, dob,
                    gender, address, insurance, null);
            req.getSession().setAttribute("registrationSuccess", "Đăng ký thành công. Bạn có thể đăng nhập ngay.");
            resp.sendRedirect(req.getContextPath() + "/Login");
        } catch (IllegalArgumentException ex) {
            req.setAttribute("err", ex.getMessage()); req.setAttribute("values", req.getParameterMap());
            req.getRequestDispatcher("views/Register.jsp").forward(req, resp);
        }
    }
    private String clean(HttpServletRequest req, String name) {
        String value = req.getParameter(name); return value == null ? "" : value.trim();
    }
}
