package controllers;

import dal.PatientRegistrationDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import vn.diabetes.service.PatientRegistrationService;

@WebServlet("/Register")
public class RegisterController extends HttpServlet {
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
            PatientRegistrationService service=new PatientRegistrationService(new PatientRegistrationDAO());
            service.register(new PatientRegistrationService.Command(username,password,confirm,fullName,phone,email,dobText,gender,address,insurance,null));
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
