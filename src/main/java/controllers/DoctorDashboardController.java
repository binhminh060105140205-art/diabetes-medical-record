package controllers;

import dal.MedicalRecordDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import models.Doctor;
import models.User;

@WebServlet("/DoctorDashboard")
public class DoctorDashboardController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "DOCTOR")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }

        MedicalRecordDAO.DoctorDashboardData data =
                new MedicalRecordDAO().loadDoctorDashboardForUser(user.getUserId());
        Doctor doctor = data.doctor();
        request.setAttribute("doctor", doctor);

        if (doctor != null) {
            HttpSession session = request.getSession();
            session.setAttribute(ControllerSupport.DOCTOR_ID_SESSION_KEY, doctor.getDoctorId());
            request.setAttribute("totalPatients", data.totalPatients());
            request.setAttribute("totalMyRecords", data.totalRecords());
            request.setAttribute("myRecords", data.recent());
            request.setAttribute("pendingRecords", data.pending());
            request.setAttribute("totalPending", data.pendingCount());
        }

        request.getRequestDispatcher("views/DoctorDashboard.jsp").forward(request, response);
    }
}
