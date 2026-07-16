package controllers;

import dal.PatientDAO;
import dal.PatientRegistrationDAO;
import models.Patient;
import models.User;
import util.AccountNotificationMailer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import vn.diabetes.service.PatientRegistrationService;
import vn.diabetes.validation.Validators;

@WebServlet("/PatientForm")
public class PatientFormController extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User staff = currentStaff(request);
        if (staff == null) { response.sendRedirect(request.getContextPath() + "/Login"); return; }
        String id = request.getParameter("id");
        if (id != null && id.matches("[1-9][0-9]*")) {
            Patient patient = new PatientDAO().getById(Integer.parseInt(id));
            if (patient == null) { response.sendError(404); return; }
            request.setAttribute("patient", patient); request.setAttribute("editMode", true);
        }
        request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User staff = currentStaff(request);
        if (staff == null) { response.sendRedirect(request.getContextPath() + "/Login"); return; }
        String id = clean(request, "patientId"); String fullName = clean(request, "fullName");
        String dobText = clean(request, "dateOfBirth"); String gender = clean(request, "gender");
        String phone = clean(request, "phone"); String address = clean(request, "address");
        String insurance = clean(request, "healthInsuranceNo");
        try {
            fullName=Validators.fullName(fullName); phone=Validators.phone(phone); gender=Validators.gender(gender); address=Validators.address(address); insurance=Validators.insurance(insurance);
            LocalDate dob = Validators.dateOfBirth(dobText,false);
            if (id.isBlank()) {
                String username = clean(request, "username"); String password = request.getParameter("password");
                String email = clean(request, "email");
                PatientRegistrationService service=new PatientRegistrationService(new PatientRegistrationDAO());
                PatientRegistrationService.Result result=service.register(new PatientRegistrationService.Command(username,password,null,fullName,phone,email,dobText,gender,address,insurance,staff.getUserId()));
                boolean mailed = AccountNotificationMailer.sendAsync(result.email(), result.fullName(), result.username(), result.temporaryPassword(), "PATIENT");
                String message = "Đã tạo hồ sơ và tài khoản " + result.username() + ". ";
                message += mailed ? "Thông tin đăng nhập đang được gửi tới " + email + "."
                        : "Hãy cấp trực tiếp mật khẩu tạm thời cho bệnh nhân.";
                request.getSession().setAttribute("flashSuccess", message);
            } else {
                if (!id.matches("[1-9][0-9]*")) throw new IllegalArgumentException("Mã bệnh nhân không hợp lệ.");
                Patient patient = new PatientDAO().getById(Integer.parseInt(id));
                if (patient == null) { response.sendError(404); return; }
                patient.setFullName(fullName); patient.setDateOfBirth(dob); patient.setGender(gender);
                patient.setPhone(phone); patient.setAddress(address); patient.setHealthInsuranceNo(insurance);
                new PatientDAO().update(patient);
                request.getSession().setAttribute("flashSuccess", "Đã cập nhật thông tin bệnh nhân " + fullName + ".");
            }
            response.sendRedirect(request.getContextPath() + "/PatientList");
        } catch (IllegalArgumentException ex) {
            request.setAttribute("err", ex.getMessage());
            if (!id.isBlank() && id.matches("[1-9][0-9]*")) { request.setAttribute("editMode", true); request.setAttribute("patient", new PatientDAO().getById(Integer.parseInt(id))); }
            request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response);
        }
    }

    private User currentStaff(HttpServletRequest request) { HttpSession s=request.getSession(false); User u=s==null?null:(User)s.getAttribute("user"); return u!=null&&"STAFF".equals(u.getRole())?u:null; }
    private String clean(HttpServletRequest request, String name) { String v=request.getParameter(name); return v==null?"":v.trim(); }
}
