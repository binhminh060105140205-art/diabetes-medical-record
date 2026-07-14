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
        if (keyword != null && !keyword.trim().isEmpty()) {
            request.setAttribute("patients", dao.search(keyword.trim()));
            request.setAttribute("keyword", keyword.trim());
        } else {
            int totalPatients = dao.countAll();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalPatients / pageSize));
            currentPage = Math.min(currentPage, totalPages);
            
            request.setAttribute("patients", dao.getWithPaging(currentPage, pageSize));
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
