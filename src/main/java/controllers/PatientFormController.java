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
        User staff = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(staff, "STAFF")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }
        String id = request.getParameter("id");
        if (id == null || id.isBlank()) {
            response.sendRedirect(request.getContextPath()
                    + "/StaffDashboard?newPatient=1#new-patient");
            return;
        }
        if (!id.matches("[1-9][0-9]*")) { response.sendError(400); return; }
        Patient patient = new PatientDAO().getById(Integer.parseInt(id));
        if (patient == null) { response.sendError(404); return; }
        request.setAttribute("patient", patient);
        request.setAttribute("editMode", true);
        request.setAttribute("maxDOB", LocalDate.now().toString());
        request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User staff = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(staff, "STAFF")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }
        String id = ControllerSupport.clean(request.getParameter("patientId"));
        String fullName = ControllerSupport.clean(request.getParameter("fullName"));
        String dobText = ControllerSupport.clean(request.getParameter("dateOfBirth"));
        String gender = ControllerSupport.clean(request.getParameter("gender"));
        String phone = ControllerSupport.clean(request.getParameter("phone"));
        String address = ControllerSupport.clean(request.getParameter("address"));
        String insurance = ControllerSupport.clean(request.getParameter("healthInsuranceNo"));
        try {
            fullName=Validators.fullName(fullName); phone=Validators.phone(phone); gender=Validators.gender(gender);
            address=Validators.max(Validators.required(address,"Địa chỉ"),255,"Địa chỉ"); insurance=Validators.insurance(insurance);
            LocalDate dob = Validators.dateOfBirth(dobText,true);
            if (id.isBlank()) {
                String username = ControllerSupport.clean(request.getParameter("username"));
                String password = request.getParameter("password");
                String email = ControllerSupport.clean(request.getParameter("email"));
                PatientRegistrationService service=new PatientRegistrationService(new PatientRegistrationDAO());
                PatientRegistrationService.Result result=service.register(new PatientRegistrationService.Command(username,password,null,fullName,phone,email,dobText,gender,address,insurance,staff.getUserId()));
                boolean mailed = AccountNotificationMailer.sendAsync(result.email(), result.fullName(), result.username(), result.temporaryPassword(), "PATIENT");
                String message = "Đã tạo hồ sơ và tài khoản " + result.username() + ". ";
                message += mailed ? "Thông tin đăng nhập đã được đưa vào hàng đợi gửi tới " + email + "."
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
            response.sendRedirect(request.getContextPath() + "/StaffDashboard#patients");
        } catch (IllegalArgumentException ex) {
            if (id.isBlank()) {
                request.setAttribute("intakeError", ex.getMessage());
                request.setAttribute("showIntakeForm", true);
                request.getRequestDispatcher("/StaffDashboard").forward(request, response);
                return;
            }
            if (!id.matches("[1-9][0-9]*")) {
                request.setAttribute("intakeError", "Mã bệnh nhân không hợp lệ.");
                request.setAttribute("showIntakeForm", true);
                request.getRequestDispatcher("/StaffDashboard").forward(request, response);
                return;
            }
            request.setAttribute("err", ex.getMessage());
            request.setAttribute("editMode", true);
            request.setAttribute("patient", new PatientDAO().getById(Integer.parseInt(id)));
            request.setAttribute("maxDOB", LocalDate.now().toString());
            request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response);
        } catch (IllegalStateException ex) {
            request.setAttribute("intakeError",
                    "Không thể lưu hồ sơ lúc này. Vui lòng kiểm tra dữ liệu và thử lại.");
            request.setAttribute("showIntakeForm", true);
            request.getRequestDispatcher("/StaffDashboard").forward(request, response);
        }
    }
}
