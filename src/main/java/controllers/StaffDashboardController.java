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
        int total = dao.countAll();
        request.setAttribute("totalPatients", total);

        String keyword = request.getParameter("keyword");
        if (keyword != null && !keyword.isBlank()) {
            request.setAttribute("recentPatients", dao.search(keyword.trim()).stream().limit(10).toList());
            request.setAttribute("keyword", keyword.trim());
        } else {
            request.setAttribute("recentPatients", dao.getRecent(8));
        }

        request.getRequestDispatcher("views/StaffDashboard.jsp").forward(request, response);
    }
}
