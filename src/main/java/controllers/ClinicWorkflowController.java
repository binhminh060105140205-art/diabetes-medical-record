package controllers;

import dal.*; import models.*;
import jakarta.servlet.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*;
import java.io.IOException; import java.time.LocalDateTime; import java.sql.Date; import java.util.Set;

@WebServlet("/ClinicWorkflow")
public class ClinicWorkflowController extends HttpServlet {
  private static final Set<String> CLINICAL=Set.of("ADMIN","STAFF","DOCTOR");
  protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
    User u=user(req); if(u==null||!CLINICAL.contains(u.getRole())){resp.sendRedirect("Login");return;}
    ClinicWorkflowDAO dao=new ClinicWorkflowDAO(); String view=req.getParameter("view"); if(view==null)view="encounters";
    req.setAttribute("view",view); req.setAttribute("patients",new PatientDAO().getAll()); req.setAttribute("doctors",new DoctorDAO().getAll());
    if ("DOCTOR".equals(u.getRole())) {
      Integer doctorId=dao.doctorIdForUser(u.getUserId());
      if(doctorId==null){resp.sendError(403,"Tài khoản chưa có hồ sơ bác sĩ");return;}
      req.setAttribute("appointments",dao.appointmentsForDoctor(doctorId));
      req.setAttribute("encounters",dao.encountersForDoctor(doctorId));
      req.setAttribute("labOrders",dao.labOrdersForDoctor(doctorId));
    } else {
      req.setAttribute("appointments",dao.appointments()); req.setAttribute("encounters",dao.encounters()); req.setAttribute("labOrders",dao.labOrders());
    }
    String p=req.getParameter("patientId"); if(p!=null&&!p.isBlank()){int id=Integer.parseInt(p);req.setAttribute("selectedPatient",new PatientDAO().getById(id));req.setAttribute("allergies",dao.allergies(id));req.setAttribute("histories",dao.histories(id));}
    Object flash=req.getSession().getAttribute("workflowFlash");req.setAttribute("workflowFlash",flash);req.getSession().removeAttribute("workflowFlash");
    req.getRequestDispatcher("views/ClinicWorkflow.jsp").forward(req,resp);
  }
  protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
    req.setCharacterEncoding("UTF-8");User u=user(req);if(u==null||!CLINICAL.contains(u.getRole())){resp.sendError(403);return;}
    ClinicWorkflowDAO d=new ClinicWorkflowDAO();String a=req.getParameter("action"),view="encounters";
    try{
      if("createAppointment".equals(a)&&Set.of("ADMIN","STAFF").contains(u.getRole())){d.createAppointment(i(req,"patientId"),i(req,"doctorId"),LocalDateTime.parse(req.getParameter("appointmentAt")),req.getParameter("reason"),req.getParameter("note"),u.getUserId());view="appointments";}
      else if("checkIn".equals(a)&&Set.of("ADMIN","STAFF").contains(u.getRole())){d.checkIn(i(req,"appointmentId"),u.getUserId());view="encounters";}
      else if("status".equals(a)){int eid=i(req,"encounterId");if("DOCTOR".equals(u.getRole())){Integer did=d.doctorIdForUser(u.getUserId());if(did==null||!d.doctorOwnsEncounter(did,eid))throw new SecurityException("Không được phân công lượt khám này");}d.setEncounterStatus(eid,req.getParameter("status"),u.getUserId());}
      else if("allergy".equals(a)){d.addAllergy(i(req,"patientId"),required(req,"allergen"),req.getParameter("reaction"),req.getParameter("severity"),u.getUserId());view="clinical";}
      else if("history".equals(a)){String ds=req.getParameter("diagnosedDate");d.addHistory(i(req,"patientId"),req.getParameter("historyType"),required(req,"conditionName"),ds==null||ds.isBlank()?null:Date.valueOf(ds),req.getParameter("historyStatus"),req.getParameter("historyNote"),u.getUserId());view="clinical";}
      else if("labOrder".equals(a)&&"DOCTOR".equals(u.getRole())){Integer did=d.doctorIdForUser(u.getUserId());int eid=i(req,"encounterId");if(did==null||!d.doctorOwnsEncounter(did,eid))throw new SecurityException("Không được phân công lượt khám này");d.createLabOrder(eid,did,required(req,"testCode"),required(req,"testName"),req.getParameter("priority"),req.getParameter("clinicalNote"),u.getUserId());view="labs";}
      else if("labResult".equals(a)&&Set.of("ADMIN","STAFF").contains(u.getRole())){d.resultLab(i(req,"labOrderId"),required(req,"resultValue"),req.getParameter("resultUnit"),req.getParameter("referenceRange"),req.getParameter("resultFlag"),u.getUserId());view="labs";}
      else throw new SecurityException("Thao tác không được phép");
      req.getSession().setAttribute("workflowFlash","Đã cập nhật thành công");
    }catch(Exception e){req.getSession().setAttribute("workflowFlash","Không thể cập nhật: "+e.getMessage());}
    String pid=req.getParameter("patientId");resp.sendRedirect(req.getContextPath()+"/ClinicWorkflow?view="+view+(pid!=null?"&patientId="+pid:""));
  }
  private User user(HttpServletRequest r){HttpSession s=r.getSession(false);return s==null?null:(User)s.getAttribute("user");}
  private int i(HttpServletRequest r,String n){return Integer.parseInt(r.getParameter(n));}
  private String required(HttpServletRequest r,String n){String v=r.getParameter(n);if(v==null||v.isBlank())throw new IllegalArgumentException(n+" là bắt buộc");return v.trim();}
}
