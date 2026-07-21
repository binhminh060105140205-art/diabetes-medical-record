package controllers;

import dal.PatientDAO;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/StaffDashboard")
public class StaffDashboardController extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Query parameters may contain Vietnamese names; decode them before reading keyword.
        request.setCharacterEncoding("UTF-8");
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "STAFF")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }
        String keyword = ControllerSupport.clean(request.getParameter("keyword"));
        boolean searching = !keyword.isEmpty();
        int pageSize = searching ? 50 : 12;
        int currentPage = searching ? 1 : Math.max(
                1, ControllerSupport.positiveIdOrZero(request.getParameter("page")));

        PatientDAO dao = new PatientDAO();
        PatientDAO.PatientListData data = dao.loadPatientList(
                searching ? keyword : null, currentPage, pageSize);
        int totalPages = searching ? 1
                : Math.max(1, (int) Math.ceil((double) data.total() / pageSize));
        if (!searching && currentPage > totalPages) {
            currentPage = totalPages;
            data = dao.loadPatientList(null, currentPage, pageSize);
        }

        request.setAttribute("patients", data.patients());
        request.setAttribute("totalPatients", data.total());
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("maxDOB", LocalDate.now().toString());
        if (searching) request.setAttribute("keyword", keyword);

        request.getRequestDispatcher("views/StaffDashboard.jsp").forward(request, response);
    }
}
