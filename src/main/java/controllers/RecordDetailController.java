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

        int recordId;
        try {
            recordId = Integer.parseInt(request.getParameter("id"));
            if (recordId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException | NullPointerException invalidId) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã bệnh án không hợp lệ");
            return;
        }

        MedicalRecordDAO recDAO = new MedicalRecordDAO();
        MedicalRecordDAO.MedicalRecordFormData data = recDAO.loadFormData(recordId);
        MedicalRecord rec = data.record();
        if (rec == null) { response.sendRedirect("PatientList"); return; }

        // Access control: patient can only see their own record
        if ("PATIENT".equals(user.getRole())) {
            if (data.patient() == null || data.patient().getUserId() != user.getUserId()) {
                response.sendRedirect("PatientDashboard"); return;
            }
        }
        if ("DOCTOR".equals(user.getRole())) {
            if (data.doctor() == null || data.doctor().getUserId() != user.getUserId()) {
                response.sendError(403); return;
            }
        }
        if (!java.util.Set.of("ADMIN","STAFF","DOCTOR","PATIENT").contains(user.getRole())) {
            response.sendError(403); return;
        }

        RecordDetail detail = new RecordDetail();
        detail.setRecord(rec);
        detail.setPatient(data.patient());
        detail.setDoctor(data.doctor());
        detail.setIndicator(data.indicator());
        request.setAttribute("prescriptionItems", data.prescriptionItems());

        request.setAttribute("detail", detail);
        if (data.patient() != null) {
            request.setAttribute("diabetesProfile", new DiabetesProfileDAO().getByPatientId(data.patient().getPatientId()));
        }
        request.getRequestDispatcher("views/RecordDetail.jsp").forward(request, response);
    }
}
