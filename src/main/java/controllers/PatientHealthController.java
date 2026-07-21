package controllers;

import dal.ClinicWorkflowDAO;
import dal.PatientDailyLogDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;
import models.PatientDailyLog;
import models.User;

/** Patient self-monitoring. A future local AI model can consume this data separately. */
@WebServlet(urlPatterns = {"/PatientHealth", "/PatientAI"})
public class PatientHealthController extends HttpServlet {
    private static final Set<String> MEAL_TYPES =
            Set.of("FASTING", "AFTER_MEAL", "BEDTIME", "OTHER");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/PatientDashboard#daily-health");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "PATIENT")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Integer patientId = patientIdForSession(request.getSession(), user);
        if (patientId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        switch (ControllerSupport.clean(request.getParameter("action"))) {
            case "saveLog" -> saveLog(request, response, patientId);
            default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Hành động không hợp lệ");
        }
    }

    private void saveLog(HttpServletRequest request, HttpServletResponse response, int patientId)
            throws IOException {
        try {
            PatientDailyLog log = new PatientDailyLog();
            log.setPatientId(patientId);
            log.setBloodGlucose(decimal(request, "bloodGlucose", "Đường huyết"));
            log.setSystolicBp(integer(request, "systolicBp", "Huyết áp tâm thu"));
            log.setDiastolicBp(integer(request, "diastolicBp", "Huyết áp tâm trương"));
            log.setWeight(decimal(request, "weight", "Cân nặng"));
            log.setHeartRate(integer(request, "heartRate", "Nhịp tim"));
            log.setSpo2(decimal(request, "spo2", "SpO2"));
            log.setMealType(text(request, "mealType"));
            log.setSymptoms(text(request, "symptoms"));
            log.setNote(text(request, "note"));
            validate(log);
            new PatientDailyLogDAO().save(log);
            response.getWriter().print(
                    "{\"success\":true,\"message\":\"Đã lưu chỉ số hôm nay\"}");
        } catch (IllegalArgumentException | ServletException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"success\":false,\"error\":\""
                    + json(error.getMessage()) + "\"}");
        }
    }

    private Integer patientIdForSession(HttpSession session, User user) {
        Object cached = session.getAttribute(ControllerSupport.PATIENT_ID_SESSION_KEY);
        if (cached instanceof Integer patientId) return patientId;
        Integer patientId = new ClinicWorkflowDAO().patientIdForUser(user.getUserId());
        if (patientId != null) {
            session.setAttribute(ControllerSupport.PATIENT_ID_SESSION_KEY, patientId);
        }
        return patientId;
    }

    private String text(HttpServletRequest request, String name) {
        String value = ControllerSupport.clean(request.getParameter(name));
        return value.isEmpty() ? null : value;
    }

    private Double decimal(HttpServletRequest request, String name, String label)
            throws ServletException {
        String value = text(request, name);
        try {
            if (value == null) return null;
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException error) {
            throw new ServletException(label + " không hợp lệ", error);
        }
    }

    private Integer integer(HttpServletRequest request, String name, String label)
            throws ServletException {
        String value = text(request, name);
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException error) {
            throw new ServletException(label + " không hợp lệ", error);
        }
    }

    private void validate(PatientDailyLog log) {
        range(log.getBloodGlucose(), 20, 600, "Đường huyết");
        range(log.getWeight(), 20, 300, "Cân nặng");
        range(log.getSpo2(), 50, 100, "SpO2");
        range(log.getSystolicBp(), 60, 260, "Huyết áp tâm thu");
        range(log.getDiastolicBp(), 30, 180, "Huyết áp tâm trương");
        range(log.getHeartRate(), 30, 220, "Nhịp tim");
        if (log.getSystolicBp() != null && log.getDiastolicBp() != null
                && log.getSystolicBp() <= log.getDiastolicBp()) {
            throw new IllegalArgumentException("Huyết áp tâm thu phải lớn hơn tâm trương.");
        }
        if (log.getSymptoms() != null && log.getSymptoms().length() > 500) {
            throw new IllegalArgumentException("Triệu chứng tối đa 500 ký tự.");
        }
        if (log.getNote() != null && log.getNote().length() > 1000) {
            throw new IllegalArgumentException("Ghi chú tối đa 1000 ký tự.");
        }
        if (log.getMealType() != null && !MEAL_TYPES.contains(log.getMealType())) {
            throw new IllegalArgumentException("Thời điểm đo không hợp lệ.");
        }
        if (log.getMealType() != null && log.getBloodGlucose() == null) {
            throw new IllegalArgumentException("Đã chọn thời điểm đo thì cần nhập đường huyết.");
        }
        if (log.getBloodGlucose() != null && log.getMealType() == null) {
            throw new IllegalArgumentException("Vui lòng chọn thời điểm đo đường huyết.");
        }
        boolean hasMeasurement = log.getBloodGlucose() != null || log.getSystolicBp() != null
                || log.getDiastolicBp() != null || log.getWeight() != null
                || log.getHeartRate() != null || log.getSpo2() != null;
        boolean hasNote = log.getSymptoms() != null || log.getNote() != null;
        if (!hasMeasurement && !hasNote) {
            throw new IllegalArgumentException("Cần nhập ít nhất một chỉ số hoặc ghi chú.");
        }
    }

    private void range(Number value, double min, double max, String label) {
        if (value != null && (value.doubleValue() < min || value.doubleValue() > max)) {
            throw new IllegalArgumentException(
                    label + " ngoài khoảng hợp lệ (" + min + "–" + max + ").");
        }
    }

    private String json(String value) {
        return ControllerSupport.clean(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
