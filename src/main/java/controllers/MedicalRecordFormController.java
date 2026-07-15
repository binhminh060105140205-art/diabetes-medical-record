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
 * [MODIFIED - Upgrade V3]
 *
 * THAY ĐỔI DUY NHẤT: Tab 3 (saveLabIndicators) — XÓA logic chạy AIEngine + lưu AIWarnings.
 * Sau khi Doctor lưu xét nghiệm, redirect thẳng sang Tab 4 (không còn AI warning cho bác sĩ).
 *
 * Tất cả logic khác (Tab 1, 2, 4, validate) giữ nguyên 100% từ V2.
 *
 * PHÂN QUYỀN THEO TAB (không đổi):
 *   Tab 1 (saveBasic)        — STAFF
 *   Tab 2 (saveClinical)     — STAFF
 *   Tab 3 (saveLabIndicators)— STAFF (nhập từ kết quả lab) [V3: không chạy AI nữa]
 *   Tab 4 (saveConclusion)   — DOCTOR
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
            MedicalRecord rec = recDAO.getById(recordId);
            if (rec == null) { response.sendRedirect(request.getContextPath() + "/PatientList"); return; }
            if ("DOCTOR".equals(user.getRole())) {
                Integer doctorId = new ClinicWorkflowDAO().doctorIdForUser(user.getUserId());
                if (doctorId == null || doctorId != rec.getDoctorId()) { response.sendError(403); return; }
            }

            request.setAttribute("record",   rec);
            request.setAttribute("patient",  patDAO.getById(rec.getPatientId()));

            HealthIndicatorDAO hiDAO = new HealthIndicatorDAO();
            HealthIndicator indicator = hiDAO.getByRecordId(recordId);
            request.setAttribute("indicator", indicator);
            request.setAttribute("prescriptionItems", recDAO.getPrescriptionItems(recordId));

            // [V3] Không còn load AIWarning cho Doctor dashboard
            // AIWarningDAO warnDAO = new AIWarningDAO();
            // request.setAttribute("warning", warnDAO.getByRecordId(recordId)); ← ĐÃ XÓA

            if (rec.getDoctorId() > 0) request.setAttribute("assignedDoctor", docDAO.getById(rec.getDoctorId()));

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
            request.setAttribute("encounterId", encounterParam);
        }

        request.setAttribute("doctors", docDAO.getAll());
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
            if (rec.getEncounterId() > 0) new ClinicWorkflowDAO().setEncounterStatus(rec.getEncounterId(), "WAITING_DOCTOR", user.getUserId());
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
            response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + recId + "&tab=3");

        // ── TAB 3: STAFF nhập chỉ số xét nghiệm ─────────────────────────
        // [V3] KHÔNG còn chạy AIEngine + AIWarningDAO.save() ở đây
        } else if ("saveLabIndicators".equals(action)) {
            if (!"STAFF".equals(user.getRole())) { response.sendRedirect(request.getContextPath() + "/Login"); return; }
            int recId = Integer.parseInt(ridParam);
            HealthIndicatorDAO hiDAO = new HealthIndicatorDAO();
            HealthIndicator existing = hiDAO.getByRecordId(recId);
            HealthIndicator h = existing != null ? existing : new HealthIndicator();
            h.setRecordId(recId);
            h.setBloodGlucose(parseDouble(request.getParameter("bloodGlucose")));
            h.setHba1c(parseDouble(request.getParameter("hba1c")));
            h.setCholesterol(parseDouble(request.getParameter("cholesterol")));
            h.setTriglyceride(parseDouble(request.getParameter("triglyceride")));
            h.setHdlC(parseDouble(request.getParameter("hdlC")));
            h.setLdlC(parseDouble(request.getParameter("ldlC")));

            List<String> errors = validateLab(h);
            if (!errors.isEmpty()) {
                MedicalRecord rec = recDAO.getById(recId);
                request.setAttribute("indicator", h); request.setAttribute("record", rec);
                request.setAttribute("patient", new PatientDAO().getById(rec.getPatientId()));
                request.setAttribute("doctors", new DoctorDAO().getAll());
                request.setAttribute("serverErrors", errors); request.setAttribute("clinicalDone", true);
                request.setAttribute("activeTab", "3");
                request.getRequestDispatcher("views/MedicalRecordForm.jsp").forward(request, response); return;
            }
            hiDAO.save(h);

            // [V3] ĐÃ XÓA: AIEngine.analyzeWithHistory() + AIWarningDAO.save()
            // Chỉ lưu chỉ số, redirect sang Tab 4 để bác sĩ kết luận

            response.sendRedirect(request.getContextPath() + "/MedicalRecordForm?recordId=" + recId + "&tab=4");

        // ── TAB 4: DOCTOR kết luận ───────────────────────────────────────
        } else if ("saveConclusion".equals(action)) {
            if (!"DOCTOR".equals(user.getRole())) { response.sendRedirect(request.getContextPath() + "/Login"); return; }
            MedicalRecord rec = recDAO.getById(Integer.parseInt(ridParam));
            if (rec == null) { response.sendRedirect(request.getContextPath() + "/DoctorDashboard"); return; }
            Integer currentDoctorId = new ClinicWorkflowDAO().doctorIdForUser(user.getUserId());
            if (currentDoctorId == null || currentDoctorId != rec.getDoctorId()) { response.sendError(403); return; }
            if (request.getParameter("complicationNote") != null) rec.setComplicationNote(request.getParameter("complicationNote"));
            if (request.getParameter("finalDiagnosis")  != null) rec.setFinalDiagnosis(request.getParameter("finalDiagnosis"));
            if (request.getParameter("treatmentPlan")   != null) rec.setTreatmentPlan(request.getParameter("treatmentPlan"));
            if (request.getParameter("prescriptionNote")!= null) rec.setPrescriptionNote(request.getParameter("prescriptionNote"));
            if (request.getParameter("advice")          != null) rec.setAdvice(request.getParameter("advice"));
            String fup = request.getParameter("followUpDate");
            if (fup != null && !fup.isEmpty()) rec.setFollowUpDate(java.time.LocalDate.parse(fup));
            if (request.getParameter("doctorNote")      != null) rec.setDoctorNote(request.getParameter("doctorNote"));
            recDAO.completeWithPrescription(rec,
                    request.getParameterValues("medicineName"),
                    request.getParameterValues("dosage"),
                    request.getParameterValues("frequency"),
                    request.getParameterValues("durationDays"));
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

    private List<String> validateLab(HealthIndicator h) {
        List<String> errors = new ArrayList<>();
        if (h.getBloodGlucose() != 0 && (h.getBloodGlucose() < 20 || h.getBloodGlucose() > 600))
            errors.add("Đường huyết bất thường: " + h.getBloodGlucose() + " mg/dL (hợp lệ: 20–600)");
        if (h.getHba1c() != 0 && (h.getHba1c() < 3.0 || h.getHba1c() > 20.0))
            errors.add("HbA1c bất thường: " + h.getHba1c() + "% (hợp lệ: 3%–20%)");
        if (h.getCholesterol() != 0 && (h.getCholesterol() < 50 || h.getCholesterol() > 700))
            errors.add("Cholesterol bất thường: " + h.getCholesterol() + " mg/dL (hợp lệ: 50–700)");
        if (h.getTriglyceride() != 0 && (h.getTriglyceride() < 20 || h.getTriglyceride() > 2000))
            errors.add("Triglyceride bất thường: " + h.getTriglyceride() + " mg/dL (hợp lệ: 20–2000)");
        if (h.getHdlC() != 0 && (h.getHdlC() < 10 || h.getHdlC() > 150))
            errors.add("HDL-C bất thường: " + h.getHdlC() + " mg/dL (hợp lệ: 10–150)");
        if (h.getLdlC() != 0 && (h.getLdlC() < 10 || h.getLdlC() > 400))
            errors.add("LDL-C bất thường: " + h.getLdlC() + " mg/dL (hợp lệ: 10–400)");
        return errors;
    }

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
