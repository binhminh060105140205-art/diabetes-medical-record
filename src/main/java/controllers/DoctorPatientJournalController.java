package controllers;

import dal.ClinicWorkflowDAO;
import dal.PatientDAO;
import dal.PatientDailyLogDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import models.Patient;
import models.User;
import java.io.IOException;

@WebServlet("/DoctorPatientJournal")
public class DoctorPatientJournalController extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws ServletException,IOException {
        HttpSession session=req.getSession(false); User user=session==null?null:(User)session.getAttribute("user");
        if(user==null||!"DOCTOR".equals(user.getRole())){resp.sendError(403);return;}
        int patientId; try{patientId=Integer.parseInt(req.getParameter("patientId"));}catch(Exception e){resp.sendError(400,"Mã bệnh nhân không hợp lệ");return;}
        ClinicWorkflowDAO workflow=new ClinicWorkflowDAO(); Integer doctorId=workflow.doctorIdForUser(user.getUserId());
        if(doctorId==null||!workflow.doctorHasPatient(doctorId,patientId)){resp.sendError(403,"Bệnh nhân không thuộc lượt khám của bác sĩ");return;}
        Patient patient=new PatientDAO().getById(patientId); if(patient==null){resp.sendError(404);return;}
        req.setAttribute("patient",patient); req.setAttribute("logs",new PatientDailyLogDAO().getRecent(patientId,30));
        req.getRequestDispatcher("views/DoctorPatientJournal.jsp").forward(req,resp);
    }
}
