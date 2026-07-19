package controllers;

import dal.*;
import models.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/DoctorDashboard")
public class DoctorDashboardController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"DOCTOR".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }

        MedicalRecordDAO recDAO = new MedicalRecordDAO();
        MedicalRecordDAO.DoctorDashboardData data =
                recDAO.loadDoctorDashboardForUser(user.getUserId());
        Doctor doctor = data.doctor();
        request.setAttribute("doctor",        doctor);

        if (doctor != null) {
            session.setAttribute("clinicDoctorId", doctor.getDoctorId());
            request.setAttribute("totalPatients", data.totalPatients());
            request.setAttribute("totalMyRecords", data.totalRecords());
            request.setAttribute("myRecords", data.recent());
            request.setAttribute("pendingRecords", data.pending());
            request.setAttribute("totalPending", data.pendingCount());
        }

        request.getRequestDispatcher("views/DoctorDashboard.jsp").forward(request, response);
    }
}
