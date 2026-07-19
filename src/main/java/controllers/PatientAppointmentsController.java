package controllers;

import dal.ClinicWorkflowDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import models.User;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import vn.diabetes.service.ClinicWorkflowService;

@WebServlet("/PatientAppointments")
public class PatientAppointmentsController extends HttpServlet {
    private static final Set<String> PATIENT_REASONS = Set.of(
            "Tái khám tiểu đường định kỳ",
            "Kiểm tra đường huyết và HbA1c",
            "Tư vấn thuốc hoặc insulin",
            "Có triệu chứng bất thường",
            "Khám lần đầu");

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = patientUser(req);
        if (user == null) { resp.sendRedirect(req.getContextPath() + "/Login"); return; }
        ClinicWorkflowDAO.PatientAppointmentPageData data =
                new ClinicWorkflowDAO().loadPatientAppointmentPage(user.getUserId());
        if (data.patientId() == null) { resp.sendError(409, "Tài khoản chưa liên kết hồ sơ bệnh nhân"); return; }
        req.setAttribute("appointments", data.appointments());
        req.getRequestDispatcher("views/PatientAppointmentsSimple.jsp").forward(req, resp);
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8"); User user = patientUser(req);
        if (user == null) { resp.sendRedirect(req.getContextPath() + "/Login"); return; }
        try {
            ClinicWorkflowService service = new ClinicWorkflowService(new ClinicWorkflowDAO());
            if ("cancel".equals(req.getParameter("action"))) {
                service.cancelOwnAppointment(positive(req.getParameter("appointmentId")),
                        user.getUserId(), user.getUserId());
                req.getSession().setAttribute("appointmentFlash", "Đã hủy lịch hẹn.");
                resp.sendRedirect(req.getContextPath() + "/PatientAppointments");
                return;
            }
            ClinicWorkflowDAO.PatientAppointmentPageData page =
                    new ClinicWorkflowDAO().loadPatientAppointmentPage(user.getUserId());
            if (page.patientId() == null) throw new IllegalArgumentException("Tài khoản chưa liên kết hồ sơ bệnh nhân.");
            LocalDate preferredDate;
            try {
                preferredDate = LocalDate.parse(clean(req.getParameter("preferredDate")));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Ngày khám không hợp lệ.");
            }
            String preferredPeriod = clean(req.getParameter("preferredPeriod"));
            String reason = clean(req.getParameter("reason"));
            if (!PATIENT_REASONS.contains(reason)) {
                throw new IllegalArgumentException("Vui lòng chọn một lý do khám trong danh sách.");
            }
            service.createAppointmentRequest(page.patientId(), preferredDate, preferredPeriod,
                    reason, null, user.getUserId());
            req.getSession().setAttribute("appointmentFlash",
                    "Đã gửi yêu cầu. Nhân viên sẽ xác nhận bác sĩ và giờ khám cụ thể.");
        } catch (IllegalArgumentException ex) {
            req.getSession().setAttribute("appointmentFlash", "Không thể đặt lịch: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ServletException("Không thể xử lý lịch hẹn", ex);
        }
        resp.sendRedirect(req.getContextPath() + "/PatientAppointments");
    }
    private User patientUser(HttpServletRequest req) { HttpSession s=req.getSession(false); User u=s==null?null:(User)s.getAttribute("user"); return u!=null&&"PATIENT".equals(u.getRole())?u:null; }
    private int positive(String value) { try { int id=Integer.parseInt(value); if(id>0)return id; } catch(Exception ignored){} throw new IllegalArgumentException("Lịch hẹn không hợp lệ."); }
    private String clean(String value) { return value==null?"":value.trim(); }
}
