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

/**
 * ===========================================================
 * AdminCreateUserController
 * ===========================================================
 * Chức năng:
 * - Cho phép ADMIN tạo tài khoản STAFF hoặc DOCTOR.
 * - Kiểm tra quyền truy cập.
 * - Validate dữ liệu đầu vào.
 * - Upload hồ sơ bác sĩ.
 * - Gửi email thông báo tài khoản.
 * - Chuyển hướng sau khi tạo thành công.
 *
 * Author: Group 2
 * ===========================================================
 */
@WebServlet("/AdminCreateUser")
@MultipartConfig(
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 20 * 1024 * 1024
)
public class AdminCreateUserController extends HttpServlet {

    /**
     * Logger dùng để ghi nhận lỗi trong quá trình tạo tài khoản.
     */
    private static final Logger LOGGER =
            Logger.getLogger(AdminCreateUserController.class.getName());

    /**
     * =======================================================
     * Hiển thị giao diện Create User.
     *
     * Chỉ ADMIN mới được phép truy cập.
     * Đồng thời hiển thị Toast Message sau khi tạo thành công.
     * =======================================================
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // Lấy Session hiện tại (không tạo mới nếu chưa tồn tại)
        HttpSession session = request.getSession(false);

        // Lấy thông tin người dùng đăng nhập
        User user = (session != null)
                ? (User) session.getAttribute("user")
                : null;

        // Kiểm tra quyền ADMIN
        if (user == null || !"ADMIN".equals(user.getRole())) {
            response.sendRedirect("Login");
            return;
        }

        // Hiển thị thông báo sau khi tạo tài khoản thành công
        if (session != null && session.getAttribute("toastMessage") != null) {

            request.setAttribute(
                    "toastMessage",
                    session.getAttribute("toastMessage"));

            request.setAttribute(
                    "toastType",
                    session.getAttribute("toastType"));

            // Xóa thông báo khỏi Session sau khi hiển thị
            session.removeAttribute("toastMessage");
            session.removeAttribute("toastType");
        }

        // Mở giao diện Create User
        request.getRequestDispatcher("views/AdminCreateUser.jsp")
                .forward(request, response);
    }

    /**
     * =======================================================
     * Xử lý tạo tài khoản mới.
     *
     * Quy trình:
     * 1. Kiểm tra quyền ADMIN.
     * 2. Đọc dữ liệu từ Form.
     * 3. Validate dữ liệu.
     * 4. Tạo User.
     * 5. Nếu là Doctor thì tạo Doctor Profile.
     * 6. Upload ảnh hồ sơ.
     * 7. Gửi Email.
     * 8. Redirect.
     * =======================================================
     */
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // Hỗ trợ nhập tiếng Việt
        request.setCharacterEncoding("UTF-8");

        // Lấy Session hiện tại
        HttpSession session = request.getSession(false);

        // Lấy thông tin Admin đang đăng nhập
        User admin = (session != null)
                ? (User) session.getAttribute("user")
                : null;

