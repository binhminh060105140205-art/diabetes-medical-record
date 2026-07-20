package controllers;

import dal.PatientDailyLogDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import models.User;

@WebServlet("/DoctorPatientJournal")
public class DoctorPatientJournalController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "DOCTOR")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int patientId;
        try {
            patientId = ControllerSupport.positiveId(
                    request.getParameter("patientId"), "Mã bệnh nhân");
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, error.getMessage());
            return;
        }

        PatientDailyLogDAO.DoctorJournalData data = new PatientDailyLogDAO()
                .loadJournalForDoctor(user.getUserId(), patientId, 30);
        if (data.patient() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!data.authorized()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bệnh nhân không thuộc lượt khám của bác sĩ");
            return;
        }

        request.setAttribute("patient", data.patient());
        request.setAttribute("logs", data.logs());
        request.getRequestDispatcher("views/DoctorPatientJournal.jsp")
                .forward(request, response);
    }
}
