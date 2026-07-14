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
        String pageStr = request.getParameter("page");
        int currentPage = (pageStr == null || pageStr.trim().isEmpty()) ? 1 : Integer.parseInt(pageStr.trim());
        
        String keyword = request.getParameter("keyword");
        if (keyword != null && !keyword.trim().isEmpty()) {
            request.setAttribute("patients", dao.search(keyword.trim()));
            request.setAttribute("keyword", keyword.trim());
        } else {
            int totalPatients = dao.countAll();
            int totalPages = (int) Math.ceil((double) totalPatients / pageSize);
            
            request.setAttribute("patients", dao.getWithPaging(currentPage, pageSize));
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
        }
        request.getRequestDispatcher("views/PatientList.jsp").forward(request, response);
    }
}