package controllers;

import dal.UserDAO;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

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
        UserDAO dao = new UserDAO();
        User target = dao.getById(uid);
        if (target != null && uid != admin.getUserId() && !target.getUsername().equals("admin")) {
            target.setStatus("ACTIVE".equals(target.getStatus()) ? "INACTIVE" : "ACTIVE");
            dao.update(target);
        }
        // Giữ lại page + filterRole + keyword khi redirect về
        String page       = request.getParameter("page");
        String filterRole = request.getParameter("filterRole");
        String keyword    = request.getParameter("keyword");
        StringBuilder url = new StringBuilder(request.getContextPath() + "/AdminDashboard?");
        if (page       != null && !page.isEmpty())       url.append("page=").append(page).append("&");
        if (filterRole != null && !filterRole.isEmpty()) url.append("filterRole=").append(filterRole).append("&");
        if (keyword    != null && !keyword.isEmpty())    url.append("keyword=").append(keyword).append("&");
        response.sendRedirect(url.toString());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
