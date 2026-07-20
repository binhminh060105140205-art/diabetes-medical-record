package controllers;

import dal.PatientDailyLogDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import models.PatientDailyLog;
import models.User;

@WebServlet("/PatientJournal")
public class PatientJournalController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "PATIENT")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }

        PatientDailyLogDAO.PatientJournalData data = new PatientDailyLogDAO()
                .loadJournalForUser(user.getUserId(), 30);
        if (data.patientId() == null) {
            response.sendError(HttpServletResponse.SC_CONFLICT,
                    "Tài khoản chưa liên kết hồ sơ bệnh nhân");
            return;
        }
        request.getSession().setAttribute(
                ControllerSupport.PATIENT_ID_SESSION_KEY, data.patientId());

        List<PatientDailyLog> logs = data.logs();
        request.setAttribute("logs", logs);
        request.setAttribute("avgGlucose", averageGlucose(logs));
        request.setAttribute("avgSystolic", averageSystolic(logs));
        request.getRequestDispatcher("views/PatientJournal.jsp").forward(request, response);
    }

    private String averageGlucose(List<PatientDailyLog> logs) {
        double average = logs.stream()
                .filter(log -> log.getBloodGlucose() != null)
                .mapToDouble(PatientDailyLog::getBloodGlucose)
                .average().orElse(0);
        return average == 0 ? null : String.format("%.1f", average);
    }

    private String averageSystolic(List<PatientDailyLog> logs) {
        double average = logs.stream()
                .filter(log -> log.getSystolicBp() != null)
                .mapToInt(PatientDailyLog::getSystolicBp)
                .average().orElse(0);
        return average == 0 ? null : String.format("%.0f", average);
    }
}
