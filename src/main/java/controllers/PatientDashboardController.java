package controllers;

import dal.*;
import models.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

/**
 * [MODIFIED - Upgrade V3]
 * Bỏ: latestWarning (AIWarnings đã xóa)
 * Thêm: unacknowledgedAlerts, alertCount, recentDeviceReadings
 */
@WebServlet("/PatientDashboard")
public class PatientDashboardController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || !"PATIENT".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }

        PatientDAO patDAO = new PatientDAO();
        Patient patient = patDAO.getByUserId(user.getUserId());
        if (patient == null) {
            request.setAttribute("msg", "Chưa có hồ sơ bệnh nhân. Vui lòng liên hệ nhân viên tiếp nhận.");
            request.getRequestDispatcher("views/PatientDashboard.jsp").forward(request, response); return;
        }

        MedicalRecordDAO   recDAO    = new MedicalRecordDAO();
        PatientDailyLogDAO logDAO    = new PatientDailyLogDAO();
        // [NEW V3]
        HealthAlertDAO   alertDAO  = new HealthAlertDAO();
        DeviceReadingDAO deviceDAO = new DeviceReadingDAO();

        List<MedicalRecord> records = recDAO.getByPatient(patient.getPatientId());
        PatientDailyLog todayLog   = logDAO.getTodayLog(patient.getPatientId());
        List<PatientDailyLog> recentLogs = logDAO.getRecent(patient.getPatientId(), 7);
        double[] avg7 = logDAO.getAvg7Days(patient.getPatientId());

        // [NEW V3] HealthAlerts thay AIWarnings
        List<HealthAlert> unacknowledgedAlerts = alertDAO.getUnacknowledged(patient.getPatientId());
        int alertCount = alertDAO.countUnacknowledged(patient.getPatientId());
        List<DeviceReading> recentDeviceReadings = deviceDAO.getRecent(patient.getPatientId(), 3);

        request.setAttribute("patient",               patient);
        request.setAttribute("records",               records);
        request.setAttribute("latestRecord",          records.isEmpty() ? null : records.get(0));
        request.setAttribute("todayLog",              todayLog);
        request.setAttribute("recentLogs",            recentLogs);
        request.setAttribute("avg7BG",                avg7[0] > 0 ? String.format("%.1f", avg7[0]) : null);
        request.setAttribute("avg7BP",                avg7[1] > 0 ? String.format("%.0f", avg7[1]) : null);
        // [NEW V3]
        request.setAttribute("unacknowledgedAlerts",  unacknowledgedAlerts);
        request.setAttribute("alertCount",            alertCount);
        request.setAttribute("recentDeviceReadings",  recentDeviceReadings);

        request.getRequestDispatcher("views/PatientDashboard.jsp").forward(request, response);
    }
}
