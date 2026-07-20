package controllers;

import dal.PatientDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import models.Patient;
import models.User;

@WebServlet("/PatientDashboard")
public class PatientDashboardController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "PATIENT")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }

        PatientDAO.PatientDashboardData data = new PatientDAO()
                .loadPatientDashboard(user.getUserId());
        Patient patient = data.patient();
        if (patient == null) {
            request.setAttribute("msg", "Chưa có hồ sơ bệnh nhân. Vui lòng liên hệ nhân viên tiếp nhận.");
            request.getRequestDispatcher("views/PatientToday.jsp").forward(request, response);
            return;
        }
        request.getSession().setAttribute(
                ControllerSupport.PATIENT_ID_SESSION_KEY, patient.getPatientId());

        request.setAttribute("patient", patient);
        request.setAttribute("diabetesProfile", data.diabetesProfile());
        request.setAttribute("latestRecord", data.latestRecord());
        request.setAttribute("todayLog", data.todayLog());

        request.getRequestDispatcher("views/PatientToday.jsp").forward(request, response);
    }
}
