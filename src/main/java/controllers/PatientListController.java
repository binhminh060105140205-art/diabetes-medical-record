package controllers;

import dal.PatientDAO;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/PatientList")
public class PatientListController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || (!user.getRole().equals("STAFF") && !user.getRole().equals("DOCTOR") && !user.getRole().equals("ADMIN"))) {
            response.sendRedirect("Login");
            return;
        }
        
        PatientDAO dao = new PatientDAO();
        int pageSize = 10;
        int currentPage = positiveInt(request.getParameter("page"), 1);
        
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

    private int positiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException | NullPointerException ignored) {
            return fallback;
        }
    }
}
