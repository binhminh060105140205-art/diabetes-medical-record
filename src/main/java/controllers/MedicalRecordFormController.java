package controllers;

import dal.*;
import models.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import vn.diabetes.validation.Validators;

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
        if (!ControllerSupport.hasRole(user, "ADMIN", "STAFF", "DOCTOR")) {
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
            request.setAttribute("labOrders", formData.labOrders());
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

            boolean clinicalDone = indicator != null
                    && indicator.getHeight() > 0 && indicator.getWeight() > 0
                    && indicator.getSystolicBp() > 0 && indicator.getDiastolicBp() > 0
                    && indicator.getHeartRate() > 0 && indicator.getTemperature() > 0;
            request.setAttribute("clinicalDone", clinicalDone);
            setLabState(request, formData.labOrders());
        } else if (pidParam != null) {
            if (!"STAFF".equals(user.getRole())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            int patientId = ControllerSupport.positiveIdOrZero(pidParam);
            if (patientId == 0) { response.sendError(400, "Mã bệnh nhân không hợp lệ"); return; }
            int encounterId = ControllerSupport.positiveIdOrZero(encounterParam);
            if (encounterId == 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Cần chọn lượt khám trước khi tạo bệnh án");
                return;
            }
            ClinicWorkflowDAO workflow = new ClinicWorkflowDAO();
            int[] assignment;
            try {
                assignment = workflow.intakeAssignmentForEncounter(encounterId);
            } catch (IllegalArgumentException error) {
                response.sendError(HttpServletResponse.SC_CONFLICT, error.getMessage());
                return;
            }
            if (assignment[0] != patientId) {
                response.sendError(400, "Bệnh nhân không thuộc lượt khám này");
                return;
            }
            Patient patient = new PatientDAO().getById(patientId);
            if (patient == null) { response.sendError(404, "Không tìm thấy bệnh nhân"); return; }
            request.setAttribute("patient", patient);
            request.setAttribute("diabetesProfile", new DiabetesProfileDAO().getByPatientId(patientId));
            request.setAttribute("encounterId", encounterId);
            request.setAttribute("assignedDoctor", new DoctorDAO().getById(assignment[1]));
            request.setAttribute("appointmentTime", workflow.appointmentTimeForEncounter(encounterId));
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
        if (!ControllerSupport.hasRole(user, "ADMIN", "STAFF", "DOCTOR")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        HttpSession session = request.getSession();

        String recordId = request.getParameter("recordId");
        String action = ControllerSupport.clean(request.getParameter("action"));
        MedicalRecordDAO records = new MedicalRecordDAO();
        try {
            switch (action) {
                case "saveBasic" -> saveBasic(request, response, user, records);
                case "saveClinical" -> saveClinical(request, response, user, records, recordId);
                case "saveLabResults" -> saveLabResults(request, response, user, records, recordId);
                case "reviewLab" -> reviewLabResults(request, response, user, records, session, recordId);
                case "saveDiabetesProfile" ->
                        saveDiabetesProfile(request, response, user, records, session, recordId);
                case "saveConclusion" ->
                        saveConclusion(request, response, user, records, session, recordId);
                default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Thao tác bệnh án không hợp lệ");
            }
        } catch (IllegalArgumentException | IllegalStateException error) {
            String message = error instanceof IllegalStateException
                    ? "Không thể lưu dữ liệu lúc này. Vui lòng tải lại trang và thử lại."
                    : error.getMessage();
            ControllerSupport.flash(request, "recordFlash", message);
            int parsedRecordId = ControllerSupport.positiveIdOrZero(recordId);
            if (parsedRecordId > 0) {
                redirectToRecordTab(request, response, parsedRecordId, tabForAction(action));
                return;
            }
            int patientId = ControllerSupport.positiveIdOrZero(request.getParameter("patientId"));
            if (patientId == 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Mã bệnh nhân không hợp lệ");
                return;
            }
            int encounterId = ControllerSupport.positiveIdOrZero(request.getParameter("encounterId"));
            String target = request.getContextPath() + "/MedicalRecordForm?patientId=" + patientId;
            if (encounterId > 0) target += "&encounterId=" + encounterId;
            response.sendRedirect(target);
        }
    }

    private void saveBasic(HttpServletRequest request, HttpServletResponse response,
            User user, MedicalRecordDAO records) throws IOException {
        if (!requireRole(request, response, user, "STAFF")) return;

        MedicalRecord record = new MedicalRecord();
        record.setPatientId(ControllerSupport.positiveId(
                request.getParameter("patientId"), "Mã bệnh nhân"));
        int existingRecordId = ControllerSupport.positiveIdOrZero(
                request.getParameter("recordId"));
        record.setCreatedByStaff(user.getUserId());

        if (existingRecordId > 0) {
            MedicalRecord existing = records.getById(existingRecordId);
            ensureDraftRecord(existing);
            record.setRecordId(existing.getRecordId());
            record.setPatientId(existing.getPatientId());
            record.setDoctorId(existing.getDoctorId());
            record.setEncounterId(existing.getEncounterId());
        } else {
            String encounterId = ControllerSupport.clean(request.getParameter("encounterId"));
            record.setEncounterId(ControllerSupport.positiveId(encounterId, "Mã lượt khám"));
            int[] assignment = new ClinicWorkflowDAO()
                    .intakeAssignmentForEncounter(record.getEncounterId());
            record.setPatientId(assignment[0]);
            record.setDoctorId(assignment[1]);
        }

        String reasonForVisit = Validators.max(Validators.required(
                request.getParameter("reasonForVisit"), "Lý do đến khám"), 255, "Lý do đến khám");
        if (reasonForVisit.length() < 5) {
            throw new IllegalArgumentException("Lý do đến khám phải có ít nhất 5 ký tự.");
        }
        record.setReasonForVisit(reasonForVisit);
        record.setSymptoms(Validators.max(request.getParameter("symptoms"), 2000, "Triệu chứng"));
        record.setMedicalHistory(Validators.max(
                request.getParameter("medicalHistory"), 2000, "Tiền sử bệnh"));
        record.setLifestyleHabits(Validators.max(
                request.getParameter("lifestyleHabits"), 2000, "Thói quen sinh hoạt"));
        record.setClinicalExam(Validators.max(
                request.getParameter("clinicalExam"), 2000, "Ghi chú lâm sàng"));
        records.saveBasic(record);
        response.sendRedirect(request.getContextPath()
                + "/MedicalRecordForm?recordId=" + record.getRecordId() + "&tab=2");
    }

    private void saveClinical(HttpServletRequest request, HttpServletResponse response,
            User user, MedicalRecordDAO records, String recordIdValue)
            throws ServletException, IOException {
        if (!requireRole(request, response, user, "STAFF")) return;

        int recordId = ControllerSupport.positiveId(recordIdValue, "Mã bệnh án");
        ensureDraftRecord(records.getById(recordId));
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
        indicator.setHeight(parseDouble(request.getParameter("height"), "Chiều cao"));
        indicator.setWeight(parseDouble(request.getParameter("weight"), "Cân nặng"));
        double height = indicator.getHeight();
        double weight = indicator.getWeight();
        indicator.setBmi(height > 0 && weight > 0
                ? Math.round(weight / Math.pow(height / 100, 2) * 10.0) / 10.0 : 0);
        indicator.setSystolicBp(parseInt(request.getParameter("systolicBp"), "Huyết áp tâm thu"));
        indicator.setDiastolicBp(parseInt(request.getParameter("diastolicBp"), "Huyết áp tâm trương"));
        indicator.setHeartRate(parseInt(request.getParameter("heartRate"), "Nhịp tim"));
        indicator.setTemperature(parseDouble(request.getParameter("temperature"), "Nhiệt độ"));
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
        request.setAttribute("labOrders", data.labOrders());
        request.setAttribute("prescriptionItems", data.prescriptionItems());
        request.setAttribute("assignedDoctor", data.doctor());
        request.setAttribute("doctors", new DoctorDAO().getAll());
        request.setAttribute("serverErrors", errors);
        request.setAttribute("activeTab", "2");
        request.getRequestDispatcher("views/MedicalRecordForm.jsp").forward(request, response);
    }

    private void saveLabResults(HttpServletRequest request, HttpServletResponse response,
            User user, MedicalRecordDAO records, String recordIdValue) throws IOException {
        if (!requireRole(request, response, user, "STAFF")) return;
        int recordId = ControllerSupport.positiveId(recordIdValue, "Mã bệnh án");
        MedicalRecord record = records.getById(recordId);
        if (record == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy bệnh án");
            return;
        }
        ensureDraftRecord(record);

        try {
            Double bloodGlucose = labValue(request, "bloodGlucose", "Đường huyết lúc đói", 1, 40);
            Double hba1c = labValue(request, "hba1c", "HbA1c", 2, 20);
            Double cholesterol = labValue(request, "cholesterol", "Cholesterol", 0.1, 30);
            Double triglyceride = labValue(request, "triglyceride", "Triglyceride", 0.1, 30);
            Double hdlC = labValue(request, "hdlC", "HDL-C", 0.1, 15);
            Double ldlC = labValue(request, "ldlC", "LDL-C", 0.1, 20);
            if (java.util.stream.Stream.of(bloodGlucose, hba1c, cholesterol,
                    triglyceride, hdlC, ldlC).allMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("Cần nhập ít nhất một kết quả xét nghiệm.");
            }

            ClinicWorkflowDAO workflow = new ClinicWorkflowDAO();
            if (!workflow.hasOpenLabOrdersForRecord(recordId)) {
                throw new IllegalArgumentException(
                        "Bác sĩ chưa tạo chỉ định xét nghiệm cho lượt khám này.");
            }
            workflow.resultStructuredLabs(recordId, user.getUserId(), bloodGlucose,
                    hba1c, cholesterol, triglyceride, hdlC, ldlC);
            ControllerSupport.flash(request, "recordSuccess",
                    "Đã lưu kết quả xét nghiệm. Chờ bác sĩ xác nhận.");
        } catch (IllegalArgumentException error) {
            ControllerSupport.flash(request, "recordFlash", error.getMessage());
        }
        redirectToRecordTab(request, response, recordId, 3);
    }

    private void reviewLabResults(HttpServletRequest request, HttpServletResponse response,
            User user, MedicalRecordDAO records, HttpSession session, String recordIdValue)
            throws IOException {
        if (!requireRole(request, response, user, "DOCTOR")) return;
        int recordId = ControllerSupport.positiveId(recordIdValue, "Mã bệnh án");
        MedicalRecord record = records.getById(recordId);
        if (record == null || record.getEncounterId() <= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy lượt khám");
            return;
        }
        Integer doctorId = doctorIdForSession(session, user);
        if (doctorId == null || doctorId != record.getDoctorId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            new ClinicWorkflowDAO().reviewLabResults(
                    record.getEncounterId(), doctorId, user.getUserId());
            ControllerSupport.flash(request, "recordSuccess", "Đã xác nhận kết quả xét nghiệm.");
        } catch (IllegalArgumentException error) {
            ControllerSupport.flash(request, "recordFlash", error.getMessage());
        }
        redirectToRecordTab(request, response, recordId, 3);
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
        ensureDraftRecord(record);
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
        ensureDraftRecord(record);
        Integer doctorId = doctorIdForSession(session, user);
        if (doctorId == null || doctorId != record.getDoctorId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        DiabetesProfile diabetesProfile = new DiabetesProfileDAO()
                .getByPatientId(record.getPatientId());
        if (diabetesProfile == null || diabetesProfile.isUnknown()) {
            redirectRecordError(request, response, recordId,
                    "Bác sĩ cần xác nhận bệnh nhân thuộc típ 1 hay típ 2 trước khi hoàn tất bệnh án.");
            return;
        }
        if ("TYPE_1".equals(diabetesProfile.getDiabetesType())
                && !java.util.Set.of("INSULIN", "COMBINATION")
                        .contains(diabetesProfile.getTreatmentMethod())) {
            redirectRecordError(request, response, recordId,
                    "Bệnh nhân típ 1 cần có phương pháp điều trị chứa insulin trước khi hoàn tất.");
            return;
        }

        String finalDiagnosis = ControllerSupport.clean(request.getParameter("finalDiagnosis"));
        if (finalDiagnosis.length() < 5 || finalDiagnosis.length() > 255) {
            redirectRecordError(request, response, recordId,
                    "Chẩn đoán phải có từ 5 đến 255 ký tự.");
            return;
        }
        try {
            record.setComplicationNote(Validators.max(
                    request.getParameter("complicationNote"), 2000, "Ghi chú biến chứng"));
            record.setFinalDiagnosis(finalDiagnosis);
            record.setTreatmentPlan(Validators.max(
                    request.getParameter("treatmentPlan"), 3000, "Hướng điều trị"));
            record.setPrescriptionNote(Validators.max(
                    request.getParameter("prescriptionNote"), 500, "Ghi chú đơn thuốc"));
            record.setAdvice(Validators.max(
                    request.getParameter("advice"), 2000, "Lời dặn bệnh nhân"));
            record.setDoctorNote(Validators.max(
                    request.getParameter("doctorNote"), 1000, "Ghi chú bác sĩ"));
        } catch (IllegalArgumentException error) {
            redirectRecordError(request, response, recordId, error.getMessage());
            return;
        }

        if (record.getEncounterId() > 0
                && new ClinicWorkflowDAO().hasUnreviewedLabOrders(record.getEncounterId())) {
            redirectRecordError(request, response, recordId,
                    "Kết quả xét nghiệm phải được nhập và bác sĩ xác nhận trước khi hoàn tất bệnh án.");
            return;
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
            if (!followUp.isAfter(java.time.LocalDate.now())) {
                redirectRecordError(request, response, recordId,
                        "Ngày tái khám phải bắt đầu từ ngày mai.");
                return;
            }
            record.setFollowUpDate(followUp);
        }

        try {
            records.completeWithPrescription(record,
                    request.getParameterValues("medicineName"),
                    request.getParameterValues("dosage"),
                    request.getParameterValues("frequency"),
                    request.getParameterValues("durationDays"),
                    doctorId, user.getUserId());
        } catch (IllegalArgumentException error) {
            redirectRecordError(request, response, recordId,
                    "Đơn thuốc chưa hợp lệ: " + error.getMessage());
            return;
        }
        response.sendRedirect(request.getContextPath()
                + "/RecordDetail?id=" + record.getRecordId());
    }

    private boolean requireRole(HttpServletRequest request, HttpServletResponse response,
            User user, String role) throws IOException {
        if (ControllerSupport.hasRole(user, role)) return true;
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
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
        if (h.getHeight() == 0) errors.add("Chiều cao là bắt buộc");
        if (h.getWeight() == 0) errors.add("Cân nặng là bắt buộc");
        if (h.getSystolicBp() == 0) errors.add("Huyết áp tâm thu là bắt buộc");
        if (h.getDiastolicBp() == 0) errors.add("Huyết áp tâm trương là bắt buộc");
        if (h.getHeartRate() == 0) errors.add("Nhịp tim là bắt buộc");
        if (h.getTemperature() == 0) errors.add("Nhiệt độ là bắt buộc");
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
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) throw new NumberFormatException();
            return parsed;
        }
        catch (NumberFormatException error) {
            throw new IllegalArgumentException("Mục tiêu HbA1c không hợp lệ.");
        }
    }

    private Double labValue(HttpServletRequest request, String name, String label,
            double min, double max) {
        String raw = ControllerSupport.clean(request.getParameter(name));
        if (raw.isEmpty()) return null;
        try {
            double value = Double.parseDouble(raw);
            if (!Double.isFinite(value) || value < min || value > max) {
                throw new IllegalArgumentException(label + " phải từ " + min + " đến " + max + ".");
            }
            return value;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
    }

    private void setLabState(HttpServletRequest request,
            List<java.util.Map<String, Object>> labOrders) {
        boolean hasOrders = labOrders != null && !labOrders.isEmpty();
        boolean allReady = hasOrders;
        boolean allReviewed = hasOrders;
        boolean orderedGlucose = false;
        boolean orderedHba1c = false;
        boolean orderedLipid = false;
        if (hasOrders) {
            for (java.util.Map<String, Object> order : labOrders) {
                String status = String.valueOf(order.get("status"));
                String code = String.valueOf(order.get("test_code"));
                if (java.util.Set.of("ORDERED", "COLLECTED").contains(status)) {
                    orderedGlucose |= java.util.Set.of("GLU", "GLU_FASTING").contains(code);
                    orderedHba1c |= "HBA1C".equals(code);
                    orderedLipid |= "LIPID".equals(code);
                }
                if (!java.util.Set.of("RESULTED", "REVIEWED", "CANCELLED").contains(status)) {
                    allReady = false;
                }
                if (!java.util.Set.of("REVIEWED", "CANCELLED").contains(status)) {
                    allReviewed = false;
                }
            }
        }
        request.setAttribute("hasLabOrders", hasOrders);
        request.setAttribute("labResultsReady", allReady);
        request.setAttribute("labReviewed", allReviewed);
        request.setAttribute("orderedGlucose", orderedGlucose);
        request.setAttribute("orderedHba1c", orderedHba1c);
        request.setAttribute("orderedLipid", orderedLipid);
    }

    private double parseDouble(String value, String label) {
        try {
            if (value == null || value.isBlank()) return 0;
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
    }

    private int parseInt(String value, String label) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
    }

    private int tabForAction(String action) {
        return switch (action) {
            case "saveClinical" -> 2;
            case "saveLabResults", "reviewLab" -> 3;
            case "saveDiabetesProfile", "saveConclusion" -> 4;
            default -> 1;
        };
    }

    private void ensureDraftRecord(MedicalRecord record) {
        if (record == null) throw new IllegalArgumentException("Không tìm thấy bệnh án.");
        if (!"DRAFT".equals(record.getStatus())) {
            throw new IllegalArgumentException(
                    "Bệnh án đã hoàn tất nên chỉ được xem, không thể chỉnh sửa.");
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
