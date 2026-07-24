package controllers;

import dal.ClinicWorkflowDAO;
import dal.PatientDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.Set;
import models.User;
import vn.diabetes.service.ClinicWorkflowService;
import vn.diabetes.validation.AppointmentRules;

/**
 * Coordinates the outpatient workflow screens.
 *
 * <p>The controller only translates HTTP parameters into service calls. Business
 * validation remains in {@link ClinicWorkflowService} and the database gateway.
 * Keeping each action in a named method makes the role rules easy to review.</p>
 */
@WebServlet("/ClinicWorkflow")
public class ClinicWorkflowController extends HttpServlet {

    private static final Set<String> RECEPTION_ROLES = Set.of("ADMIN", "STAFF");
    private static final Set<String> SUPPORTED_VIEWS =
            Set.of("appointments", "encounters", "clinical", "labs");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "ADMIN", "STAFF", "DOCTOR")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }

        ClinicWorkflowDAO workflow = new ClinicWorkflowDAO();
        String view = normalizeView(request.getParameter("view"), user.getRole());
        request.setAttribute("view", view);
        request.setAttribute("today", AppointmentRules.nowInVietnam().toLocalDate().toString());

        loadViewData(request, response, user, workflow, view);
        if (response.isCommitted()) return;

        if ("clinical".equals(view)) {
            loadSelectedPatient(request, response, user, workflow);
            if (response.isCommitted()) return;
        }

        moveFlashMessageToRequest(request);
        request.getRequestDispatcher("views/ClinicWorkflow.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "ADMIN", "STAFF", "DOCTOR")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        ClinicWorkflowDAO workflow = new ClinicWorkflowDAO();
        ClinicWorkflowService service = new ClinicWorkflowService(workflow);
        String destinationView = "encounters";

        try {
            destinationView = executeAction(request, user, workflow, service);
            ControllerSupport.flash(request, "workflowFlash", "Đã cập nhật thành công");
        } catch (IllegalArgumentException | SecurityException error) {
            ControllerSupport.flash(request, "workflowFlash",
                    "Không thể cập nhật: " + error.getMessage());
        } catch (IllegalStateException error) {
            ControllerSupport.flash(request, "workflowFlash",
                    "Không thể cập nhật dữ liệu lúc này. Vui lòng tải lại trang và thử lại.");
        } catch (Exception error) {
            throw new ServletException("Lỗi cập nhật quy trình khám", error);
        }

        redirectToWorkflow(request, response, destinationView);
    }

    private void loadViewData(HttpServletRequest request, HttpServletResponse response,
            User user, ClinicWorkflowDAO workflow, String view) throws IOException {
        if ("labs".equals(view)) {
            request.setAttribute("labTests", ClinicWorkflowService.labTests());
        }
        if ("clinical".equals(view)) {
            request.setAttribute("patients", "DOCTOR".equals(user.getRole())
                    ? new PatientDAO().listForDoctorSelection(user.getUserId())
                    : new PatientDAO().listForSelection());
        }
        if ("appointments".equals(view)) {
            ClinicWorkflowDAO.AppointmentOperationsPageData page =
                    workflow.loadAppointmentOperationsPage();
            request.setAttribute("patients", page.patients());
            request.setAttribute("doctors", page.doctors());
            request.setAttribute("appointments", page.appointments());
            request.setAttribute("appointmentDates",
                    ControllerSupport.appointmentDateOptions(true));
            request.setAttribute("appointmentTimeSlots",
                    ControllerSupport.appointmentTimeOptions());
            LocalDateTime now = AppointmentRules.nowInVietnam();
            request.setAttribute("appointmentToday", now.toLocalDate().toString());
            request.setAttribute("appointmentMinDateTime",
                    ControllerSupport.appointmentDateTimeInputValue(
                            ControllerSupport.nextAppointmentSlot(now)));
            request.setAttribute("appointmentMaxDateTime",
                    ControllerSupport.appointmentDateTimeInputValue(
                            now.toLocalDate().plusDays(AppointmentRules.MAX_ADVANCE_DAYS)
                                    .atTime(AppointmentRules.CLOSE_TIME.minusMinutes(
                                            AppointmentRules.SLOT_MINUTES))));
        }

        if ("DOCTOR".equals(user.getRole())) {
            loadDoctorViewData(request, response, user, workflow, view);
        } else if ("encounters".equals(view)) {
            request.setAttribute("encounters", workflow.encounters());
        } else if ("labs".equals(view)) {
            request.setAttribute("labOrders", workflow.labOrders());
            request.setAttribute("labImportRecords", workflow.labImportRecords());
        }
    }

    private void loadDoctorViewData(HttpServletRequest request, HttpServletResponse response,
            User user, ClinicWorkflowDAO workflow, String view) throws IOException {
        Integer doctorId = doctorIdForSession(request, user, workflow);
        if (doctorId == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Tài khoản chưa có hồ sơ bác sĩ");
            return;
        }

        if (Set.of("encounters", "labs").contains(view)) {
            request.setAttribute("encounters", workflow.encountersForDoctor(doctorId));
        }
        if ("labs".equals(view)) {
            request.setAttribute("labOrders", workflow.labOrdersForDoctor(doctorId));
        }
    }

    private void loadSelectedPatient(HttpServletRequest request, HttpServletResponse response,
            User user, ClinicWorkflowDAO workflow) throws IOException {
        String patientIdValue = request.getParameter("patientId");
        if (patientIdValue == null || patientIdValue.isBlank()) return;

        int patientId = ControllerSupport.positiveIdOrZero(patientIdValue);
        if (patientId == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Mã bệnh nhân không hợp lệ");
            return;
        }

        if ("DOCTOR".equals(user.getRole())) {
            Integer doctorId = doctorIdForSession(request, user, workflow);
            if (doctorId == null || !workflow.doctorHasPatient(doctorId, patientId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bệnh nhân không thuộc phạm vi phụ trách của bác sĩ");
                return;
            }
        }

        ClinicWorkflowDAO.ClinicalPatientData data = workflow.loadClinicalPatient(patientId);
        if (data.patient() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Không tìm thấy bệnh nhân");
            return;
        }

        request.setAttribute("selectedPatient", data.patient());
        request.setAttribute("allergies", data.allergies());
        request.setAttribute("histories", data.histories());
    }

    private String executeAction(HttpServletRequest request, User user,
            ClinicWorkflowDAO workflow, ClinicWorkflowService service) {
        String action = request.getParameter("action");
        if (action == null) throw forbiddenAction();

        return switch (action) {
            case "createAppointment" -> createAppointment(request, user, service);
            case "assignAppointmentRequest" -> assignAppointment(request, user, service);
            case "rescheduleAppointment" -> rescheduleAppointment(request, user, service);
            case "appointmentStatus" -> updateAppointmentStatus(request, user, service);
            case "checkIn" -> checkIn(request, user, service);
            case "status" -> updateEncounterStatus(request, user, workflow, service);
            case "allergy" -> addAllergy(request, user, workflow, service);
            case "history" -> addMedicalHistory(request, user, workflow, service);
            case "labOrder" -> createLabOrder(request, user, workflow, service);
            case "labResult" -> addLabResult(request, user, service);
            default -> throw forbiddenAction();
        };
    }

    private String createAppointment(HttpServletRequest request, User user,
            ClinicWorkflowService service) {
        requireReception(user);
        service.createAppointment(
                positiveParameter(request, "patientId"),
                positiveParameter(request, "doctorId"),
                appointmentDateTime(request),
                ControllerSupport.requiredParameter(request, "reason"),
                request.getParameter("note"),
                user.getUserId());
        return "appointments";
    }

    private String assignAppointment(HttpServletRequest request, User user,
            ClinicWorkflowService service) {
        requireReception(user);
        service.assignAppointmentRequest(
                positiveParameter(request, "appointmentId"),
                positiveParameter(request, "doctorId"),
                appointmentDateTime(request),
                request.getParameter("note"),
                user.getUserId());
        return "appointments";
    }

    private String rescheduleAppointment(HttpServletRequest request, User user,
            ClinicWorkflowService service) {
        requireReception(user);
        service.rescheduleAppointment(
                positiveParameter(request, "appointmentId"),
                appointmentDateTime(request),
                request.getParameter("note"),
                user.getUserId());
        return "appointments";
    }

    private String updateAppointmentStatus(HttpServletRequest request, User user,
            ClinicWorkflowService service) {
        requireReception(user);
        service.setAppointmentStatus(
                positiveParameter(request, "appointmentId"),
                ControllerSupport.requiredParameter(request, "status"),
                user.getUserId());
        return "appointments";
    }

    private String checkIn(HttpServletRequest request, User user,
            ClinicWorkflowService service) {
        requireReception(user);
        service.checkIn(positiveParameter(request, "appointmentId"), user.getUserId());
        return "encounters";
    }

    private String updateEncounterStatus(HttpServletRequest request, User user,
            ClinicWorkflowDAO workflow, ClinicWorkflowService service) {
        requireDoctor(user);
        int encounterId = positiveParameter(request, "encounterId");
        int doctorId = requireAssignedDoctor(request, user, workflow, encounterId);

        String status = ControllerSupport.requiredParameter(request, "status");
        // Doctors start/resume a visit here; completion is only allowed through
        // the conclusion transaction so the medical record cannot be skipped.
        if (!"IN_CONSULTATION".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException(
                    "Chỉ được bắt đầu hoặc tiếp tục lượt khám tại bước này.");
        }
        service.setEncounterStatus(
                encounterId, status, user.getUserId());
        return "encounters";
    }

    private String addAllergy(HttpServletRequest request, User user,
            ClinicWorkflowDAO workflow, ClinicWorkflowService service) {
        requireDoctor(user);
        int patientId = requireAssignedPatient(request, user, workflow);
        service.addAllergy(
                patientId,
                ControllerSupport.requiredParameter(request, "allergen"),
                request.getParameter("reaction"),
                request.getParameter("severity"),
                user.getUserId());
        return "clinical";
    }

    private String addMedicalHistory(HttpServletRequest request, User user,
            ClinicWorkflowDAO workflow, ClinicWorkflowService service) {
        requireDoctor(user);
        int patientId = requireAssignedPatient(request, user, workflow);
        String diagnosedDate = request.getParameter("diagnosedDate");
        service.addHistory(
                patientId,
                request.getParameter("historyType"),
                ControllerSupport.requiredParameter(request, "conditionName"),
                diagnosedDate == null || diagnosedDate.isBlank()
                        ? null : Date.valueOf(diagnosedDate),
                request.getParameter("historyStatus"),
                request.getParameter("historyNote"),
                user.getUserId());
        return "clinical";
    }

    private String createLabOrder(HttpServletRequest request, User user,
            ClinicWorkflowDAO workflow, ClinicWorkflowService service) {
        requireDoctor(user);
        int encounterId = positiveParameter(request, "encounterId");
        int doctorId = requireAssignedDoctor(request, user, workflow, encounterId);

        service.createLabOrder(
                encounterId,
                doctorId,
                ControllerSupport.requiredParameter(request, "testCode"),
                ControllerSupport.requiredParameter(request, "testName"),
                request.getParameter("priority"),
                request.getParameter("clinicalNote"),
                user.getUserId());
        return "labs";
    }

    private String addLabResult(HttpServletRequest request, User user,
            ClinicWorkflowService service) {
        requireReception(user);
        service.resultLab(
                positiveParameter(request, "labOrderId"),
                ControllerSupport.requiredParameter(request, "resultValue"),
                request.getParameter("resultUnit"),
                request.getParameter("referenceRange"),
                request.getParameter("resultFlag"),
                user.getUserId());
        return "labs";
    }

    private int requireAssignedDoctor(HttpServletRequest request, User user,
            ClinicWorkflowDAO workflow, int encounterId) {
        Integer doctorId = doctorIdForSession(request, user, workflow);
        if (doctorId == null || !workflow.doctorOwnsEncounter(doctorId, encounterId)) {
            throw forbiddenEncounter();
        }
        return doctorId;
    }

    private int requireAssignedPatient(HttpServletRequest request, User user,
            ClinicWorkflowDAO workflow) {
        int patientId = positiveParameter(request, "patientId");
        Integer doctorId = doctorIdForSession(request, user, workflow);
        if (doctorId == null || !workflow.doctorHasPatient(doctorId, patientId)) {
            throw new SecurityException("Bệnh nhân không thuộc phạm vi phụ trách của bác sĩ");
        }
        return patientId;
    }

    private Integer doctorIdForSession(HttpServletRequest request, User user,
            ClinicWorkflowDAO workflow) {
        HttpSession session = request.getSession();
        Integer doctorId = (Integer) session.getAttribute(ControllerSupport.DOCTOR_ID_SESSION_KEY);
        if (doctorId == null) {
            doctorId = workflow.doctorIdForUser(user.getUserId());
            if (doctorId != null) {
                session.setAttribute(ControllerSupport.DOCTOR_ID_SESSION_KEY, doctorId);
            }
        }
        return doctorId;
    }

    String normalizeView(String requestedView, String role) {
        String view = requestedView != null && SUPPORTED_VIEWS.contains(requestedView)
                ? requestedView : "encounters";
        if ("DOCTOR".equals(role) && "appointments".equals(view)) return "encounters";
        if (!"DOCTOR".equals(role) && "clinical".equals(view)) return "encounters";
        return view;
    }

    private void redirectToWorkflow(HttpServletRequest request, HttpServletResponse response,
            String view) throws IOException {
        String patientId = request.getParameter("patientId");
        int parsedPatientId = ControllerSupport.positiveIdOrZero(patientId);
        String patientQuery = parsedPatientId == 0 ? "" : "&patientId=" + parsedPatientId;
        response.sendRedirect(request.getContextPath()
                + "/ClinicWorkflow?view=" + view + patientQuery);
    }

    private void moveFlashMessageToRequest(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object flash = session.getAttribute("workflowFlash");
        request.setAttribute("workflowFlash", flash);
        session.removeAttribute("workflowFlash");
    }

    private void requireReception(User user) {
        if (!RECEPTION_ROLES.contains(user.getRole())) throw forbiddenAction();
    }

    private void requireDoctor(User user) {
        if (!"DOCTOR".equals(user.getRole())) throw forbiddenAction();
    }

    private SecurityException forbiddenAction() {
        return new SecurityException("Thao tác không được phép");
    }

    private SecurityException forbiddenEncounter() {
        return new SecurityException("Không được phân công lượt khám này");
    }

    private int positiveParameter(HttpServletRequest request, String name) {
        return ControllerSupport.positiveId(
                ControllerSupport.requiredParameter(request, name),
                ControllerSupport.parameterLabel(name));
    }

    private java.time.LocalDateTime appointmentDateTime(HttpServletRequest request) {
        String combinedValue = request.getParameter("appointmentAt");
        if (combinedValue != null && !combinedValue.isBlank()) {
            return ControllerSupport.appointmentDateTime(combinedValue);
        }
        return ControllerSupport.appointmentDateTime(
                request.getParameter("appointmentDate"),
                request.getParameter("appointmentTime"));
    }
}
