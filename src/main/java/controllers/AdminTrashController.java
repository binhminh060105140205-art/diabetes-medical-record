package controllers;

import dal.AdminDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import models.User;

@WebServlet("/AdminTrash")
public class AdminTrashController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User admin = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(admin, "ADMIN")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }
        request.setAttribute("trashItems", new AdminDAO().trashItems());
        moveFlash(request, "adminTrashMessage");
        request.getRequestDispatcher("views/AdminTrash.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        User admin = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(admin, "ADMIN")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            AdminDAO dao = new AdminDAO();
            switch (ControllerSupport.clean(request.getParameter("action"))) {
                case "softDelete" -> dao.softDeleteUser(
                        ControllerSupport.positiveId(request.getParameter("userId"), "Mã tài khoản"),
                        admin.getUserId());
                case "restore" -> dao.restore(positiveLong(request.getParameter("trashId")),
                        admin.getUserId());
                case "purge" -> dao.purge(positiveLong(request.getParameter("trashId")),
                        admin.getUserId());
                default -> throw new IllegalArgumentException("Thao tác thùng rác không hợp lệ.");
            }
            ControllerSupport.flash(request, "adminTrashMessage", "Đã cập nhật thùng rác.");
        } catch (IllegalArgumentException error) {
            ControllerSupport.flash(request, "adminTrashMessage", error.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/AdminTrash");
    }

    private long positiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed > 0) return parsed;
        } catch (NumberFormatException | NullPointerException ignored) { }
        throw new IllegalArgumentException("Mã thùng rác không hợp lệ.");
    }

    private void moveFlash(HttpServletRequest request, String key) {
        Object value = request.getSession().getAttribute(key);
        request.setAttribute(key, value);
        request.getSession().removeAttribute(key);
    }
}
