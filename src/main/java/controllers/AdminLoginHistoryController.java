package controllers;

import dal.AdminDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import models.User;

@WebServlet("/AdminLoginHistory")
public class AdminLoginHistoryController extends HttpServlet {
    private static final int PAGE_SIZE = 50;

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
        int page = pageNumber(request.getParameter("page"));
        AdminDAO adminDAO = new AdminDAO();
        List<Map<String, Object>> history = loadPage(adminDAO, eventType, keyword, page);
        if (history.isEmpty() && page > 1) {
            page = 1;
            history = loadPage(adminDAO, eventType, keyword, page);
        }
        boolean hasNext = history.size() > PAGE_SIZE;
        if (hasNext) history.remove(PAGE_SIZE);

        request.setAttribute("eventType", eventType);
        request.setAttribute("keyword", keyword);
        request.setAttribute("history", history);
        request.setAttribute("currentPage", page);
        request.setAttribute("hasPrevious", page > 1);
        request.setAttribute("hasNext", hasNext);
        request.getRequestDispatcher("views/AdminLoginHistory.jsp").forward(request, response);
    }

    private List<Map<String, Object>> loadPage(
            AdminDAO adminDAO, String eventType, String keyword, int page) {
        int offset = (page - 1) * PAGE_SIZE;
        return new ArrayList<>(adminDAO.loginHistory(
                eventType, keyword, PAGE_SIZE + 1, offset));
    }

    private int pageNumber(String value) {
        try {
            return Math.max(1, Math.min(Integer.parseInt(value), 10_000));
        } catch (NumberFormatException | NullPointerException ignored) {
            return 1;
        }
    }
}
