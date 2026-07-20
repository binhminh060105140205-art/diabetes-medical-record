package controllers;

import dal.PatientDAO;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/PatientList")
public class PatientListController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "STAFF", "DOCTOR", "ADMIN")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }
        
        PatientDAO dao = new PatientDAO();
        int pageSize = 10;
        int currentPage = Math.max(
                1, ControllerSupport.positiveIdOrZero(request.getParameter("page")));
        
        String keyword = request.getParameter("keyword");
        boolean searching = keyword != null && !keyword.isBlank();
        PatientDAO.PatientListData data = dao.loadPatientList(keyword, currentPage, searching ? 50 : pageSize);
        if (searching) {
            request.setAttribute("patients", data.patients());
            request.setAttribute("keyword", keyword.trim());
        } else {
            int totalPages = Math.max(1, (int) Math.ceil((double) data.total() / pageSize));
            if (currentPage > totalPages) {
                currentPage = totalPages;
                data = dao.loadPatientList(null, currentPage, pageSize);
            }
            request.setAttribute("patients", data.patients());
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
        }
        request.getRequestDispatcher("views/PatientList.jsp").forward(request, response);
    }

}
