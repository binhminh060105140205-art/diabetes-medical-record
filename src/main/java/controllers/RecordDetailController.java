package controllers;

import dal.*;
import models.*;
import viewmodels.RecordDetail;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/RecordDetail")
public class RecordDetailController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) { response.sendRedirect("Login"); return; }

        int recordId = Integer.parseInt(request.getParameter("id"));

        MedicalRecordDAO recDAO  = new MedicalRecordDAO();
        PatientDAO       patDAO  = new PatientDAO();
        DoctorDAO        docDAO  = new DoctorDAO();
        HealthIndicatorDAO hiDAO = new HealthIndicatorDAO();

        MedicalRecord rec = recDAO.getById(recordId);
        if (rec == null) { response.sendRedirect("PatientList"); return; }

        // Access control: patient can only see their own record
        if ("PATIENT".equals(user.getRole())) {
            Patient me = patDAO.getByUserId(user.getUserId());
            if (me == null || me.getPatientId() != rec.getPatientId()) {
                response.sendRedirect("PatientDashboard"); return;
            }
        }
        if ("DOCTOR".equals(user.getRole())) {
            Integer doctorId = new ClinicWorkflowDAO().doctorIdForUser(user.getUserId());
            if (doctorId == null || doctorId != rec.getDoctorId()) { response.sendError(403); return; }
        }
        if (!java.util.Set.of("ADMIN","STAFF","DOCTOR","PATIENT").contains(user.getRole())) {
            response.sendError(403); return;
        }

        RecordDetail detail = new RecordDetail();
        detail.setRecord(rec);
        detail.setPatient(patDAO.getById(rec.getPatientId()));
        detail.setDoctor(docDAO.getById(rec.getDoctorId()));
        detail.setIndicator(hiDAO.getByRecordId(recordId));
        request.setAttribute("prescriptionItems", recDAO.getPrescriptionItems(recordId));

        request.setAttribute("detail", detail);
        request.getRequestDispatcher("views/RecordDetail.jsp").forward(request, response);
    }
}
