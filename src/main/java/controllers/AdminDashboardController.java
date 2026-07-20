package controllers;

import dal.UserDAO;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/AdminDashboard")
public class AdminDashboardController extends HttpServlet {

    private static final int PAGE_SIZE = 5; // 5 dòng mỗi trang

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "ADMIN")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }

        UserDAO userDAO = new UserDAO();

        String filterRole = normalizeRole(request.getParameter("filterRole"));
        String filterStatus = normalizeStatus(request.getParameter("filterStatus"));
        String sortOrder = normalizeSort(request.getParameter("sortOrder"));
        String pageStr    = request.getParameter("page");
        String keyword    = ControllerSupport.clean(request.getParameter("keyword"));
        if (keyword.length() > 80) keyword = keyword.substring(0, 80);

        int currentPage = Math.max(1, ControllerSupport.positiveIdOrZero(pageStr));

        UserDAO.AdminDashboardData data = userDAO.loadAdminDashboard(
                filterRole, filterStatus, keyword, sortOrder, currentPage, PAGE_SIZE);
        int totalRecords = data.filteredTotal();
        int totalPages   = Math.max(1, (int) Math.ceil((double) totalRecords / PAGE_SIZE));
        if (currentPage > totalPages) {
            currentPage = totalPages;
            data = userDAO.loadAdminDashboard(
                    filterRole, filterStatus, keyword, sortOrder, currentPage, PAGE_SIZE);
        }
        request.setAttribute("allUsers",     data.users());
        request.setAttribute("currentPage",  currentPage);
        request.setAttribute("totalPages",   totalPages);
        request.setAttribute("totalRecords", totalRecords);
        request.setAttribute("totalUsers",   data.totalUsers());
        request.setAttribute("filterRole",   filterRole);
        request.setAttribute("filterStatus", filterStatus);
        request.setAttribute("sortOrder",    sortOrder);
        request.setAttribute("keyword",      keyword);
        request.setAttribute("pageSize",     PAGE_SIZE);

        request.setAttribute("totalPatients", data.patients());
        request.setAttribute("totalDoctors",  data.doctors());
        request.setAttribute("totalStaffs",   data.staff());

        request.getRequestDispatcher("views/AdminDashboard.jsp").forward(request, response);
    }

    static String normalizeRole(String value) {
        String role = ControllerSupport.clean(value).toUpperCase();
        return java.util.Set.of("ADMIN", "STAFF", "DOCTOR", "PATIENT").contains(role)
                ? role : "";
    }

    static String normalizeStatus(String value) {
        String status = ControllerSupport.clean(value).toUpperCase();
        return java.util.Set.of("ACTIVE", "INACTIVE").contains(status) ? status : "";
    }

    static String normalizeSort(String value) {
        return "OLDEST".equalsIgnoreCase(ControllerSupport.clean(value)) ? "OLDEST" : "NEWEST";
    }
}
