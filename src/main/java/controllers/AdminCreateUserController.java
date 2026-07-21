package controllers;

import dal.*;
import models.*;
import util.FileStorageUtil;
import util.AccountNotificationMailer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.diabetes.validation.Validators;

@WebServlet("/AdminCreateUser")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class AdminCreateUserController extends HttpServlet {
    private static final Logger LOGGER=Logger.getLogger(AdminCreateUserController.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"ADMIN".equals(user.getRole())) {
            response.sendRedirect("Login"); return;
        }

        if (session != null && session.getAttribute("toastMessage") != null) {
            request.setAttribute("toastMessage", session.getAttribute("toastMessage"));
            request.setAttribute("toastType", session.getAttribute("toastType"));
            session.removeAttribute("toastMessage");
            session.removeAttribute("toastType");
        }

        request.getRequestDispatcher("views/AdminCreateUser.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User admin = (session != null) ? (User) session.getAttribute("user") : null;
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            response.sendRedirect("Login"); return;
        }

        String username  = request.getParameter("username");
        String password  = request.getParameter("password");
        String fullName  = request.getParameter("fullName");
        String phone     = request.getParameter("phone");
        String email     = request.getParameter("email");
        String dobString = request.getParameter("dob");
        String gender    = request.getParameter("gender");
        String address   = request.getParameter("address");
        String role      = request.getParameter("role"); 
        String cccd      = request.getParameter("cccd");
        
        String specialty = Doctor.DIABETES_SPECIALTY;
        String licenseNo = request.getParameter("licenseNo");
        String degree    = request.getParameter("degree");
        String diabetesFocus = request.getParameter("diabetesFocus");
        Part cccdFrontPart = null;
        Part cccdBackPart = null;
        Part licensePart = null;

        try {
            username=Validators.username(username); password=Validators.password(password,"Mật khẩu");
            fullName=Validators.fullName(fullName); phone=Validators.phone(phone); email=Validators.email(email,true);
            gender=Validators.gender(gender);
            address=Validators.max(Validators.required(address,"Địa chỉ"),255,"Địa chỉ");
            role=Validators.role(role);
            cccd=Validators.cccd(Validators.required(cccd,"Số CCCD"));
            if (!java.util.Set.of("STAFF", "DOCTOR").contains(role)) {
                throw new IllegalArgumentException("Chỉ được tạo tài khoản nhân viên tiếp nhận hoặc bác sĩ.");
            }
            licenseNo=Validators.max(licenseNo,50,"Số chứng chỉ"); degree=Validators.max(degree,50,"Học vị / Bằng cấp");
            if("DOCTOR".equals(role)) {
                licenseNo=Validators.required(licenseNo,"Số chứng chỉ hành nghề");
                degree=Validators.required(degree,"Học vị / Bằng cấp");
                cccdFrontPart=request.getPart("cccdFrontImage");
                cccdBackPart=request.getPart("cccdBackImage");
                licensePart=request.getPart("licenseImage");
                FileStorageUtil.validateDoctorImage(cccdFrontPart,"Ảnh CCCD mặt trước",true);
                FileStorageUtil.validateDoctorImage(cccdBackPart,"Ảnh CCCD mặt sau",true);
                FileStorageUtil.validateDoctorImage(licensePart,"Ảnh chứng chỉ hành nghề",true);
            }
            if (diabetesFocus == null || !java.util.Set.of("TYPE_1","TYPE_2","BOTH").contains(diabetesFocus)) diabetesFocus="BOTH";
        } catch(IllegalArgumentException ex) {
            request.setAttribute("err",ex.getMessage()); request.getRequestDispatcher("views/AdminCreateUser.jsp").forward(request,response); return;
        } catch (ServletException | IOException | IllegalStateException ex) {
            request.setAttribute("err", "Không thể đọc ảnh hồ sơ. Vui lòng chọn ảnh JPG, PNG hoặc WEBP dưới 5MB.");
            request.getRequestDispatcher("views/AdminCreateUser.jsp").forward(request,response); return;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setFullName(fullName);
        newUser.setPhone(phone);
        newUser.setEmail(email);
        newUser.setGender(gender);
        newUser.setAddress(address);
        newUser.setRole(role);
        newUser.setCccd(cccd);

        try { LocalDate dob=Validators.dateOfBirth(dobString,true); newUser.setDob(java.sql.Date.valueOf(dob)); }
        catch(IllegalArgumentException ex){request.setAttribute("err",ex.getMessage());request.getRequestDispatcher("views/AdminCreateUser.jsp").forward(request,response);return;}

        Doctor doc = null;
        if ("DOCTOR".equals(role)) {
            doc = new Doctor();
            doc.setSpecialty(specialty);
            doc.setLicenseNo(licenseNo);
            doc.setDegree(degree);
            doc.setDiabetesFocus(diabetesFocus);
        }

        try {
            AdminDAO.CreatedAccount created = new AdminDAO()
                    .createManagedAccount(newUser, doc, admin.getUserId());
            newUser = created.user();
            doc = created.doctor();
        } catch (IllegalArgumentException error) {
            request.setAttribute("err", error.getMessage());
            request.getRequestDispatcher("views/AdminCreateUser.jsp").forward(request, response);
            return;
        } catch (IllegalStateException error) {
            LOGGER.log(Level.SEVERE, "Không thể tạo tài khoản quản lý", error);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("err", "Không thể tạo tài khoản lúc này. Vui lòng thử lại sau.");
            request.getRequestDispatcher("views/AdminCreateUser.jsp").forward(request, response);
            return;
        }

        if (doc != null) {
            DoctorDAO docDAO = new DoctorDAO();

            try {
                String cccdFrontFile = FileStorageUtil.saveDoctorImage(cccdFrontPart, doc.getDoctorId(), FileStorageUtil.TYPE_CCCD);
                String cccdBackFile  = FileStorageUtil.saveDoctorImage(cccdBackPart, doc.getDoctorId(), FileStorageUtil.TYPE_CCCD_BACK);
                String licenseFile = FileStorageUtil.saveDoctorImage(licensePart, doc.getDoctorId(), FileStorageUtil.TYPE_LICENSE);

                if (cccdFrontFile != null || cccdBackFile != null || licenseFile != null) {
                    docDAO.updateImages(doc.getDoctorId(), cccdFrontFile, cccdBackFile, licenseFile);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING,"Không thể lưu ảnh hồ sơ bác sĩ userId="+newUser.getUserId(),ex);
            }
        }

        HttpSession currentSession = request.getSession();
        boolean queued = AccountNotificationMailer.sendAsync(email, fullName, username, password, role);
        String roleLabel = "DOCTOR".equals(role) ? "bác sĩ" : "nhân viên tiếp nhận";
        currentSession.setAttribute("toastMessage", queued
                ? "Đã tạo tài khoản " + roleLabel + ". Thông tin đăng nhập đã được đưa vào hàng đợi gửi tới " + email + "."
                : "Đã tạo tài khoản " + roleLabel + ". Chưa thể gửi email vì máy chủ chưa cấu hình dịch vụ email.");
        currentSession.setAttribute("toastType", "success");

        if (doc != null) {
            // Đẩy admin sang thẳng trang hồ sơ bác sĩ vừa tạo để xem lại thông tin/ảnh vừa upload
            response.sendRedirect(request.getContextPath() + "/AdminDoctorDetail?userId=" + newUser.getUserId());
        } else {
            response.sendRedirect(request.getContextPath() + "/AdminCreateUser");
        }
    }
}
