package controllers;

import dal.*;
import models.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Staff handles intake; the assigned doctor maintains the diabetes profile
 * and completes the consultation. Lab results are entered in ClinicWorkflow.
 */
@WebServlet("/MedicalRecordForm")
public class MedicalRecordFormController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "STAFF", "DOCTOR")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }

        String pidParam = request.getParameter("patientId");
        String encounterParam = request.getParameter("encounterId");
        String ridParam = request.getParameter("recordId");

        if (ridParam != null) {
            int recordId = ControllerSupport.positiveIdOrZero(ridParam);
            if (recordId == 0) { response.sendError(400, "Mã bệnh án không hợp lệ"); return; }
            MedicalRecordDAO recDAO = new MedicalRecordDAO();
            var formData = recDAO.loadFormData(recordId);
            MedicalRecord rec = formData.record();
            if (rec == null) { response.sendRedirect(request.getContextPath() + "/PatientList"); return; }
            Doctor assignedDoctor = formData.doctor();
            request.setAttribute("diabetesProfile", formData.diabetesProfile());
            request.setAttribute("latestIndicator", formData.latestIndicator());
            request.setAttribute("labSummary", formData.labSummary());
            if ("DOCTOR".equals(user.getRole())) {
                if (assignedDoctor == null || assignedDoctor.getUserId() != user.getUserId()) {
                    response.sendError(403); return;
                }
            }

            request.setAttribute("record",   rec);
            request.setAttribute("patient",  formData.patient());

            HealthIndicator indicator = formData.indicator();
            request.setAttribute("indicator", indicator);
            request.setAttribute("prescriptionItems", formData.prescriptionItems());

            if (assignedDoctor != null) request.setAttribute("assignedDoctor", assignedDoctor);

            boolean clinicalDone = indicator != null && (indicator.getHeight() > 0 || indicator.getSystolicBp() > 0);
            boolean labDone      = indicator != null && (indicator.getBloodGlucose() > 0 || indicator.getHba1c() > 0);
            request.setAttribute("clinicalDone", clinicalDone);
            request.setAttribute("labDone",      labDone);
        } else if (pidParam != null) {
            int patientId = ControllerSupport.positiveIdOrZero(pidParam);
            if (patientId == 0) { response.sendError(400, "Mã bệnh nhân không hợp lệ"); return; }
            Patient patient = new PatientDAO().getById(patientId);
            if (patient == null) { response.sendError(404, "Không tìm thấy bệnh nhân"); return; }
            request.setAttribute("patient", patient);
            request.setAttribute("diabetesProfile", new DiabetesProfileDAO().getByPatientId(patientId));
            request.setAttribute("encounterId", encounterParam);
            if (encounterParam != null && !encounterParam.isBlank()) {
                int encounterId = ControllerSupport.positiveIdOrZero(encounterParam);
                request.setAttribute("appointmentTime",
                        new ClinicWorkflowDAO().appointmentTimeForEncounter(encounterId));
            }
        }

        if ("STAFF".equals(user.getRole())) {
            request.setAttribute("doctors", new DoctorDAO().getAll());
        }
        request.getRequestDispatcher("views/MedicalRecordForm.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = ControllerSupport.currentUser(request);
        if (user == null) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }
        HttpSession session = request.getSession();

        String recordId = request.getParameter("recordId");
        MedicalRecordDAO records = new MedicalRecordDAO();
        switch (ControllerSupport.clean(request.getParameter("action"))) {
            case "saveBasic" -> saveBasic(request, response, user, records);
            case "saveClinical" -> saveClinical(request, response, user, records, recordId);
            case "saveDiabetesProfile" ->
                    saveDiabetesProfile(request, response, user, records, session, recordId);
            case "saveConclusion" ->
                    saveConclusion(request, response, user, records, session, recordId);
            default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Thao tác bệnh án không hợp lệ");
        }
    }

    private void saveBasic(HttpServletRequest request, HttpServletResponse response,
            User user, MedicalRecordDAO records) throws IOException {
        if (!requireRole(request, response, user, "STAFF")) return;

        MedicalRecord record = new MedicalRecord();
        record.setPatientId(ControllerSupport.positiveId(
                request.getParameter("patientId"), "Mã bệnh nhân"));
        String doctorId = ControllerSupport.clean(request.getParameter("doctorId"));
        if (!doctorId.isEmpty()) {
            record.setDoctorId(ControllerSupport.positiveId(doctorId, "Mã bác sĩ"));
        }
        record.setCreatedByStaff(user.getUserId());

        String encounterId = ControllerSupport.clean(request.getParameter("encounterId"));
        if (!encounterId.isEmpty()) {
            record.setEncounterId(ControllerSupport.positiveId(encounterId, "Mã lượt khám"));
            int[] assignment = new ClinicWorkflowDAO()
                    .assignmentForEncounter(record.getEncounterId());
            record.setPatientId(assignment[0]);
            record.setDoctorId(assignment[1]);
        }

        record.setReasonForVisit(request.getParameter("reasonForVisit"));
        record.setSymptoms(request.getParameter("symptoms"));
        record.setMedicalHistory(request.getParameter("medicalHistory"));
        record.setLifestyleHabits(request.getParameter("lifestyleHabits"));
        record.setClinicalExam(request.getParameter("clinicalExam"));
        records.create(record);
        response.sendRedirect(request.getContextPath()
                + "/MedicalRecordForm?recordId=" + record.getRecordId() + "&tab=2");
    }

    private void saveClinical(HttpServletRequest request, HttpServletResponse response,
            User user, MedicalRecordDAO records, String recordIdValue)
            throws ServletException, IOException {
        if (!requireRole(request, response, user, "STAFF")) return;

        int recordId = ControllerSupport.positiveId(recordIdValue, "Mã bệnh án");
        HealthIndicator indicator = clinicalIndicator(request, recordId, user.getUserId());
        List<String> errors = validateClinical(indicator);
        if (!errors.isEmpty()) {
            loadClinicalErrorForm(request, response, records, recordId, indicator, errors);
            return;
        }

        Integer encounterId = new HealthIndicatorDAO().saveClinical(indicator);
        if (encounterId != null && encounterId > 0) {
            new ClinicWorkflowDAO().setEncounterStatus(
                    encounterId, "WAITING_DOCTOR", user.getUserId());
        }
        response.sendRedirect(request.getContextPath() + "/ClinicWorkflow?view=encounters");
    }

    private HealthIndicator clinicalIndicator(HttpServletRequest request, int recordId, int staffId) {
        HealthIndicator indicator = new HealthIndicator();
        indicator.setRecordId(recordId);
        indicator.setEnteredByStaff(staffId);
        indicator.setHeight(parseDouble(request.getParameter("height")));
        indicator.setWeight(parseDouble(request.getParameter("weight")));
        double height = indicator.getHeight();
        double weight = indicator.getWeight();
        indicator.setBmi(height > 0 && weight > 0
                ? Math.round(weight / Math.pow(height / 100, 2) * 10.0) / 10.0 : 0);
        indicator.setSystolicBp(parseInt(request.getParameter("systolicBp")));
        indicator.setDiastolicBp(parseInt(request.getParameter("diastolicBp")));
        indicator.setHeartRate(parseInt(request.getParameter("heartRate")));
        indicator.setTemperature(parseDouble(request.getParameter("temperature")));
        return indicator;
    }

    private void loadClinicalErrorForm(HttpServletRequest request, HttpServletResponse response,
            MedicalRecordDAO records, int recordId, HealthIndicator indicator,
            List<String> errors) throws ServletException, IOException {
        MedicalRecordDAO.MedicalRecordFormData data = records.loadFormData(recordId);
        if (data.record() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy bệnh án");
            return;
        }
        request.setAttribute("record", data.record());
        request.setAttribute("patient", data.patient());
        request.setAttribute("indicator", indicator);
        request.setAttribute("latestIndicator", data.latestIndicator());
        request.setAttribute("diabetesProfile", data.diabetesProfile());
        request.setAttribute("labSummary", data.labSummary());
        request.setAttribute("prescriptionItems", data.prescriptionItems());
        request.setAttribute("assignedDoctor", data.doctor());
        request.setAttribute("doctors", new DoctorDAO().getAll());
        request.setAttribute("serverErrors", errors);
        request.setAttribute("activeTab", "2");
        request.getRequestDispatcher("views/MedicalRecordForm.jsp").forward(request, response);
    }

    private void saveDiabetesProfile(HttpServletRequest request, HttpServletResponse response,
            User user, MedicalRecordDAO records, HttpSession session, String recordIdValue)
            throws IOException {
        if (!requireRole(request, response, user, "DOCTOR")) return;

        int recordId = ControllerSupport.positiveId(recordIdValue, "Mã bệnh án");
        MedicalRecord record = records.getById(recordId);
        if (record == null) {
            response.sendRedirect(request.getContextPath() + "/DoctorDashboard");
            return;
        }
        Integer doctorId = doctorIdForSession(session, user);
        if (doctorId == null || doctorId != record.getDoctorId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String diabetesType = request.getParameter("diabetesType");
        String treatmentMethod = ControllerSupport.clean(request.getParameter("treatmentMethod"));
        if (!java.util.Set.of("UNKNOWN", "TYPE_1", "TYPE_2").contains(diabetesType)) {
            redirectRecordError(request, response, recordId,
                    "Loại tiểu đường không hợp lệ.");
            return;
        }
        if (treatmentMethod.isEmpty()) treatmentMethod = null;
        if (treatmentMethod != null && !java.util.Set.of(
                "INSULIN", "ORAL_MEDICATION", "LIFESTYLE", "COMBINATION")
                .contains(treatmentMethod)) {
            redirectRecordError(request, response, recordId,
                    "Phương pháp điều trị không hợp lệ.");
            return;
        }

        java.time.LocalDate diagnosisDate;
        Double hba1cTarget;
        try {
            diagnosisDate = optionalDate(request.getParameter("diagnosisDate"));
            hba1cTarget = optionalDouble(request.getParameter("hba1cTarget"));
        } catch (IllegalArgumentException error) {
            redirectRecordError(request, response, recordId, error.getMessage());
            return;
        }
        if (diagnosisDate != null && diagnosisDate.isAfter(java.time.LocalDate.now())) {
            redirectRecordError(request, response, recordId,
                    "Ngày phát hiện không được ở tương lai.");
            return;
        }

        Patient patient = new PatientDAO().getById(record.getPatientId());
        if (diagnosisDate != null && patient != null && patient.getDateOfBirth() != null
                && diagnosisDate.isBefore(patient.getDateOfBirth())) {
            redirectRecordError(request, response, recordId,
                    "Ngày phát hiện không được trước ngày sinh của bệnh nhân.");
            return;
        }
        if (hba1cTarget != null && (hba1cTarget < 4 || hba1cTarget > 15)) {
            redirectRecordError(request, response, recordId,
                    "Mục tiêu HbA1c phải từ 4% đến 15%.");
            return;
        }
        if ("TYPE_1".equals(diabetesType) && treatmentMethod != null
                && !java.util.Set.of("INSULIN", "COMBINATION").contains(treatmentMethod)) {
            redirectRecordError(request, response, recordId,
                    "Hồ sơ đái tháo đường típ 1 cần ghi nhận phương pháp có insulin; nếu chưa rõ hãy để trống.");
            return;
        }

        new DiabetesProfileDAO().update(record.getPatientId(), diabetesType,
                diagnosisDate, treatmentMethod, hba1cTarget);
        redirectToRecordTab(request, response, recordId, 4);
    }

    private void saveConclusion(HttpServletRequest request, HttpServletResponse response,
            User user, MedicalRecordDAO records, HttpSession session, String recordIdValue)
            throws IOException {
        if (!requireRole(request, response, user, "DOCTOR")) return;

        int recordId = ControllerSupport.positiveId(recordIdValue, "Mã bệnh án");
        MedicalRecord record = records.getById(recordId);
        if (record == null) {
            response.sendRedirect(request.getContextPath() + "/DoctorDashboard");
            return;
        }
        Integer doctorId = doctorIdForSession(session, user);
        if (doctorId == null || doctorId != record.getDoctorId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String finalDiagnosis = ControllerSupport.clean(request.getParameter("finalDiagnosis"));
        if (finalDiagnosis.isEmpty() || finalDiagnosis.length() > 2000) {
            redirectRecordError(request, response, recordId,
                    "Chẩn đoán là bắt buộc và tối đa 2000 ký tự.");
            return;
        }
        if (tooLong(request.getParameter("treatmentPlan"), 3000)
                || tooLong(request.getParameter("advice"), 2000)
                || tooLong(request.getParameter("complicationNote"), 2000)) {
            redirectRecordError(request, response, recordId,
                    "Nội dung kết luận vượt quá độ dài cho phép.");
            return;
        }

        if (request.getParameter("complicationNote") != null) {
            record.setComplicationNote(request.getParameter("complicationNote"));
        }
        if (request.getParameter("finalDiagnosis") != null) {
            record.setFinalDiagnosis(request.getParameter("finalDiagnosis"));
        }
        if (request.getParameter("treatmentPlan") != null) {
            record.setTreatmentPlan(request.getParameter("treatmentPlan"));
        }
        if (request.getParameter("prescriptionNote") != null) {
            record.setPrescriptionNote(request.getParameter("prescriptionNote"));
        }
        if (request.getParameter("advice") != null) {
            record.setAdvice(request.getParameter("advice"));
        }
        if (request.getParameter("doctorNote") != null) {
            record.setDoctorNote(request.getParameter("doctorNote"));
        }

        String followUpValue = ControllerSupport.clean(request.getParameter("followUpDate"));
        if (!followUpValue.isEmpty()) {
            java.time.LocalDate followUp;
            try {
                followUp = optionalDate(followUpValue);
            } catch (IllegalArgumentException error) {
                redirectRecordError(request, response, recordId,
                        "Ngày tái khám không hợp lệ.");
                return;
            }
            if (followUp.isBefore(java.time.LocalDate.now())) {
                redirectRecordError(request, response, recordId,
                        "Ngày tái khám không được ở quá khứ.");
                return;
            }
            record.setFollowUpDate(followUp);
        }

        try {
            records.completeWithPrescription(record,
                    request.getParameterValues("medicineName"),
                    request.getParameterValues("dosage"),
                    request.getParameterValues("frequency"),
                    request.getParameterValues("durationDays"));
        } catch (IllegalArgumentException error) {
            redirectRecordError(request, response, recordId,
                    "Đơn thuốc chưa hợp lệ: " + error.getMessage());
            return;
        }
        if (record.getEncounterId() > 0) {
            new ClinicWorkflowDAO().setEncounterStatus(
                    record.getEncounterId(), "COMPLETED", user.getUserId());
        }
        response.sendRedirect(request.getContextPath()
                + "/RecordDetail?id=" + record.getRecordId());
    }

    private boolean requireRole(HttpServletRequest request, HttpServletResponse response,
            User user, String role) throws IOException {
        if (ControllerSupport.hasRole(user, role)) return true;
        ControllerSupport.redirectToLogin(request, response);
        return false;
    }

    private void redirectRecordError(HttpServletRequest request, HttpServletResponse response,
            int recordId, String message) throws IOException {
        ControllerSupport.flash(request, "recordFlash", message);
        redirectToRecordTab(request, response, recordId, 4);
    }

    private void redirectToRecordTab(HttpServletRequest request, HttpServletResponse response,
            int recordId, int tab) throws IOException {
        response.sendRedirect(request.getContextPath()
                + "/MedicalRecordForm?recordId=" + recordId + "&tab=" + tab);
    }

    private List<String> validateClinical(HealthIndicator h) {
        List<String> errors = new ArrayList<>();
        if (h.getHeight() != 0 && (h.getHeight() < 50 || h.getHeight() > 250))
            errors.add("Chiều cao bất thường: " + h.getHeight() + " cm (hợp lệ: 50–250 cm)");
        if (h.getWeight() != 0 && (h.getWeight() < 10 || h.getWeight() > 300))
            errors.add("Cân nặng bất thường: " + h.getWeight() + " kg (hợp lệ: 10–300 kg)");
        if (h.getSystolicBp() != 0 && (h.getSystolicBp() < 60 || h.getSystolicBp() > 250))
            errors.add("Huyết áp tâm thu bất thường: " + h.getSystolicBp() + " mmHg (hợp lệ: 60–250)");
        if (h.getDiastolicBp() != 0 && (h.getDiastolicBp() < 40 || h.getDiastolicBp() > 150))
            errors.add("Huyết áp tâm trương bất thường: " + h.getDiastolicBp() + " mmHg (hợp lệ: 40–150)");
        if (h.getSystolicBp() > 0 && h.getDiastolicBp() > 0 && h.getSystolicBp() <= h.getDiastolicBp())
            errors.add("Huyết áp tâm thu phải lớn hơn tâm trương");
        if (h.getHeartRate() != 0 && (h.getHeartRate() < 30 || h.getHeartRate() > 250))
            errors.add("Nhịp tim bất thường: " + h.getHeartRate() + " lần/phút (hợp lệ: 30–250)");
        if (h.getTemperature() != 0 && (h.getTemperature() < 34 || h.getTemperature() > 42))
            errors.add("Nhiệt độ bất thường: " + h.getTemperature() + " °C (hợp lệ: 34–42)");
        return errors;
    }

    private java.time.LocalDate optionalDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return java.time.LocalDate.parse(value); }
        catch (java.time.format.DateTimeParseException error) {
            throw new IllegalArgumentException("Ngày phát hiện không hợp lệ.");
        }
    }

    private Double optionalDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Double.valueOf(value); }
        catch (NumberFormatException error) {
            throw new IllegalArgumentException("Mục tiêu HbA1c không hợp lệ.");
        }
    }

    private boolean tooLong(String value, int max) { return value != null && value.length() > max; }

    private double parseDouble(String value) {
        try {
            return value == null || value.isEmpty() ? 0 : Double.parseDouble(value);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private int parseInt(String value) {
        try {
            return value == null || value.isEmpty() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    /** Doctor id never changes during a session; cache it to avoid a remote DB round-trip per action. */
    private Integer doctorIdForSession(HttpSession session, User user) {
        Object cached = session.getAttribute(ControllerSupport.DOCTOR_ID_SESSION_KEY);
        if (cached instanceof Integer doctorId) return doctorId;
        Integer doctorId = new ClinicWorkflowDAO().doctorIdForUser(user.getUserId());
        if (doctorId != null) {
            session.setAttribute(ControllerSupport.DOCTOR_ID_SESSION_KEY, doctorId);
        }
        return doctorId;
    }
}
