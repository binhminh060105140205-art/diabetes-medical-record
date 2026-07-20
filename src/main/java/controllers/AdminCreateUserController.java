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

        try {
            username=Validators.username(username); password=Validators.password(password,"Mật khẩu");
            fullName=Validators.fullName(fullName); phone=Validators.phone(phone); email=Validators.email(email,false);
            gender=Validators.gender(gender); address=Validators.address(address); role=Validators.role(role); cccd=Validators.cccd(cccd);
            if("PATIENT".equals(role))throw new IllegalArgumentException("Hãy tạo tài khoản bệnh nhân tại màn Tiếp nhận bệnh nhân.");
            licenseNo=Validators.max(licenseNo,50,"Số chứng chỉ"); degree=Validators.max(degree,100,"Học vị");
            if("DOCTOR".equals(role)) licenseNo=Validators.required(licenseNo,"Số chứng chỉ hành nghề");
            if (diabetesFocus == null || !java.util.Set.of("TYPE_1","TYPE_2","BOTH").contains(diabetesFocus)) diabetesFocus="BOTH";
        } catch(IllegalArgumentException ex) {
            request.setAttribute("err",ex.getMessage()); request.getRequestDispatcher("views/AdminCreateUser.jsp").forward(request,response); return;
        }

        UserDAO userDAO = new UserDAO();
        if (userDAO.usernameExists(username)) {
            request.setAttribute("err", "Tên đăng nhập đã tồn tại.");
            request.getRequestDispatcher("views/AdminCreateUser.jsp").forward(request, response);
            return;
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

        try { LocalDate dob=Validators.dateOfBirth(dobString,false); if(dob!=null)newUser.setDob(java.sql.Date.valueOf(dob)); }
        catch(IllegalArgumentException ex){request.setAttribute("err",ex.getMessage());request.getRequestDispatcher("views/AdminCreateUser.jsp").forward(request,response);return;}

        newUser = userDAO.create(newUser);

        if ("DOCTOR".equals(role) && newUser.getUserId() > 0) {
            DoctorDAO docDAO = new DoctorDAO();
            Doctor doc = new Doctor();
            doc.setUserId(newUser.getUserId());
            doc.setSpecialty(specialty);
            doc.setLicenseNo(licenseNo);
            doc.setDegree(degree);
            doc.setDiabetesFocus(diabetesFocus);
            docDAO.create(doc);

            try {
                Part facePart    = request.getPart("faceImage");
                Part cccdPart    = request.getPart("cccdImage");
                Part licensePart = request.getPart("licenseImage");

                String faceFile    = FileStorageUtil.saveDoctorImage(facePart, doc.getDoctorId(), FileStorageUtil.TYPE_FACE);
                String cccdFile    = FileStorageUtil.saveDoctorImage(cccdPart, doc.getDoctorId(), FileStorageUtil.TYPE_CCCD);
                String licenseFile = FileStorageUtil.saveDoctorImage(licensePart, doc.getDoctorId(), FileStorageUtil.TYPE_LICENSE);

                if (faceFile != null || cccdFile != null || licenseFile != null) {
                    docDAO.updateImages(doc.getDoctorId(), faceFile, cccdFile, licenseFile);
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

        if ("DOCTOR".equals(role) && newUser.getUserId() > 0) {
            // Đẩy admin sang thẳng trang hồ sơ bác sĩ vừa tạo để xem lại thông tin/ảnh vừa upload
            response.sendRedirect(request.getContextPath() + "/AdminDoctorDetail?userId=" + newUser.getUserId());
        } else {
            response.sendRedirect(request.getContextPath() + "/AdminCreateUser");
        }
    }
}
