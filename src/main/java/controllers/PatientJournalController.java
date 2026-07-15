package controllers;

import dal.PatientDAO;
import dal.PatientDailyLogDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import models.Patient;
import models.PatientDailyLog;
import models.User;
import java.io.IOException;
import java.util.List;

@WebServlet("/PatientJournal")
public class PatientJournalController extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws ServletException,IOException {
        HttpSession session=req.getSession(false); User user=session==null?null:(User)session.getAttribute("user");
        if(user==null||!"PATIENT".equals(user.getRole())){resp.sendRedirect(req.getContextPath()+"/Login");return;}
        Patient patient=new PatientDAO().getByUserId(user.getUserId());
        if(patient==null){resp.sendError(409,"Tài khoản chưa liên kết hồ sơ bệnh nhân");return;}
        List<PatientDailyLog> logs=new PatientDailyLogDAO().getRecent(patient.getPatientId(),30);
        req.setAttribute("patient",patient); req.setAttribute("logs",logs);
        req.setAttribute("avgGlucose",averageGlucose(logs)); req.setAttribute("avgSystolic",averageSystolic(logs));
        req.getRequestDispatcher("views/PatientJournal.jsp").forward(req,resp);
    }
    private String averageGlucose(List<PatientDailyLog> logs){double v=logs.stream().filter(x->x.getBloodGlucose()!=null).mapToDouble(PatientDailyLog::getBloodGlucose).average().orElse(0);return v==0?null:String.format("%.1f",v);}
    private String averageSystolic(List<PatientDailyLog> logs){double v=logs.stream().filter(x->x.getSystolicBp()!=null).mapToInt(PatientDailyLog::getSystolicBp).average().orElse(0);return v==0?null:String.format("%.0f",v);}
}
