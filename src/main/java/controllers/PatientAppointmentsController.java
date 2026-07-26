package controllers;

import dal.ClinicWorkflowDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import models.User;
import vn.diabetes.service.ClinicWorkflowService;
import vn.diabetes.validation.AppointmentRules;

@WebServlet("/PatientAppointments")
public class PatientAppointmentsController extends HttpServlet {
    private static final Set<String> PATIENT_REASONS = Set.of(
            "Tái khám tiểu đường định kỳ",
            "Kiểm tra đường huyết và HbA1c",
            "Tư vấn thuốc hoặc insulin",
            "Có triệu chứng bất thường",
            "Khám lần đầu");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "PATIENT")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }

        ClinicWorkflowDAO.PatientAppointmentPageData data =
                new ClinicWorkflowDAO().loadPatientAppointmentPage(user.getUserId());
        if (data.patientId() == null) {
            response.sendError(HttpServletResponse.SC_CONFLICT,
                    "Tài khoản chưa liên kết hồ sơ bệnh nhân");
            return;
        }

        request.getSession().setAttribute(ControllerSupport.PATIENT_ID_SESSION_KEY, data.patientId());
        request.setAttribute("appointments", data.appointments());
        request.setAttribute("appointmentDates", ControllerSupport.appointmentDateOptions(true));
        LocalDate today = AppointmentRules.nowInVietnam().toLocalDate();
        request.setAttribute("minAppointmentDate", today);
        request.setAttribute("maxAppointmentDate", today.plusDays(AppointmentRules.MAX_ADVANCE_DAYS));
        request.getRequestDispatcher("views/PatientAppointmentsSimple.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "PATIENT")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }

        ClinicWorkflowDAO workflow = new ClinicWorkflowDAO();
        ClinicWorkflowService service = new ClinicWorkflowService(workflow);
        try {
            if ("cancel".equals(request.getParameter("action"))) {
                cancelAppointment(request, service, user);
                flash(request, "Đã hủy lịch hẹn.", "success");
            } else {
                createAppointmentRequest(request, workflow, service, user);
                flash(request,
                        "Đã gửi yêu cầu. Nhân viên sẽ xác nhận bác sĩ và giờ khám cụ thể.", "success");
            }
        } catch (IllegalArgumentException error) {
            flash(request, "Không thể đặt lịch: " + error.getMessage(), "danger");
        } catch (IllegalStateException error) {
            flash(request,
                    "Không thể xử lý lịch hẹn lúc này. Vui lòng tải lại trang và thử lại.", "danger");
        }
        redirectToPage(request, response);
    }

    // Nút "Hủy yêu cầu / lịch" chỉ được phép hủy lịch của chính bệnh nhân đang đăng nhập.
    private void cancelAppointment(HttpServletRequest request, ClinicWorkflowService service,
            User user) {
        int appointmentId = ControllerSupport.positiveId(
                request.getParameter("appointmentId"), "Lịch hẹn");
        service.cancelOwnAppointment(appointmentId, user.getUserId(), user.getUserId());
    }

    // Nút "Gửi yêu cầu" tạo trạng thái REQUESTED; nhân viên mới là người phân công bác sĩ và giờ cụ thể.
    private void createAppointmentRequest(HttpServletRequest request, ClinicWorkflowDAO workflow,
            ClinicWorkflowService service, User user) {
        Integer patientId = workflow.patientIdForUser(user.getUserId());
        if (patientId == null) {
            throw new IllegalArgumentException("Tài khoản chưa liên kết hồ sơ bệnh nhân.");
        }
        String reason = ControllerSupport.clean(request.getParameter("reason"));
        if (!PATIENT_REASONS.contains(reason)) {
            throw new IllegalArgumentException("Vui lòng chọn một lý do khám trong danh sách.");
        }
        service.createAppointmentRequest(patientId,
                parseDate(request.getParameter("preferredDate")),
                ControllerSupport.clean(request.getParameter("preferredPeriod")),
                reason, null, user.getUserId());
    }

    private void flash(HttpServletRequest request, String message, String type) {
        ControllerSupport.flash(request, "appointmentFlash", message);
        ControllerSupport.flash(request, "appointmentFlashType", type);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(ControllerSupport.clean(value));
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Ngày khám không hợp lệ.");
        }
    }

    private void redirectToPage(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/PatientAppointments");
    }
}
