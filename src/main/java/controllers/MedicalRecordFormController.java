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
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || (!user.getRole().equals("STAFF") && !user.getRole().equals("DOCTOR"))) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }

        String pidParam = request.getParameter("patientId");
        String encounterParam = request.getParameter("encounterId");
        String ridParam = request.getParameter("recordId");
        PatientDAO patDAO = new PatientDAO();
        DoctorDAO  docDAO = new DoctorDAO();

        if (ridParam != null) {
            int recordId = positiveId(ridParam);
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

            // [V3] Không còn load AIWarning cho Doctor dashboard
            // AIWarningDAO warnDAO = new AIWarningDAO();
            // request.setAttribute("warning", warnDAO.getByRecordId(recordId)); ← ĐÃ XÓA

            if (assignedDoctor != null) request.setAttribute("assignedDoctor", assignedDoctor);

            boolean clinicalDone = indicator != null && (indicator.getHeight() > 0 || indicator.getSystolicBp() > 0);
            boolean labDone      = indicator != null && (indicator.getBloodGlucose() > 0 || indicator.getHba1c() > 0);
            request.setAttribute("clinicalDone", clinicalDone);
            request.setAttribute("labDone",      labDone);
        } else if (pidParam != null) {
            int patientId = positiveId(pidParam);
            if (patientId == 0) { response.sendError(400, "Mã bệnh nhân không hợp lệ"); return; }
            Patient patient = patDAO.getById(patientId);
            if (patient == null) { response.sendError(404, "Không tìm thấy bệnh nhân"); return; }
            request.setAttribute("patient", patient);
            request.setAttribute("diabetesProfile", new DiabetesProfileDAO().getByPatientId(patientId));
            request.setAttribute("encounterId", encounterParam);
            if (encounterParam != null && !encounterParam.isBlank()) request.setAttribute("appointmentTime", new ClinicWorkflowDAO().appointmentTimeForEncounter(positiveId(encounterParam)));
        }

        if ("STAFF".equals(user.getRole())) request.setAttribute("doctors", docDAO.getAll());
        request.getRequestDispatcher("views/MedicalRecordForm.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) { response.sendRedirect(request.getContextPath() + "/Login"); return; }

        String action   = request.getParameter("action");
        String ridParam = request.getParameter("recordId");
        MedicalRecordDAO recDAO = new MedicalRecordDAO();

        // ── TAB 1: STAFF tạo bệnh án ────────────────────────────────────
        if ("saveBasic".equals(action)) {
            if (!"STAFF".equals(user.getRole())) { response.sendRedirect(request.getContextPath() + "/Login"); return; }
            MedicalRecord rec = new MedicalRecord();
            rec.setPatientId(Integer.parseInt(request.getParameter("patientId")));
            String docIdParam = request.getParameter("doctorId");
            if (docIdParam != null && !docIdParam.isEmpty()) rec.setDoctorId(Integer.parseInt(docIdParam));
            rec.setCreatedByStaff(user.getUserId());
            String encounterId = request.getParameter("encounterId");
            if (encounterId != null && !encounterId.isBlank()) {
                rec.setEncounterId(Integer.parseInt(encounterId));
                int[] assignment = new ClinicWorkflowDAO().assignmentForEncounter(rec.getEncounterId());
                rec.setPatientId(assignment[0]);
                rec.setDoctorId(assignment[1]);
            }
            rec.setReasonForVisit(request.getParameter("reasonForVisit"));
            rec.setSymptoms(request.getParameter("symptoms"));
            rec.setMedicalHistory(request.getParameter("medicalHistory"));
            rec.setLifestyleHabits(request.getParameter("lifestyleHabits"));
            rec.setClinicalExam(request.getParameter("clinicalExam"));
            rec = recDAO.create(rec);
            response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + rec.getRecordId() + "&tab=2");

        // ── TAB 2: STAFF nhập chỉ số lâm sàng ──────────────────────────
        } else if ("saveClinical".equals(action)) {
            if (!"STAFF".equals(user.getRole())) { response.sendRedirect(request.getContextPath() + "/Login"); return; }
            int recId = Integer.parseInt(ridParam);
            HealthIndicatorDAO hiDAO = new HealthIndicatorDAO();
            HealthIndicator existing = hiDAO.getByRecordId(recId);
            HealthIndicator h = existing != null ? existing : new HealthIndicator();
            h.setRecordId(recId);
            h.setEnteredByStaff(user.getUserId());
            h.setHeight(parseDouble(request.getParameter("height")));
            h.setWeight(parseDouble(request.getParameter("weight")));
            double ht = h.getHeight(), wt = h.getWeight();
            h.setBmi(ht > 0 && wt > 0 ? Math.round((wt / ((ht/100)*(ht/100))) * 10.0) / 10.0 : 0);
            h.setSystolicBp(parseInt(request.getParameter("systolicBp")));
            h.setDiastolicBp(parseInt(request.getParameter("diastolicBp")));
            h.setHeartRate(parseInt(request.getParameter("heartRate")));
            h.setTemperature(parseDouble(request.getParameter("temperature")));

            List<String> errors = validateClinical(h);
            if (!errors.isEmpty()) {
                MedicalRecord rec = recDAO.getById(recId);
                request.setAttribute("indicator", h); request.setAttribute("record", rec);
                request.setAttribute("patient", new PatientDAO().getById(rec.getPatientId()));
                request.setAttribute("doctors", new DoctorDAO().getAll());
                request.setAttribute("serverErrors", errors); request.setAttribute("activeTab", "2");
                request.getRequestDispatcher("views/MedicalRecordForm.jsp").forward(request, response); return;
            }
            hiDAO.save(h);
            MedicalRecord savedRecord = recDAO.getById(recId);
            if (savedRecord != null && savedRecord.getEncounterId() > 0)
                new ClinicWorkflowDAO().setEncounterStatus(savedRecord.getEncounterId(), "WAITING_DOCTOR", user.getUserId());
            response.sendRedirect(request.getContextPath() + "/ClinicWorkflow?view=encounters");

        // Hồ sơ tiểu đường: chỉ bác sĩ phụ trách được xác nhận/cập nhật.
        } else if ("saveDiabetesProfile".equals(action)) {
            if (!"DOCTOR".equals(user.getRole())) { response.sendRedirect(request.getContextPath() + "/Login"); return; }
            MedicalRecord recForProfile = recDAO.getById(Integer.parseInt(ridParam));
            if (recForProfile == null) { response.sendRedirect(request.getContextPath() + "/DoctorDashboard"); return; }
            Integer currentDoctorId = new ClinicWorkflowDAO().doctorIdForUser(user.getUserId());
            if (currentDoctorId == null || currentDoctorId != recForProfile.getDoctorId()) {
                response.sendError(403); return;
            }
            String diabetesType = request.getParameter("diabetesType");
            String treatmentMethod = request.getParameter("treatmentMethod");
            if (!java.util.Set.of("UNKNOWN","TYPE_1","TYPE_2").contains(diabetesType)) {
                flash(request, "Loại tiểu đường không hợp lệ.");
                response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
            }
            if (treatmentMethod != null && treatmentMethod.isBlank()) treatmentMethod = null;
            if (treatmentMethod != null && !java.util.Set.of("INSULIN","ORAL_MEDICATION","LIFESTYLE","COMBINATION").contains(treatmentMethod)) {
                flash(request, "Phương pháp điều trị không hợp lệ.");
                response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
            }
            String diagnosisDateParam = request.getParameter("diagnosisDate");
            String hba1cTargetParam = request.getParameter("hba1cTarget");
            java.time.LocalDate diagnosisDate;
            Double hba1cTarget;
            try {
                diagnosisDate = optionalDate(diagnosisDateParam);
                hba1cTarget = optionalDouble(hba1cTargetParam);
            } catch (IllegalArgumentException error) {
                flash(request, error.getMessage());
                response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
            }
            if (diagnosisDate != null && diagnosisDate.isAfter(java.time.LocalDate.now())) {
                flash(request, "Ngày phát hiện không được ở tương lai.");
                response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
            }
            Patient profilePatient = new PatientDAO().getById(recForProfile.getPatientId());
            if (diagnosisDate != null && profilePatient != null && profilePatient.getDateOfBirth() != null
                    && diagnosisDate.isBefore(profilePatient.getDateOfBirth())) {
                flash(request, "Ngày phát hiện không được trước ngày sinh của bệnh nhân.");
                response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
            }
            if (hba1cTarget != null && (hba1cTarget < 4 || hba1cTarget > 15)) {
                flash(request, "Mục tiêu HbA1c phải từ 4% đến 15%.");
                response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
            }
            if ("TYPE_1".equals(diabetesType) && treatmentMethod != null
                    && !java.util.Set.of("INSULIN", "COMBINATION").contains(treatmentMethod)) {
                flash(request, "Hồ sơ Type 1 cần ghi nhận phương pháp có insulin; nếu chưa rõ hãy để trống.");
                response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
            }
            new DiabetesProfileDAO().update(recForProfile.getPatientId(), diabetesType, diagnosisDate, treatmentMethod, hba1cTarget);
            response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4");

        // ── TAB 4: DOCTOR kết luận ───────────────────────────────────────
        } else if ("saveConclusion".equals(action)) {
            if (!"DOCTOR".equals(user.getRole())) { response.sendRedirect(request.getContextPath() + "/Login"); return; }
            MedicalRecord rec = recDAO.getById(Integer.parseInt(ridParam));
            if (rec == null) { response.sendRedirect(request.getContextPath() + "/DoctorDashboard"); return; }
            Integer currentDoctorId = new ClinicWorkflowDAO().doctorIdForUser(user.getUserId());
            if (currentDoctorId == null || currentDoctorId != rec.getDoctorId()) { response.sendError(403); return; }
            String finalDiagnosis = clean(request.getParameter("finalDiagnosis"));
            if (finalDiagnosis.isEmpty() || finalDiagnosis.length() > 2000) {
                flash(request, "Chẩn đoán là bắt buộc và tối đa 2000 ký tự.");
                response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
            }
            if (tooLong(request.getParameter("treatmentPlan"), 3000)
                    || tooLong(request.getParameter("advice"), 2000)
                    || tooLong(request.getParameter("complicationNote"), 2000)) {
                flash(request, "Nội dung kết luận vượt quá độ dài cho phép.");
                response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
            }
            if (request.getParameter("complicationNote") != null) rec.setComplicationNote(request.getParameter("complicationNote"));
            if (request.getParameter("finalDiagnosis")  != null) rec.setFinalDiagnosis(request.getParameter("finalDiagnosis"));
            if (request.getParameter("treatmentPlan")   != null) rec.setTreatmentPlan(request.getParameter("treatmentPlan"));
            if (request.getParameter("prescriptionNote")!= null) rec.setPrescriptionNote(request.getParameter("prescriptionNote"));
            if (request.getParameter("advice")          != null) rec.setAdvice(request.getParameter("advice"));
            String fup = request.getParameter("followUpDate");
            if (fup != null && !fup.isEmpty()) {
                java.time.LocalDate followUp;
                try { followUp = optionalDate(fup); }
                catch (IllegalArgumentException error) {
                    flash(request, "Ngày tái khám không hợp lệ.");
                    response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
                }
                if (followUp.isBefore(java.time.LocalDate.now())) {
                    flash(request, "Ngày tái khám không được ở quá khứ.");
                    response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
                }
                rec.setFollowUpDate(followUp);
            }
            if (request.getParameter("doctorNote")      != null) rec.setDoctorNote(request.getParameter("doctorNote"));
            try {
                recDAO.completeWithPrescription(rec,
                        request.getParameterValues("medicineName"),
                        request.getParameterValues("dosage"),
                        request.getParameterValues("frequency"),
                        request.getParameterValues("durationDays"));
            } catch (IllegalArgumentException error) {
                flash(request, "Đơn thuốc chưa hợp lệ: " + error.getMessage());
                response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + ridParam + "&tab=4"); return;
            }
            if (rec.getEncounterId() > 0) new ClinicWorkflowDAO().setEncounterStatus(rec.getEncounterId(), "COMPLETED", user.getUserId());
            // [V3] Bỏ AIWarningDAO.markReviewed() — bảng đã xóa
            response.sendRedirect(request.getContextPath() + "/RecordDetail?id=" + rec.getRecordId());
        }
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

    private void flash(HttpServletRequest request, String message) {
        request.getSession().setAttribute("recordFlash", message);
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

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private boolean tooLong(String value, int max) { return value != null && value.length() > max; }

    private double parseDouble(String s) { try { return s != null && !s.isEmpty() ? Double.parseDouble(s) : 0; } catch (Exception e) { return 0; } }
    private int    parseInt(String s)    { try { return s != null && !s.isEmpty() ? Integer.parseInt(s) : 0;  } catch (Exception e) { return 0; } }
    private int positiveId(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) return parsed;
        } catch (NumberFormatException | NullPointerException ignored) { }
        return 0;
    }
}
