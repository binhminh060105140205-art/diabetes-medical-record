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

        Integer patientId = null;
        Integer patientUserId = null;

        String pidParam = request.getParameter("patientId");
        String uidParam = request.getParameter("userId");

        if ("PATIENT".equals(user.getRole())) {
            patientUserId = user.getUserId();
        } else if (pidParam != null && !pidParam.trim().isEmpty()) {
            try { patientId = Integer.parseInt(pidParam.trim()); }
            catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/PatientList"); return;
            }
        } else if (uidParam != null && !uidParam.trim().isEmpty()) {
            try { 
                int userId = Integer.parseInt(uidParam.trim()); 
                patientUserId = userId;
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/AdminDashboard"); return;
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/PatientList"); return;
        }

        Integer doctorUserId = "DOCTOR".equals(user.getRole()) ? user.getUserId() : null;
        MedicalRecordDAO recDAO = new MedicalRecordDAO();
        var history = recDAO.loadPatientHistory(patientId, patientUserId, doctorUserId);
        Patient patient = history.patient();
        if (patient == null) {
            if (uidParam != null) {
                response.sendRedirect(request.getContextPath() + "/AdminDashboard");
            } else {
                response.sendRedirect(request.getContextPath() + "/PatientList");
            }
            return;
        }
        if (!history.authorized()) { response.sendError(403); return; }

        request.setAttribute("patient", patient);
        request.setAttribute("diabetesProfile", history.diabetesProfile());
        request.setAttribute("records", history.records());
        request.getRequestDispatcher("views/PatientTimeline.jsp").forward(request, response);
    }
}
