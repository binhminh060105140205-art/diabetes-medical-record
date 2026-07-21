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
        String username = ControllerSupport.clean(req.getParameter("username"));
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");
        String fullName = ControllerSupport.clean(req.getParameter("fullName"));
        String phone = ControllerSupport.clean(req.getParameter("phone"));
        String email = ControllerSupport.clean(req.getParameter("email"));
        String dobText = ControllerSupport.clean(req.getParameter("dateOfBirth"));
        String gender = ControllerSupport.clean(req.getParameter("gender"));
        String address = ControllerSupport.clean(req.getParameter("address"));
        String insurance = ControllerSupport.clean(req.getParameter("healthInsuranceNo"));
        try {
            PatientRegistrationService service=new PatientRegistrationService(new PatientRegistrationDAO());
            service.register(new PatientRegistrationService.Command(username,password,confirm,fullName,phone,email,dobText,gender,address,insurance,null));
            req.getSession().setAttribute("registrationSuccess", "Đăng ký thành công. Bạn có thể đăng nhập ngay.");
            resp.sendRedirect(req.getContextPath() + "/Login");
        } catch (IllegalStateException ex) {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            req.setAttribute("err", "Cơ sở dữ liệu đang khởi động hoặc mất kết nối. Vui lòng thử lại sau ít phút.");
            req.setAttribute("values", req.getParameterMap());
            req.getRequestDispatcher("views/Register.jsp").forward(req, resp);
        } catch (IllegalArgumentException ex) {
            req.setAttribute("err", ex.getMessage()); req.setAttribute("values", req.getParameterMap());
            req.getRequestDispatcher("views/Register.jsp").forward(req, resp);
        }
    }
}
