package controllers;

import dal.LoginHistoryDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import models.User;

@WebServlet("/Logout")
public class LogoutController extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(LogoutController.class.getName());
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logout(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logout(request, response);
    }

    private void logout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                try {
                    String forwarded = request.getHeader("X-Forwarded-For");
                    String ip = forwarded == null || forwarded.isBlank()
                            ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
                    new LoginHistoryDAO().record(user.getUserId(), user.getUsername(),
                            user.getFullName(), user.getRole(), "LOGOUT", ip,
                            request.getHeader("User-Agent"), session.getId());
                } catch (RuntimeException error) {
                    LOGGER.log(Level.WARNING, "Không thể ghi lịch sử đăng xuất", error);
                }
            }
        }
        if (session != null) session.invalidate();
        response.sendRedirect(request.getContextPath() + "/Login");
    }
}
