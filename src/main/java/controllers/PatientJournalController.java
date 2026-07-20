package controllers;

import dal.PatientDailyLogDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
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
        request.setAttribute("avgDiastolic", averageDiastolic(logs));
        request.setAttribute("avgWeight", averageWeight(logs));
        request.setAttribute("latestLog", logs.isEmpty() ? null : logs.get(0));
        request.setAttribute("measurementDays", logs.size());
        request.setAttribute("glucoseTrend", glucoseTrend(logs));
        request.getRequestDispatcher("views/PatientJournal.jsp").forward(request, response);
    }

    private String averageGlucose(List<PatientDailyLog> logs) {
        double average = logs.stream()
                .filter(log -> log.getBloodGlucose() != null)
                .mapToDouble(PatientDailyLog::getBloodGlucose)
                .average().orElse(0);
        return average == 0 ? null : String.format(Locale.ROOT, "%.1f", average);
    }

    private String averageSystolic(List<PatientDailyLog> logs) {
        double average = logs.stream()
                .filter(log -> log.getSystolicBp() != null)
                .mapToInt(PatientDailyLog::getSystolicBp)
                .average().orElse(0);
        return average == 0 ? null : String.format(Locale.ROOT, "%.0f", average);
    }

    private String averageDiastolic(List<PatientDailyLog> logs) {
        double average = logs.stream()
                .filter(log -> log.getDiastolicBp() != null)
                .mapToInt(PatientDailyLog::getDiastolicBp)
                .average().orElse(0);
        return average == 0 ? null : String.format(Locale.ROOT, "%.0f", average);
    }

    private String averageWeight(List<PatientDailyLog> logs) {
        double average = logs.stream()
                .filter(log -> log.getWeight() != null)
                .mapToDouble(PatientDailyLog::getWeight)
                .average().orElse(0);
        return average == 0 ? null : String.format(Locale.ROOT, "%.1f", average);
    }

    private String glucoseTrend(List<PatientDailyLog> logs) {
        List<Double> values = logs.stream()
                .map(PatientDailyLog::getBloodGlucose)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (values.size() < 2) return "Chưa đủ dữ liệu";
        double latest = values.get(0);
        double previousAverage = values.subList(1, values.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(latest);
        if (previousAverage == 0) return "Chưa đủ dữ liệu";
        double change = (latest - previousAverage) / previousAverage;
        if (change >= 0.10) return "Đang tăng";
        if (change <= -0.10) return "Đang giảm";
        return "Ổn định";
    }
}
