package controllers;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import models.User;

@WebServlet(name = "HomeController", urlPatterns = {"/Home"})
public class HomeController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user != null) {
            String role = user.getRole();
            if ("ADMIN".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/AdminDashboard");
            } else if ("STAFF".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/StaffDashboard");
            } else if ("DOCTOR".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/DoctorDashboard");
            } else if ("PATIENT".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/PatientDashboard");
            } else {
                response.sendRedirect(request.getContextPath() + "/Login");
            }
        } else {
            request.getRequestDispatcher("views/Login.jsp").forward(request, response);
        }
    }
}
