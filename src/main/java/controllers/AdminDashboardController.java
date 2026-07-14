package controllers;

import dal.*;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/AdminDashboard")
public class AdminDashboardController extends HttpServlet {

    private static final int PAGE_SIZE = 5; // 5 dòng mỗi trang

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"ADMIN".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }

        UserDAO    userDAO = new UserDAO();
        PatientDAO patDAO  = new PatientDAO();

        String filterRole = request.getParameter("filterRole");
        String pageStr    = request.getParameter("page");
        String keyword    = request.getParameter("keyword");

        int currentPage = 1;
        try { currentPage = Integer.parseInt(pageStr); } catch (Exception ignored) {}
        if (currentPage < 1) currentPage = 1;

        int totalRecords = userDAO.countWithFilter(filterRole, keyword);
        int totalPages   = Math.max(1, (int) Math.ceil((double) totalRecords / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        request.setAttribute("allUsers",     userDAO.getWithPagingAndFilter(filterRole, keyword, currentPage, PAGE_SIZE));
        request.setAttribute("currentPage",  currentPage);
        request.setAttribute("totalPages",   totalPages);
        request.setAttribute("totalRecords", totalRecords);
        request.setAttribute("filterRole",   filterRole);
        request.setAttribute("keyword",      keyword);
        request.setAttribute("pageSize",     PAGE_SIZE);

        request.setAttribute("totalPatients", patDAO.countAll());
        request.setAttribute("totalDoctors",  userDAO.countWithFilter("DOCTOR", null));
        request.setAttribute("totalStaffs",   userDAO.countWithFilter("STAFF", null));

        request.getRequestDispatcher("views/AdminDashboard.jsp").forward(request, response);
    }
}
