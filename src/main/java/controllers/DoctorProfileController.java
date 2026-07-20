package controllers;

import models.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/** Giữ tương thích đường dẫn cũ; hồ sơ hành nghề nay nằm trong Cài đặt tài khoản. */
@WebServlet(name = "DoctorProfileController", urlPatterns = {"/DoctorProfile"})
public class DoctorProfileController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/Login");
            return;
        }
        if (!"DOCTOR".equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/Settings?section=profile#professional");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/Login");
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Chỉ quản trị viên được cập nhật ảnh hồ sơ hành nghề của bác sĩ.");
    }
}
