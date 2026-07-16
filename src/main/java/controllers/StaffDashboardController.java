package controllers;

import dal.PatientDAO;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/StaffDashboard")
public class StaffDashboardController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || !"STAFF".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }
        PatientDAO dao = new PatientDAO();
        String keyword = request.getParameter("keyword");
        PatientDAO.StaffDashboardData data = dao.loadStaffDashboard(keyword, keyword != null && !keyword.isBlank() ? 10 : 8);
        request.setAttribute("totalPatients", data.totalPatients());
        request.setAttribute("recentPatients", data.patients());
        if (keyword != null && !keyword.isBlank()) request.setAttribute("keyword", keyword.trim());

        request.getRequestDispatcher("views/StaffDashboard.jsp").forward(request, response);
    }
}
