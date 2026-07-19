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

        PatientDAO.PatientDashboardData data =
                new PatientDAO().loadPatientDashboard(user.getUserId());
        Patient patient = data.patient();
        if (patient == null) {
            request.setAttribute("msg", "Chưa có hồ sơ bệnh nhân. Vui lòng liên hệ nhân viên tiếp nhận.");
            request.getRequestDispatcher("views/PatientToday.jsp").forward(request, response); return;
        }

        PatientDailyLog todayLog = data.todayLog();
        List<HealthAlert> unacknowledgedAlerts = data.alerts();
        int alertCount = unacknowledgedAlerts.size();

        request.setAttribute("patient",               patient);
        request.setAttribute("diabetesProfile",       data.diabetesProfile());
        request.setAttribute("latestRecord",          data.latestRecord());
        request.setAttribute("todayLog",              todayLog);
        // [NEW V3]
        request.setAttribute("unacknowledgedAlerts",  unacknowledgedAlerts);
        request.setAttribute("alertCount",            alertCount);

        request.getRequestDispatcher("views/PatientToday.jsp").forward(request, response);
    }
}
