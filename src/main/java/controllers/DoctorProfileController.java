package controllers;

import dal.DoctorDAO;
import models.Doctor;
import models.User;
import util.FileStorageUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.IOException;

/**
 * Trang hồ sơ minh chứng của bác sĩ: cho phép bác sĩ tự upload/cập nhật
 * ảnh khuôn mặt, ảnh CCCD và ảnh chứng chỉ hành nghề của chính mình.
 */
@WebServlet(name = "DoctorProfileController", urlPatterns = {"/DoctorProfile"})
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class DoctorProfileController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"DOCTOR".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login");
            return;
        }

        DoctorDAO doctorDAO = new DoctorDAO();
        Doctor doctor = doctorDAO.getByUserId(user.getUserId());
        request.setAttribute("doctor", doctor);

        if (session.getAttribute("toastMessage") != null) {
            request.setAttribute("toastMessage", session.getAttribute("toastMessage"));
            request.setAttribute("toastType", session.getAttribute("toastType"));
            session.removeAttribute("toastMessage");
            session.removeAttribute("toastType");
        }

        request.getRequestDispatcher("views/doctorProfile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || !"DOCTOR".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login");
            return;
        }

        DoctorDAO doctorDAO = new DoctorDAO();
        Doctor doctor = doctorDAO.getByUserId(user.getUserId());
        if (doctor == null) {
            response.sendRedirect(request.getContextPath() + "/DoctorDashboard");
            return;
        }

        try {
            Part facePart    = request.getPart("faceImage");
            Part cccdPart    = request.getPart("cccdImage");
            Part licensePart = request.getPart("licenseImage");

            String faceFile    = FileStorageUtil.saveDoctorImage(facePart, doctor.getDoctorId(), FileStorageUtil.TYPE_FACE);
            String cccdFile    = FileStorageUtil.saveDoctorImage(cccdPart, doctor.getDoctorId(), FileStorageUtil.TYPE_CCCD);
            String licenseFile = FileStorageUtil.saveDoctorImage(licensePart, doctor.getDoctorId(), FileStorageUtil.TYPE_LICENSE);

            if (faceFile != null || cccdFile != null || licenseFile != null) {
                doctorDAO.updateImages(doctor.getDoctorId(), faceFile, cccdFile, licenseFile);
            }

            session.setAttribute("toastMessage", "Cập nhật ảnh hồ sơ thành công!");
            session.setAttribute("toastType", "success");
        } catch (IOException e) {
            session.setAttribute("toastMessage", "Lỗi upload ảnh: " + e.getMessage());
            session.setAttribute("toastType", "danger");
        }

        response.sendRedirect(request.getContextPath() + "/DoctorProfile");
    }
}
