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
        User user = ControllerSupport.currentUser(request);
        if (user == null) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }
        if (!ControllerSupport.hasRole(user, "ADMIN", "STAFF", "DOCTOR", "PATIENT")) {
            response.sendError(403); return;
        }

        Integer patientId = null;
        Integer patientUserId = null;

        String pidParam = request.getParameter("patientId");
        String uidParam = request.getParameter("userId");

        if ("PATIENT".equals(user.getRole())) {
            patientUserId = user.getUserId();
        } else if (pidParam != null && !pidParam.trim().isEmpty()) {
            patientId = ControllerSupport.positiveIdOrZero(pidParam.trim());
            if (patientId == 0) {
                response.sendRedirect(request.getContextPath() + "/PatientList"); return;
            }
        } else if (uidParam != null && !uidParam.trim().isEmpty()) {
            patientUserId = ControllerSupport.positiveIdOrZero(uidParam.trim());
            if (patientUserId == 0) {
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
