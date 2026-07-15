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
import java.util.Set;

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
            validatePatient(fullName, dobText, gender, phone, address, insurance);
            LocalDate dob = dobText.isBlank() ? null : LocalDate.parse(dobText);
            if (id.isBlank()) {
                String username = clean(request, "username"); String password = request.getParameter("password");
                String email = clean(request, "email");
                if (!username.matches("^[A-Za-z0-9_]{4,30}$")) throw new IllegalArgumentException("Tên đăng nhập gồm 4–30 chữ, số hoặc dấu gạch dưới.");
                if (password == null || password.length() < 8 || password.length() > 72) throw new IllegalArgumentException("Mật khẩu tạm thời phải có từ 8 đến 72 ký tự.");
                if (!email.isBlank() && (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 100)) throw new IllegalArgumentException("Email không hợp lệ.");
                new PatientRegistrationDAO().register(username, password, fullName, phone, email, dob,
                        gender, address, insurance, staff.getUserId());
                boolean mailed = AccountNotificationMailer.sendAsync(email, fullName, username, password, "PATIENT");
                String message = "Đã tạo hồ sơ và tài khoản " + username + ". ";
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

    private void validatePatient(String name, String dob, String gender, String phone, String address, String insurance) {
        if (name.length() < 2 || name.length() > 100) throw new IllegalArgumentException("Họ tên phải có từ 2 đến 100 ký tự.");
        if (!phone.matches("^(0[0-9]{9}|\\+84[0-9]{9})$")) throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        if (!Set.of("Nam", "Nữ", "Khác").contains(gender)) throw new IllegalArgumentException("Giới tính không hợp lệ.");
        if (!dob.isBlank()) { LocalDate date = LocalDate.parse(dob); if (date.isAfter(LocalDate.now()) || date.isBefore(LocalDate.of(1900,1,1))) throw new IllegalArgumentException("Ngày sinh không hợp lệ."); }
        if (address.length() > 255) throw new IllegalArgumentException("Địa chỉ tối đa 255 ký tự.");
        if (!insurance.isBlank() && !insurance.matches("^[A-Za-z0-9]{10,20}$")) throw new IllegalArgumentException("Số BHYT không hợp lệ.");
    }
    private User currentStaff(HttpServletRequest request) { HttpSession s=request.getSession(false); User u=s==null?null:(User)s.getAttribute("user"); return u!=null&&"STAFF".equals(u.getRole())?u:null; }
    private String clean(HttpServletRequest request, String name) { String v=request.getParameter(name); return v==null?"":v.trim(); }
}
