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

        List<MedicalRecord> records = recDAO.getByPatient(patient.getPatientId());
        PatientDailyLog todayLog = logDAO.getTodayLog(patient.getPatientId());

        // [NEW V3] HealthAlerts thay AIWarnings
        List<HealthAlert> unacknowledgedAlerts = alertDAO.getUnacknowledged(patient.getPatientId());
        int alertCount = unacknowledgedAlerts.size();

        request.setAttribute("patient",               patient);
        request.setAttribute("records",               records);
        request.setAttribute("latestRecord",          records.isEmpty() ? null : records.get(0));
        request.setAttribute("todayLog",              todayLog);
        // [NEW V3]
        request.setAttribute("unacknowledgedAlerts",  unacknowledgedAlerts);
        request.setAttribute("alertCount",            alertCount);

        request.getRequestDispatcher("views/PatientDashboard.jsp").forward(request, response);
    }
}