        // Chỉ ADMIN mới được phép tạo tài khoản
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            response.sendRedirect("Login");
            return;
        }

        // =====================================================
        // Đọc dữ liệu từ Form Create User
        // =====================================================

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String dobString = request.getParameter("dob");
        String gender = request.getParameter("gender");
        String address = request.getParameter("address");
        String role = request.getParameter("role");
        String cccd = request.getParameter("cccd");

        // =====================================================
        // Thông tin dành riêng cho Doctor
        // =====================================================

        // Chuyên khoa mặc định của hệ thống
        String specialty = Doctor.DIABETES_SPECIALTY;

        // Số chứng chỉ hành nghề
        String licenseNo = request.getParameter("licenseNo");

        // Học vị/Bằng cấp
        String degree = request.getParameter("degree");

        // Chuyên môn điều trị tiểu đường
        String diabetesFocus = request.getParameter("diabetesFocus");

        // File upload của bác sĩ
        Part cccdFrontPart = null;
        Part cccdBackPart = null;
        Part licensePart = null;
                try {

            // =====================================================
            // Validate thông tin tài khoản
            // =====================================================

            // Kiểm tra Username
            username = Validators.username(username);

            // Kiểm tra Password
            password = Validators.password(password, "Mật khẩu");

            // Kiểm tra Họ và tên
            fullName = Validators.fullName(fullName);

            // Kiểm tra số điện thoại
            phone = Validators.phone(phone);

            // Kiểm tra Email và đảm bảo Email chưa tồn tại
            email = Validators.email(email, true);

            // Kiểm tra giới tính
            gender = Validators.gender(gender);

            // Kiểm tra địa chỉ
            address = Validators.requiredAddress(address);

            // Kiểm tra Role
            role = Validators.role(role);

            // Kiểm tra CCCD
            cccd = Validators.cccd(
                    Validators.required(cccd, "Số CCCD"));

            // =====================================================
            // Chỉ cho phép ADMIN tạo STAFF hoặc DOCTOR
            // =====================================================

            if (!java.util.Set.of("STAFF", "DOCTOR").contains(role)) {
                throw new IllegalArgumentException(
                        "Chỉ được tạo tài khoản nhân viên tiếp nhận hoặc bác sĩ.");
            }

            // =====================================================
            // Validate thông tin chuyên môn của Doctor
            // =====================================================

            licenseNo = Validators.max(
                    licenseNo,
                    50,
                    "Số chứng chỉ");

            degree = Validators.max(
                    degree,
                    50,
                    "Học vị / Bằng cấp");

            // Nếu Role là DOCTOR thì bắt buộc nhập đầy đủ hồ sơ
            if ("DOCTOR".equals(role)) {

                // Bắt buộc có số chứng chỉ
                licenseNo = Validators.required(
                        licenseNo,
                        "Số chứng chỉ hành nghề");

                // Bắt buộc có học vị
                degree = Validators.required(
                        degree,
                        "Học vị / Bằng cấp");

                // Đọc file upload
                cccdFrontPart = request.getPart("cccdFrontImage");
                cccdBackPart = request.getPart("cccdBackImage");
                licensePart = request.getPart("licenseImage");

                // Kiểm tra định dạng và dung lượng ảnh
                FileStorageUtil.validateDoctorImage(
                        cccdFrontPart,
                        "Ảnh CCCD mặt trước",
                        true);

                FileStorageUtil.validateDoctorImage(
                        cccdBackPart,
                        "Ảnh CCCD mặt sau",
                        true);

                FileStorageUtil.validateDoctorImage(
                        licensePart,
                        "Ảnh chứng chỉ hành nghề",
                        true);
            }

            // Nếu chưa chọn chuyên môn thì mặc định BOTH
            if (diabetesFocus == null
                    || !java.util.Set.of(
                            "TYPE_1",
                            "TYPE_2",
                            "BOTH").contains(diabetesFocus)) {

                diabetesFocus = "BOTH";
            }

        }

        // =====================================================
        // Có lỗi dữ liệu nhập
        // =====================================================
        catch (IllegalArgumentException ex) {

            request.setAttribute("err", ex.getMessage());

            request.getRequestDispatcher(
                    "views/AdminCreateUser.jsp")
                    .forward(request, response);

            return;
        }

        // =====================================================
        // Có lỗi khi upload hoặc đọc ảnh
        // =====================================================
        catch (ServletException | IOException | IllegalStateException ex) {

            request.setAttribute(
                    "err",
                    "Không thể đọc ảnh hồ sơ. "
                    + "Vui lòng chọn ảnh JPG, PNG hoặc WEBP dưới 5MB.");

            request.getRequestDispatcher(
                    "views/AdminCreateUser.jsp")
                    .forward(request, response);

            return;
        }

        // =====================================================
        // Khởi tạo đối tượng User
        // =====================================================

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

        // =====================================================
        // Validate ngày sinh
        // =====================================================

        try {

            LocalDate dob = Validators.dateOfBirth(
                    dobString,
                    true);

            newUser.setDob(java.sql.Date.valueOf(dob));

        } catch (IllegalArgumentException ex) {

            request.setAttribute("err", ex.getMessage());

            request.getRequestDispatcher(
                    "views/AdminCreateUser.jsp")
                    .forward(request, response);

            return;
        }

        // =====================================================
        // Nếu Role là DOCTOR thì tạo Doctor Profile
        // =====================================================

        Doctor doc = null;

        if ("DOCTOR".equals(role)) {

            doc = new Doctor();

            doc.setSpecialty(specialty);

            doc.setLicenseNo(licenseNo);

            doc.setDegree(degree);

            doc.setDiabetesFocus(diabetesFocus);
        }

        // =====================================================
        // Lưu User và Doctor xuống Database
        // =====================================================

        try {

            AdminDAO.CreatedAccount created =
                    new AdminDAO()
                            .createManagedAccount(
                                    newUser,
                                    doc,
                                    admin.getUserId());

            // Lấy dữ liệu sau khi Database tạo thành công
            newUser = created.user();
            doc = created.doctor();
                    }

        // =====================================================
        // Có lỗi nghiệp vụ (Username, Email, CCCD đã tồn tại...)
        // =====================================================
        catch (IllegalArgumentException error) {

            request.setAttribute("err", error.getMessage());

            request.getRequestDispatcher("views/AdminCreateUser.jsp")
                    .forward(request, response);

            return;
        }

        // =====================================================
        // Có lỗi hệ thống hoặc Database
        // =====================================================
        catch (IllegalStateException error) {

            LOGGER.log(Level.SEVERE,
                    "Không thể tạo tài khoản quản lý",
                    error);

            response.setStatus(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            request.setAttribute(
                    "err",
                    "Không thể tạo tài khoản lúc này. Vui lòng thử lại sau.");

            request.getRequestDispatcher("views/AdminCreateUser.jsp")
                    .forward(request, response);

            return;
        }

        // =====================================================
        // Upload hồ sơ của Doctor
        // =====================================================

        if (doc != null) {

            DoctorDAO docDAO = new DoctorDAO();

            try {

                // ---------------------------------------------
                // Lưu ảnh CCCD mặt trước
                // ---------------------------------------------
                String cccdFrontFile =
                        FileStorageUtil.saveDoctorImage(
                                cccdFrontPart,
                                doc.getDoctorId(),
                                FileStorageUtil.TYPE_CCCD);

                // ---------------------------------------------
                // Lưu ảnh CCCD mặt sau
                // ---------------------------------------------
                String cccdBackFile =
                        FileStorageUtil.saveDoctorImage(
                                cccdBackPart,
                                doc.getDoctorId(),
                                FileStorageUtil.TYPE_CCCD_BACK);

                // ---------------------------------------------
                // Lưu ảnh chứng chỉ hành nghề
                // ---------------------------------------------
                String licenseFile =
                        FileStorageUtil.saveDoctorImage(
                                licensePart,
                                doc.getDoctorId(),
                                FileStorageUtil.TYPE_LICENSE);

                // ---------------------------------------------
                // Nếu có ít nhất một ảnh thì cập nhật Database
                // ---------------------------------------------
                if (cccdFrontFile != null
                        || cccdBackFile != null
                        || licenseFile != null) {

                    docDAO.updateImages(
                            doc.getDoctorId(),
                            cccdFrontFile,
                            cccdBackFile,
                            licenseFile);
                }

            }

            // =================================================
            // Nếu Upload ảnh thất bại
            // =================================================
            catch (Exception ex) {

                LOGGER.log(
                        Level.SEVERE,
                        "Không thể lưu ảnh hồ sơ bác sĩ userId="
                                + newUser.getUserId(),
                        ex);

                // ---------------------------------------------
                // Xóa các ảnh đã upload
                // ---------------------------------------------
                FileStorageUtil.deleteDoctorImages(
                        doc.getDoctorId());

                // ---------------------------------------------
                // Rollback tài khoản vừa tạo
                // ---------------------------------------------
                try {

                    new AdminDAO()
                            .rollbackFreshManagedAccount(
                                    newUser.getUserId());

                }

                catch (RuntimeException cleanupError) {

                    LOGGER.log(
                            Level.SEVERE,
                            "Không thể hoàn tác tài khoản bác sĩ sau lỗi lưu ảnh userId="
                                    + newUser.getUserId(),
                            cleanupError);

                }

                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                request.setAttribute(
                        "err",
                        "Không thể lưu đủ ảnh hồ sơ bác sĩ nên tài khoản chưa được tạo. "
                                + "Vui lòng thử lại.");

                request.getRequestDispatcher(
                        "views/AdminCreateUser.jsp")
                        .forward(request, response);

                return;
            }
        }

        // =====================================================
        // Gửi Email thông báo tài khoản
        // =====================================================

        HttpSession currentSession = request.getSession();

        // Đưa Email vào hàng đợi gửi (Async)
        boolean queued =
                AccountNotificationMailer.sendAsync(
                        email,
                        fullName,
                        username,
                        password,
                        role);

        // Hiển thị tên Role bằng tiếng Việt
        String roleLabel =
                "DOCTOR".equals(role)
                        ? "bác sĩ"
                        : "nhân viên tiếp nhận";

        // =====================================================
        // Hiển thị Toast Message
        // =====================================================

        currentSession.setAttribute(
                "toastMessage",

                queued

                        ? "Đã tạo tài khoản "
                                + roleLabel
                                + ". Thông tin đăng nhập đã được đưa vào hàng đợi gửi tới "
                                + email
                                + "."

                        : "Đã tạo tài khoản "
                                + roleLabel
                                + ". Chưa thể gửi email vì máy chủ chưa cấu hình dịch vụ email.");

        currentSession.setAttribute(
                "toastType",
                "success");

        // =====================================================
        // Chuyển hướng sau khi tạo thành công
        // =====================================================

        if (doc != null) {

            // ---------------------------------------------
            // Nếu là Doctor
            // Chuyển đến trang Doctor Detail để Admin
            // kiểm tra hồ sơ và ảnh vừa upload
            // ---------------------------------------------
            response.sendRedirect(
                    request.getContextPath()
                    + "/AdminDoctorDetail?userId="
                    + newUser.getUserId());

        } else {

            // ---------------------------------------------
            // Nếu là Staff
            // Quay về trang Create User
            // để Admin tiếp tục tạo tài khoản mới
            // ---------------------------------------------
            response.sendRedirect(
                    request.getContextPath()
                    + "/AdminCreateUser");
        }

    }
}
