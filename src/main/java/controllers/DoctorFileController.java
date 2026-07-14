package controllers;

import dal.DoctorDAO;
import models.Doctor;
import models.User;
import util.FileStorageUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Phục vụ (stream) ảnh khuôn mặt / CCCD / chứng chỉ hành nghề của bác sĩ.
 * Ảnh khuôn mặt: mọi người dùng đã đăng nhập đều xem được.
 * Ảnh CCCD / chứng chỉ hành nghề: chỉ ADMIN hoặc chính bác sĩ đó được xem (giấy tờ nhạy cảm).
 */
@WebServlet(name = "DoctorFileController", urlPatterns = {"/DoctorFile"})
public class DoctorFileController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("user") : null;
        if (currentUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        int doctorId;
        try {
            doctorId = Integer.parseInt(request.getParameter("doctorId"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String type = request.getParameter("type");
        if (type == null || (!type.equals(FileStorageUtil.TYPE_FACE)
                && !type.equals(FileStorageUtil.TYPE_CCCD)
                && !type.equals(FileStorageUtil.TYPE_LICENSE))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        DoctorDAO doctorDAO = new DoctorDAO();
        Doctor doctor = doctorDAO.getById(doctorId);
        if (doctor == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        boolean isOwner = doctor.getUserId() == currentUser.getUserId();
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        boolean isSensitive = !type.equals(FileStorageUtil.TYPE_FACE);
        if (isSensitive && !isOwner && !isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String storedFileName;
        switch (type) {
            case FileStorageUtil.TYPE_CCCD:    storedFileName = doctor.getCccdImagePath(); break;
            case FileStorageUtil.TYPE_LICENSE: storedFileName = doctor.getLicenseImagePath(); break;
            default:                           storedFileName = doctor.getFaceImagePath(); break;
        }

        File imageFile = FileStorageUtil.resolveDoctorImage(doctorId, storedFileName);
        if (imageFile == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(imageFile.toPath());
        response.setContentType(contentType != null ? contentType : "application/octet-stream");
        response.setContentLengthLong(imageFile.length());
        Files.copy(imageFile.toPath(), response.getOutputStream());
    }
}
