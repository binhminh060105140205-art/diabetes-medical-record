package controllers;

import dal.PatientDAO;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/StaffDashboard")
public class StaffDashboardController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "STAFF")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }
        String keyword = request.getParameter("keyword");
        PatientDAO.StaffDashboardData data = new PatientDAO().loadStaffDashboard(
                keyword, keyword != null && !keyword.isBlank() ? 10 : 8);
        request.setAttribute("totalPatients", data.totalPatients());
        request.setAttribute("recentPatients", data.patients());
        if (keyword != null && !keyword.isBlank()) request.setAttribute("keyword", keyword.trim());

        request.getRequestDispatcher("views/StaffDashboard.jsp").forward(request, response);
    }
}
