package controllers;

import dal.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import models.*;

/** Patient self-monitoring. A future local AI model can consume this data separately. */
@WebServlet(urlPatterns = {"/PatientHealth", "/PatientAI"})
public class PatientHealthController extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/PatientDashboard#daily-health");
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8"); resp.setContentType("application/json;charset=UTF-8");
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || !"PATIENT".equals(user.getRole())) { resp.sendError(401); return; }
        Patient patient = new PatientDAO().getByUserId(user.getUserId());
        if (patient == null) { resp.sendError(404); return; }
        String action = req.getParameter("action");
        if ("saveLog".equals(action)) {
            PatientDailyLog log = new PatientDailyLog(); log.setPatientId(patient.getPatientId());
            log.setBloodGlucose(decimal(req,"bloodGlucose")); log.setSystolicBp(integer(req,"systolicBp"));
            log.setDiastolicBp(integer(req,"diastolicBp")); log.setWeight(decimal(req,"weight"));
            log.setHeartRate(integer(req,"heartRate")); log.setSpo2(decimal(req,"spo2"));
            log.setMealType(text(req,"mealType")); log.setSymptoms(text(req,"symptoms")); log.setNote(text(req,"note"));
            try { validate(log); } catch(IllegalArgumentException e) { resp.setStatus(400); resp.getWriter().print("{\"success\":false,\"error\":\""+json(e.getMessage())+"\"}"); return; }
            PatientDailyLog saved = new PatientDailyLogDAO().save(log);
            List<HealthAlert> alerts = AlertEngine.analyzeLog(saved); HealthAlertDAO dao = new HealthAlertDAO();
            for (HealthAlert alert : alerts) dao.save(alert);
            resp.getWriter().printf("{\"success\":true,\"message\":\"Đã lưu chỉ số hôm nay\",\"alerts_created\":%d,\"has_alerts\":%s}", alerts.size(), !alerts.isEmpty()); return;
        }
        if ("acknowledgeAlert".equals(action)) {
            Integer id = integer(req,"alertId");
            if (id == null || !new HealthAlertDAO().acknowledgeForPatient(id, patient.getPatientId())) { resp.sendError(404); return; }
            resp.getWriter().print("{\"success\":true}"); return;
        }
        resp.sendError(400, "Hành động không hợp lệ");
    }
    private String text(HttpServletRequest req,String name){String v=req.getParameter(name);return v==null||v.isBlank()?null:v.trim();}
    private Double decimal(HttpServletRequest req,String name)throws ServletException{String v=text(req,name);try{return v==null?null:Double.valueOf(v);}catch(NumberFormatException e){throw new ServletException(name+" không hợp lệ",e);}}
    private Integer integer(HttpServletRequest req,String name)throws ServletException{String v=text(req,name);try{return v==null?null:Integer.valueOf(v);}catch(NumberFormatException e){throw new ServletException(name+" không hợp lệ",e);}}
    private void validate(PatientDailyLog log) {
        range(log.getBloodGlucose(),20,600,"Đường huyết"); range(log.getWeight(),20,300,"Cân nặng"); range(log.getSpo2(),50,100,"SpO2");
        range(log.getSystolicBp(),60,260,"Huyết áp tâm thu"); range(log.getDiastolicBp(),30,180,"Huyết áp tâm trương"); range(log.getHeartRate(),30,220,"Nhịp tim");
        if(log.getSystolicBp()!=null&&log.getDiastolicBp()!=null&&log.getSystolicBp()<=log.getDiastolicBp()) throw new IllegalArgumentException("Huyết áp tâm thu phải lớn hơn tâm trương.");
        if(log.getSymptoms()!=null&&log.getSymptoms().length()>500) throw new IllegalArgumentException("Triệu chứng tối đa 500 ký tự.");
        if(log.getNote()!=null&&log.getNote().length()>1000) throw new IllegalArgumentException("Ghi chú tối đa 1000 ký tự.");
    }
    private void range(Number value,double min,double max,String label){if(value!=null&&(value.doubleValue()<min||value.doubleValue()>max))throw new IllegalArgumentException(label+" ngoài khoảng hợp lệ ("+min+"–"+max+").");}
    private String json(String value){return value.replace("\\","\\\\").replace("\"","\\\"");}
}
