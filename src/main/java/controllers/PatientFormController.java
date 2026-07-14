package controllers;

import dal.PatientDAO;
import dal.UserDAO;
import models.Patient;
import models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/PatientForm")
public class PatientFormController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || !"STAFF".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }
        String idParam = request.getParameter("id");
        if (idParam != null) {
            request.setAttribute("patient", new PatientDAO().getById(Integer.parseInt(idParam)));
            request.setAttribute("editMode", true);
        }
        request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User staff = session != null ? (User) session.getAttribute("user") : null;
        if (staff == null || !"STAFF".equals(staff.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }

        String idParam  = request.getParameter("patientId");
        String fullName = request.getParameter("fullName");
        String dob      = request.getParameter("dateOfBirth");
        String gender   = request.getParameter("gender");
        String phone    = request.getParameter("phone");
        String address  = request.getParameter("address");
        String bhyt     = request.getParameter("healthInsuranceNo");

        PatientDAO patientDAO = new PatientDAO();
        UserDAO    userDAO    = new UserDAO();

        // ── TẠO MỚI ─────────────────────────────────────────────────────
        if (idParam == null || idParam.isEmpty()) {

            // Validate bắt buộc
            if (fullName == null || fullName.trim().isEmpty()) {
                request.setAttribute("err", "Họ tên không được để trống.");
                request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response); return;
            }
            if (phone == null || phone.trim().isEmpty()) {
                request.setAttribute("err", "Số điện thoại không được để trống.");
                request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response); return;
            }
            if (!phone.trim().matches("^(0|\\+84)[0-9]{9}$")) {
                request.setAttribute("err", "Số điện thoại không hợp lệ (VD: 0912345678).");
                request.setAttribute("fullName", fullName);
                request.setAttribute("phone", phone);
                request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response); return;
            }
            if (dob != null && !dob.isEmpty()) {
                int year = LocalDate.parse(dob).getYear();
                if (year > LocalDate.now().getYear() - 1 || year < 1900) {
                    request.setAttribute("err", "Ngày sinh không hợp lệ.");
                    request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response); return;
                }
            }

            // Auto-generate username/password
            String username = "bn_" + phone.trim();
            String password = "Patient@123";

            if (userDAO.usernameExists(username)) {
                request.setAttribute("err",
                    "Số điện thoại " + phone + " đã tồn tại trong hệ thống. Vui lòng tìm kiếm bệnh nhân cũ.");
                request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response); return;
            }

            // INSERT vào Users trước
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(password);
            newUser.setFullName(fullName.trim());
            newUser.setPhone(phone.trim());
            newUser.setRole("PATIENT");
            newUser = userDAO.create(newUser);

            if (newUser.getUserId() <= 0) {
                request.setAttribute("err", "Lỗi tạo tài khoản. Vui lòng thử lại.");
                request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response); return;
            }

            // INSERT vào Patients (liên kết user_id)
            Patient p = new Patient();
            p.setUserId(newUser.getUserId());
            p.setFullName(fullName.trim());
            p.setDateOfBirth(dob != null && !dob.isEmpty() ? LocalDate.parse(dob) : null);
            p.setGender(gender);
            p.setPhone(phone.trim());
            p.setAddress(address);
            p.setHealthInsuranceNo(bhyt);
            p.setCreatedBy(staff.getUserId());
            p = patientDAO.create(p);

            if (p.getPatientId() <= 0) {
                request.setAttribute("err", "Lỗi tạo hồ sơ bệnh nhân. Vui lòng thử lại.");
                request.getRequestDispatcher("views/PatientForm.jsp").forward(request, response); return;
            }

            // Thành công
            HttpSession sess = request.getSession();
            sess.setAttribute("flashSuccess",
                "✅ Đã tạo bệnh nhân: <strong>" + fullName + "</strong>"
                + " | Tài khoản: <code>" + username + "</code>"
                + " / Mật khẩu: <code>" + password + "</code>");
            response.sendRedirect(request.getContextPath() + "/PatientList");

        // ── CẬP NHẬT ─────────────────────────────────────────────────────
        } else {
            Patient p = patientDAO.getById(Integer.parseInt(idParam));
            if (p == null) { response.sendRedirect(request.getContextPath() + "/PatientList"); return; }
            p.setFullName(fullName);
            p.setDateOfBirth(dob != null && !dob.isEmpty() ? LocalDate.parse(dob) : null);
            p.setGender(gender);
            p.setPhone(phone);
            p.setAddress(address);
            p.setHealthInsuranceNo(bhyt);
            patientDAO.update(p);

            request.getSession().setAttribute("flashSuccess", "✅ Đã cập nhật thông tin: " + fullName);
            response.sendRedirect(request.getContextPath() + "/PatientList");
        }
    }
}
