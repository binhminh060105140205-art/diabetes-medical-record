package controllers;

import dal.AdminDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import models.User;

@WebServlet("/AdminLoginHistory")
public class AdminLoginHistoryController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User admin = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(admin, "ADMIN")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }
        String eventType = ControllerSupport.clean(request.getParameter("eventType")).toUpperCase();
        if (!Set.of("LOGIN", "LOGOUT").contains(eventType)) eventType = "";
        String keyword = ControllerSupport.clean(request.getParameter("keyword"));
        if (keyword.length() > 80) keyword = keyword.substring(0, 80);
        request.setAttribute("eventType", eventType);
        request.setAttribute("keyword", keyword);
        request.setAttribute("history", new AdminDAO().loginHistory(eventType, keyword, 200));
        request.getRequestDispatcher("views/AdminLoginHistory.jsp").forward(request, response);
    }
}
