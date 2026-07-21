package controllers;

import dal.PatientDAO;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

        if ("STAFF".equals(user.getRole())) {
            String keyword = ControllerSupport.clean(request.getParameter("keyword"));
            String target = request.getContextPath() + "/StaffDashboard";
            if (!keyword.isEmpty()) {
                target += "?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            }
            response.sendRedirect(target + "#patients");
            return;
        }
        
        PatientDAO dao = new PatientDAO();
        int pageSize = 10;
        int currentPage = Math.max(
                1, ControllerSupport.positiveIdOrZero(request.getParameter("page")));
        
        String keyword = request.getParameter("keyword");
        boolean searching = keyword != null && !keyword.isBlank();
        if (searching) currentPage = 1;
        int effectivePageSize = searching ? 50 : pageSize;
        PatientDAO.PatientListData data = "DOCTOR".equals(user.getRole())
                ? dao.loadPatientListForDoctor(
                        user.getUserId(), keyword, currentPage, effectivePageSize)
                : dao.loadPatientList(keyword, currentPage, effectivePageSize);
        if (searching) {
            request.setAttribute("patients", data.patients());
            request.setAttribute("keyword", keyword.trim());
            request.setAttribute("currentPage", currentPage);
        } else {
            int totalPages = Math.max(1, (int) Math.ceil((double) data.total() / pageSize));
            if (currentPage > totalPages) {
                currentPage = totalPages;
                data = "DOCTOR".equals(user.getRole())
                        ? dao.loadPatientListForDoctor(
                                user.getUserId(), null, currentPage, pageSize)
                        : dao.loadPatientList(null, currentPage, pageSize);
            }
            request.setAttribute("patients", data.patients());
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
        }
        request.getRequestDispatcher("views/PatientList.jsp").forward(request, response);
    }

}
