package controllers;

import dal.*;
import models.*;
import util.FileStorageUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.text.SimpleDateFormat;

@WebServlet("/AdminCreateUser")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class AdminCreateUserController extends HttpServlet {

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
        
        String specialty = request.getParameter("specialty");
        String licenseNo = request.getParameter("licenseNo");
        String degree    = request.getParameter("degree");

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

        if (dobString != null && !dobString.trim().isEmpty()) {
            try {
                java.util.Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dobString);
                newUser.setDob(parsedDate);
            } catch (Exception e) {
                System.out.println("Lỗi parse ngày sinh: " + e.getMessage());
            }
        }

        newUser = userDAO.create(newUser);

        if ("DOCTOR".equals(role) && newUser.getUserId() > 0) {
            DoctorDAO docDAO = new DoctorDAO();
            Doctor doc = new Doctor();
            doc.setUserId(newUser.getUserId());
            doc.setSpecialty(specialty);
            doc.setLicenseNo(licenseNo);
            doc.setDegree(degree);
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
                System.out.println("AdminCreateUser: lỗi upload ảnh bác sĩ - " + ex.getMessage());
            }
        }

        HttpSession currentSession = request.getSession();
        currentSession.setAttribute("toastMessage", "Khởi tạo tài khoản " + role + " thành công!");
        currentSession.setAttribute("toastType", "success");

        if ("DOCTOR".equals(role) && newUser.getUserId() > 0) {
            // Đẩy admin sang thẳng trang hồ sơ bác sĩ vừa tạo để xem lại thông tin/ảnh vừa upload
            response.sendRedirect(request.getContextPath() + "/AdminDoctorDetail?userId=" + newUser.getUserId());
        } else {
            response.sendRedirect(request.getContextPath() + "/AdminCreateUser");
        }
    }
}
