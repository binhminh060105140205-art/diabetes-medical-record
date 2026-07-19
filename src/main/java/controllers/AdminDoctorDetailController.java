package controllers;

import dal.DoctorDAO;
import dal.UserDAO;
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
 * Trang admin xem hồ sơ minh chứng của một bác sĩ (ảnh khuôn mặt, CCCD, chứng chỉ
 * hành nghề) để đối chiếu/xác minh, và cho phép admin upload/thay thế ảnh hộ bác sĩ
 * nếu cần (ví dụ bác sĩ chưa tự nộp được).
 */
@WebServlet(name = "AdminDoctorDetailController", urlPatterns = {"/AdminDoctorDetail"})
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class AdminDoctorDetailController extends HttpServlet {

    // Trả về targetUser, hoặc null nếu không hợp lệ (đã gửi lỗi qua response trong trường hợp đó).
    private User loadTargetDoctorUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int userId;
        try {
            userId = Integer.parseInt(request.getParameter("userId"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        User targetUser = new UserDAO().getById(userId);
        if (targetUser == null || !"DOCTOR".equals(targetUser.getRole())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return targetUser;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User admin = (session != null) ? (User) session.getAttribute("user") : null;
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login");
            return;
        }

        User targetUser = loadTargetDoctorUser(request, response);
        if (targetUser == null) return; // lỗi đã được gửi trong loadTargetDoctorUser

        Doctor doctor = new DoctorDAO().getByUserId(targetUser.getUserId());
        request.setAttribute("targetUser", targetUser);
        request.setAttribute("doctor", doctor);

        if (session.getAttribute("toastMessage") != null) {
            request.setAttribute("toastMessage", session.getAttribute("toastMessage"));
            request.setAttribute("toastType", session.getAttribute("toastType"));
            session.removeAttribute("toastMessage");
            session.removeAttribute("toastType");
        }

        request.getRequestDispatcher("views/AdminDoctorDetail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User admin = (session != null) ? (User) session.getAttribute("user") : null;
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login");
            return;
        }

        User targetUser = loadTargetDoctorUser(request, response);
        if (targetUser == null) return;

        Doctor doctor = new DoctorDAO().getByUserId(targetUser.getUserId());
        if (doctor == null) {
            session.setAttribute("toastMessage", "Bác sĩ này chưa có hồ sơ Doctors tương ứng, không thể lưu ảnh.");
            session.setAttribute("toastType", "danger");
            response.sendRedirect(request.getContextPath() + "/AdminDoctorDetail?userId=" + targetUser.getUserId());
            return;
        }

        int userId = targetUser.getUserId();
        try {
            String focus = request.getParameter("diabetesFocus");
            if (!java.util.Set.of("TYPE_1", "TYPE_2", "BOTH", "GENERAL").contains(focus)) {
                throw new IllegalArgumentException("Nhóm tiểu đường không hợp lệ.");
            }
            new DoctorDAO().updateDiabetesFocus(doctor.getDoctorId(), focus);
            Part facePart    = request.getPart("faceImage");
            Part cccdPart    = request.getPart("cccdImage");
            Part licensePart = request.getPart("licenseImage");

            String faceFile    = FileStorageUtil.saveDoctorImage(facePart, doctor.getDoctorId(), FileStorageUtil.TYPE_FACE);
            String cccdFile    = FileStorageUtil.saveDoctorImage(cccdPart, doctor.getDoctorId(), FileStorageUtil.TYPE_CCCD);
            String licenseFile = FileStorageUtil.saveDoctorImage(licensePart, doctor.getDoctorId(), FileStorageUtil.TYPE_LICENSE);

            if (faceFile != null || cccdFile != null || licenseFile != null) {
                new DoctorDAO().updateImages(doctor.getDoctorId(), faceFile, cccdFile, licenseFile);
            }

            session.setAttribute("toastMessage", "Cập nhật ảnh hồ sơ bác sĩ thành công!");
            session.setAttribute("toastType", "success");
        } catch (IOException | IllegalArgumentException e) {
            session.setAttribute("toastMessage", "Lỗi upload ảnh: " + e.getMessage());
            session.setAttribute("toastType", "danger");
        }

        response.sendRedirect(request.getContextPath() + "/AdminDoctorDetail?userId=" + userId);
    }
}
