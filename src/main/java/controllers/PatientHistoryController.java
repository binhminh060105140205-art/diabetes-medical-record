package controllers;

import dal.*;
import models.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/PatientHistory")
public class PatientHistoryController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) { response.sendRedirect(request.getContextPath() + "/Login"); return; }
        if (!java.util.Set.of("ADMIN","STAFF","DOCTOR","PATIENT").contains(user.getRole())) {
            response.sendError(403); return;
        }

        PatientDAO patDAO = new PatientDAO();
        int patientId = -1;

        String pidParam = request.getParameter("patientId");
        String uidParam = request.getParameter("userId");

        if ("PATIENT".equals(user.getRole())) {
            Patient me = patDAO.getByUserId(user.getUserId());
            if (me == null) { response.sendRedirect(request.getContextPath() + "/PatientDashboard"); return; }
            patientId = me.getPatientId();
        } else if (pidParam != null && !pidParam.trim().isEmpty()) {
            try { patientId = Integer.parseInt(pidParam.trim()); }
            catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/PatientList"); return;
            }
        } else if (uidParam != null && !uidParam.trim().isEmpty()) {
            try { 
                int userId = Integer.parseInt(uidParam.trim()); 
                Patient p = patDAO.getByUserId(userId);
                if (p != null) {
                    patientId = p.getPatientId();
                }
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/AdminDashboard"); return;
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/PatientList"); return;
        }

        Patient patient = patDAO.getById(patientId);
        if (patient == null) {
            if (uidParam != null) {
                response.sendRedirect(request.getContextPath() + "/AdminDashboard");
            } else {
                response.sendRedirect(request.getContextPath() + "/PatientList");
            }
            return;
        }
        if ("DOCTOR".equals(user.getRole())) {
            ClinicWorkflowDAO workflowDAO = new ClinicWorkflowDAO();
            Integer doctorId = workflowDAO.doctorIdForUser(user.getUserId());
            if (doctorId == null || !workflowDAO.doctorHasPatient(doctorId, patientId)) {
                response.sendError(403); return;
            }
        }

        MedicalRecordDAO recDAO  = new MedicalRecordDAO();
        var records = recDAO.getByPatient(patientId);

        request.setAttribute("patient", patient);
        request.setAttribute("records", records);
        request.getRequestDispatcher("views/PatientHistory.jsp").forward(request, response);
    }
}
