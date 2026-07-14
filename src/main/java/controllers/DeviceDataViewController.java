package controllers;

import dal.*;
import models.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

/**
 * [NEW - Upgrade V3]
 * GET /DeviceData — Trang bệnh nhân xem và upload dữ liệu thiết bị.
 */
@WebServlet("/DeviceData")
public class DeviceDataViewController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || !"PATIENT".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }

        Patient patient = new PatientDAO().getByUserId(user.getUserId());
        if (patient == null) { response.sendRedirect(request.getContextPath() + "/PatientDashboard"); return; }

        DeviceReadingDAO deviceDAO = new DeviceReadingDAO();
        HealthAlertDAO   alertDAO  = new HealthAlertDAO();

        List<DeviceReading> recentDeviceReadings = deviceDAO.getRecent(patient.getPatientId(), 50);
        List<HealthAlert>   recentAlerts         = alertDAO.getRecent(patient.getPatientId(), 10);
        int unreadAlerts = alertDAO.countUnacknowledged(patient.getPatientId());

        request.setAttribute("patient",              patient);
        request.setAttribute("recentDeviceReadings", recentDeviceReadings);
        request.setAttribute("recentAlerts",         recentAlerts);
        request.setAttribute("unreadAlerts",         unreadAlerts);

        request.getRequestDispatcher("views/DeviceDataView.jsp").forward(request, response);
    }
}
