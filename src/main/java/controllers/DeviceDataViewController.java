package controllers;

import dal.DeviceReadingDAO;
import dal.HealthAlertDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import models.User;

@WebServlet("/DeviceData")
public class DeviceDataViewController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "PATIENT")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }

        DeviceReadingDAO.DevicePageData deviceData = new DeviceReadingDAO()
                .loadPageForUser(user.getUserId(), 50);
        if (deviceData.patientId() == null) {
            response.sendRedirect(request.getContextPath() + "/PatientDashboard");
            return;
        }
        request.getSession().setAttribute(
                ControllerSupport.PATIENT_ID_SESSION_KEY, deviceData.patientId());
        HealthAlertDAO.AlertOverview alerts = new HealthAlertDAO()
                .loadOverview(deviceData.patientId(), 10);

        request.setAttribute("patientId", deviceData.patientId());
        request.setAttribute("recentDeviceReadings", deviceData.readings());
        request.setAttribute("recentAlerts", alerts.alerts());
        request.setAttribute("unreadAlerts", alerts.unread());

        request.getRequestDispatcher("views/DeviceDataView.jsp").forward(request, response);
    }
}
