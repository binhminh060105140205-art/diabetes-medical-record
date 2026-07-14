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

        DoctorDAO        doctorDAO = new DoctorDAO();
        MedicalRecordDAO recDAO    = new MedicalRecordDAO();
        PatientDAO       patDAO    = new PatientDAO();

        Doctor doctor = doctorDAO.getByUserId(user.getUserId());
        request.setAttribute("doctor",        doctor);
        request.setAttribute("totalPatients", patDAO.countAll());

        if (doctor != null) {
            List<MedicalRecord> allRecords = recDAO.getByDoctor(doctor.getDoctorId());
            request.setAttribute("totalMyRecords", allRecords.size());

            // Bệnh án gần đây (top 5)
            int end = Math.min(5, allRecords.size());
            request.setAttribute("myRecords", allRecords.subList(0, end));

            // Bệnh nhân CHỜ KHÁM (DRAFT)
            List<MedicalRecord> pending = recDAO.getPendingByDoctor(doctor.getDoctorId());
            request.setAttribute("pendingRecords", pending);
            request.setAttribute("totalPending",   pending.size());
        }

        request.getRequestDispatcher("views/DoctorDashboard.jsp").forward(request, response);
    }
}
