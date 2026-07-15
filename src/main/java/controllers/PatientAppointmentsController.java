package controllers;

import dal.ClinicWorkflowDAO;
import dal.DoctorDAO;
import dal.PatientDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import models.Patient;
import models.User;
import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet("/PatientAppointments")
public class PatientAppointmentsController extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = patientUser(req);
        if (user == null) { resp.sendRedirect(req.getContextPath() + "/Login"); return; }
        Patient patient = new PatientDAO().getByUserId(user.getUserId());
        if (patient == null) { resp.sendError(409, "Tài khoản chưa liên kết hồ sơ bệnh nhân"); return; }
        req.setAttribute("doctors", new DoctorDAO().getAll());
        req.setAttribute("appointments", new ClinicWorkflowDAO().appointmentsForPatient(patient.getPatientId()));
        req.getRequestDispatcher("views/PatientAppointments.jsp").forward(req, resp);
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8"); User user = patientUser(req);
        if (user == null) { resp.sendRedirect(req.getContextPath() + "/Login"); return; }
        Patient patient = new PatientDAO().getByUserId(user.getUserId());
        try {
            int doctorId = positive(req.getParameter("doctorId"));
            LocalDateTime at = LocalDateTime.parse(req.getParameter("appointmentAt"));
            String reason = clean(req.getParameter("reason")); String note = clean(req.getParameter("note"));
            if (reason.length() < 5 || reason.length() > 255) throw new IllegalArgumentException("Lý do khám phải có từ 5 đến 255 ký tự.");
            if (note.length() > 500) throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự.");
            new ClinicWorkflowDAO().createAppointment(patient.getPatientId(), doctorId, at, reason, note, user.getUserId());
            req.getSession().setAttribute("appointmentFlash", "Đặt lịch thành công. Vui lòng đến trước giờ hẹn 15 phút.");
        } catch (IllegalArgumentException ex) {
            req.getSession().setAttribute("appointmentFlash", "Không thể đặt lịch: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ServletException("Không thể xử lý lịch hẹn", ex);
        }
        resp.sendRedirect(req.getContextPath() + "/PatientAppointments");
    }
    private User patientUser(HttpServletRequest req) { HttpSession s=req.getSession(false); User u=s==null?null:(User)s.getAttribute("user"); return u!=null&&"PATIENT".equals(u.getRole())?u:null; }
    private int positive(String value) { try { int id=Integer.parseInt(value); if(id>0)return id; } catch(Exception ignored){} throw new IllegalArgumentException("Bác sĩ không hợp lệ."); }
    private String clean(String value) { return value==null?"":value.trim(); }
}
