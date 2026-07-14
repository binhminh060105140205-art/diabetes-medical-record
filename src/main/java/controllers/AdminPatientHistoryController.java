package controllers;

import dal.*;
import models.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * Admin xem lịch sử bệnh án của một bệnh nhân qua userId
 * URL: /AdminPatientHistory?userId=X
 */
@WebServlet("/AdminPatientHistory")
public class AdminPatientHistoryController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User admin = (session != null) ? (User) session.getAttribute("user") : null;
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }

        String userIdParam = request.getParameter("userId");
        if (userIdParam == null || userIdParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/AdminDashboard"); return;
        }

        PatientDAO patDAO = new PatientDAO();
        Patient patient = null;

        try {
            int userId = Integer.parseInt(userIdParam.trim());
            patient = patDAO.getByUserId(userId);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/AdminDashboard"); return;
        }

        if (patient == null) {
            request.getSession().setAttribute("flashErr",
                "Không tìm thấy hồ sơ bệnh nhân cho tài khoản này.");
            response.sendRedirect(request.getContextPath() + "/AdminDashboard"); return;
        }

        MedicalRecordDAO recDAO = new MedicalRecordDAO();
        AIWarningDAO warnDAO    = new AIWarningDAO();

        request.setAttribute("patient", patient);
        request.setAttribute("records", recDAO.getByPatient(patient.getPatientId()));
        request.getRequestDispatcher("views/PatientHistory.jsp").forward(request, response);
    }
}
