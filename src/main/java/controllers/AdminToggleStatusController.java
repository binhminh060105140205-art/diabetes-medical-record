package controllers;

import dal.AdminDAO;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/AdminToggleStatus")
public class AdminToggleStatusController extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User admin = (session != null) ? (User) session.getAttribute("user") : null;
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }
        int uid;
        try {
            uid = Integer.parseInt(request.getParameter("id"));
            if (uid <= 0) throw new NumberFormatException();
        } catch (NumberFormatException | NullPointerException invalidId) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã tài khoản không hợp lệ");
            return;
        }
        try {
            String status = new AdminDAO().toggleUserStatus(uid, admin.getUserId());
            request.getSession().setAttribute("adminDashboardMessage",
                    "ACTIVE".equals(status) ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.");
        } catch (IllegalArgumentException error) {
            request.getSession().setAttribute("adminDashboardMessage", error.getMessage());
        }
        // Giữ lại page + filterRole + keyword khi redirect về
        String page       = request.getParameter("page");
        String filterRole = request.getParameter("filterRole");
        String filterStatus = request.getParameter("filterStatus");
        String sortOrder = request.getParameter("sortOrder");
        String keyword = request.getParameter("keyword");
        List<String> query = new ArrayList<>();
        addQueryParameter(query, "page", page);
        addQueryParameter(query, "filterRole", filterRole);
        addQueryParameter(query, "filterStatus", filterStatus);
        addQueryParameter(query, "sortOrder", sortOrder);
        addQueryParameter(query, "keyword", keyword);
        String url = request.getContextPath() + "/AdminDashboard"
                + (query.isEmpty() ? "" : "?" + String.join("&", query));
        response.sendRedirect(url);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private void addQueryParameter(List<String> query, String name, String value) {
        if (value == null || value.isBlank()) return;
        query.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
