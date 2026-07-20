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
        request.getSession().setAttribute(
                ControllerSupport.PATIENT_ID_SESSION_KEY, data.patientId());
        request.setAttribute("appointments", data.appointments());
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
                service.cancelOwnAppointment(
                        ControllerSupport.positiveId(request.getParameter("appointmentId"), "Lịch hẹn"),
                        user.getUserId(), user.getUserId());
                ControllerSupport.flash(request, "appointmentFlash", "Đã hủy lịch hẹn.");
                redirectToPage(request, response);
                return;
            }

            Integer patientId = workflow.patientIdForUser(user.getUserId());
            if (patientId == null) {
                throw new IllegalArgumentException("Tài khoản chưa liên kết hồ sơ bệnh nhân.");
            }
            String reason = ControllerSupport.clean(request.getParameter("reason"));
            if (!PATIENT_REASONS.contains(reason)) {
                throw new IllegalArgumentException("Vui lòng chọn một lý do khám trong danh sách.");
            }

            service.createAppointmentRequest(
                    patientId,
                    parseDate(request.getParameter("preferredDate")),
                    ControllerSupport.clean(request.getParameter("preferredPeriod")),
                    reason, null, user.getUserId());
            ControllerSupport.flash(request, "appointmentFlash",
                    "Đã gửi yêu cầu. Nhân viên sẽ xác nhận bác sĩ và giờ khám cụ thể.");
        } catch (IllegalArgumentException error) {
            ControllerSupport.flash(request, "appointmentFlash",
                    "Không thể đặt lịch: " + error.getMessage());
        } catch (IllegalStateException error) {
            throw new ServletException("Không thể xử lý lịch hẹn", error);
        }
        redirectToPage(request, response);
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
