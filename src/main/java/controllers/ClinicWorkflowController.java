package controllers;

import dal.*; import models.*;
import jakarta.servlet.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*;
import java.io.IOException; import java.time.LocalDateTime; import java.sql.Date; import java.util.Set;
import vn.diabetes.service.ClinicWorkflowService;

@WebServlet("/ClinicWorkflow")
public class ClinicWorkflowController extends HttpServlet {
  private static final Set<String> CLINICAL=Set.of("ADMIN","STAFF","DOCTOR");
  protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
    User u=user(req); if(u==null||!CLINICAL.contains(u.getRole())){resp.sendRedirect("Login");return;}
    ClinicWorkflowDAO dao=new ClinicWorkflowDAO(); String view=req.getParameter("view");
    if(view==null || !Set.of("appointments","encounters","clinical","labs").contains(view)) view="encounters";
    if ("DOCTOR".equals(u.getRole()) && "appointments".equals(view)) view="encounters";
    if (!"DOCTOR".equals(u.getRole()) && "clinical".equals(view)) view="encounters";
    req.setAttribute("view",view);
    if ("labs".equals(view)) req.setAttribute("labTests",ClinicWorkflowService.labTests());
    if ("clinical".equals(view)) req.setAttribute("patients",new PatientDAO().getAll());
    if ("appointments".equals(view)) {
      var page=dao.loadAppointmentOperationsPage();
      req.setAttribute("patients",page.patients());
      req.setAttribute("doctors",page.doctors());
      req.setAttribute("appointments",page.appointments());
    }
    if ("DOCTOR".equals(u.getRole())) {
      Integer doctorId=(Integer)req.getSession().getAttribute("clinicDoctorId");
      if(doctorId==null){doctorId=dao.doctorIdForUser(u.getUserId());if(doctorId!=null)req.getSession().setAttribute("clinicDoctorId",doctorId);}
      if(doctorId==null){resp.sendError(403,"Tài khoản chưa có hồ sơ bác sĩ");return;}
      if("appointments".equals(view)) req.setAttribute("appointments",dao.appointmentsForDoctor(doctorId));
      if(Set.of("encounters","labs").contains(view)) req.setAttribute("encounters",dao.encountersForDoctor(doctorId));
      if("labs".equals(view)) req.setAttribute("labOrders",dao.labOrdersForDoctor(doctorId));
    } else {
      if("encounters".equals(view)) req.setAttribute("encounters",dao.encounters());
      if("labs".equals(view)) req.setAttribute("labOrders",dao.labOrders());
    }
    String p=req.getParameter("patientId"); if(p!=null&&!p.isBlank()){int id=positiveOrZero(p);if(id==0){resp.sendError(400,"Mã bệnh nhân không hợp lệ");return;}Patient selected=new PatientDAO().getById(id);if(selected==null){resp.sendError(404,"Không tìm thấy bệnh nhân");return;}req.setAttribute("selectedPatient",selected);req.setAttribute("allergies",dao.allergies(id));req.setAttribute("histories",dao.histories(id));}
    Object flash=req.getSession().getAttribute("workflowFlash");req.setAttribute("workflowFlash",flash);req.getSession().removeAttribute("workflowFlash");
    req.getRequestDispatcher("views/ClinicWorkflow.jsp").forward(req,resp);
  }
  protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException,ServletException{
    req.setCharacterEncoding("UTF-8");User u=user(req);if(u==null||!CLINICAL.contains(u.getRole())){resp.sendError(403);return;}
    ClinicWorkflowDAO d=new ClinicWorkflowDAO();ClinicWorkflowService service=new ClinicWorkflowService(d);String a=req.getParameter("action"),view="encounters";
    try{
      if("createAppointment".equals(a)&&isReception(u)){service.createAppointment(i(req,"patientId"),i(req,"doctorId"),LocalDateTime.parse(required(req,"appointmentAt")),required(req,"reason"),req.getParameter("note"),u.getUserId());view="appointments";}
      else if("assignAppointmentRequest".equals(a)&&isReception(u)){service.assignAppointmentRequest(i(req,"appointmentId"),i(req,"doctorId"),LocalDateTime.parse(required(req,"appointmentAt")),req.getParameter("note"),u.getUserId());view="appointments";}
      else if("rescheduleAppointment".equals(a)&&isReception(u)){service.rescheduleAppointment(i(req,"appointmentId"),LocalDateTime.parse(required(req,"appointmentAt")),req.getParameter("note"),u.getUserId());view="appointments";}
      else if("appointmentStatus".equals(a)&&isReception(u)){service.setAppointmentStatus(i(req,"appointmentId"),required(req,"status"),u.getUserId());view="appointments";}
      else if("checkIn".equals(a)&&isReception(u)){service.checkIn(i(req,"appointmentId"),u.getUserId());view="encounters";}
      else if("status".equals(a)&&"DOCTOR".equals(u.getRole())){int eid=i(req,"encounterId");Integer did=d.doctorIdForUser(u.getUserId());if(did==null||!d.doctorOwnsEncounter(did,eid))throw new SecurityException("Không được phân công lượt khám này");service.setEncounterStatus(eid,req.getParameter("status"),u.getUserId());}
      else if("allergy".equals(a)&&"DOCTOR".equals(u.getRole())){service.addAllergy(i(req,"patientId"),required(req,"allergen"),req.getParameter("reaction"),req.getParameter("severity"),u.getUserId());view="clinical";}
      else if("history".equals(a)&&"DOCTOR".equals(u.getRole())){String ds=req.getParameter("diagnosedDate");service.addHistory(i(req,"patientId"),req.getParameter("historyType"),required(req,"conditionName"),ds==null||ds.isBlank()?null:Date.valueOf(ds),req.getParameter("historyStatus"),req.getParameter("historyNote"),u.getUserId());view="clinical";}
      else if("labOrder".equals(a)&&"DOCTOR".equals(u.getRole())){Integer did=d.doctorIdForUser(u.getUserId());int eid=i(req,"encounterId");if(did==null||!d.doctorOwnsEncounter(did,eid))throw new SecurityException("Không được phân công lượt khám này");service.createLabOrder(eid,did,required(req,"testCode"),required(req,"testName"),req.getParameter("priority"),req.getParameter("clinicalNote"),u.getUserId());view="labs";}
      else if("labResult".equals(a)&&isReception(u)){service.resultLab(i(req,"labOrderId"),required(req,"resultValue"),req.getParameter("resultUnit"),req.getParameter("referenceRange"),req.getParameter("resultFlag"),u.getUserId());view="labs";}
      else throw new SecurityException("Thao tác không được phép");
      req.getSession().setAttribute("workflowFlash","Đã cập nhật thành công");
    }catch(IllegalArgumentException|SecurityException e){req.getSession().setAttribute("workflowFlash","Không thể cập nhật: "+e.getMessage());}
    catch(Exception e){throw new ServletException("Lỗi cập nhật quy trình khám",e);}
    String pid=req.getParameter("patientId");resp.sendRedirect(req.getContextPath()+"/ClinicWorkflow?view="+view+(pid!=null?"&patientId="+pid:""));
  }
  private User user(HttpServletRequest r){HttpSession s=r.getSession(false);return s==null?null:(User)s.getAttribute("user");}
  private boolean isReception(User u){return Set.of("ADMIN","STAFF").contains(u.getRole());}
  private int i(HttpServletRequest r,String n){return positive(required(r,n),n);}
  private int positive(String value,String name){try{int parsed=Integer.parseInt(value);if(parsed>0)return parsed;}catch(NumberFormatException ignored){}throw new IllegalArgumentException(name+" không hợp lệ");}
  private int positiveOrZero(String value){try{int parsed=Integer.parseInt(value);return parsed>0?parsed:0;}catch(NumberFormatException ignored){return 0;}}
  private String required(HttpServletRequest r,String n){String v=r.getParameter(n);if(v==null||v.isBlank())throw new IllegalArgumentException(n+" là bắt buộc");return v.trim();}
}
