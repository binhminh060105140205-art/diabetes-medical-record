<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Tạo Tài Khoản</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260719-ai1">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <h1 class="page-title">➕ Tạo Tài Khoản Staff / Doctor</h1>

    <c:if test="${not empty toastMessage}">
        <div class="alert alert-success">${toastMessage}</div>
    </c:if>
    <c:if test="${not empty err}">
        <div class="alert alert-danger">${err}</div>
    </c:if>

    <div class="card">
        <form action="${pageContext.request.contextPath}/AdminCreateUser" method="post" enctype="multipart/form-data" data-validate="createUser">
            <div class="form-row">
                <div class="form-group">
                    <label class="required">Tên đăng nhập</label>
                    <input type="text" name="username" class="form-control" required>
                </div>
                <div class="form-group">
                    <label class="required">Mật khẩu</label>
                    <input type="password" name="password" class="form-control" required>
                </div>
                <div class="form-group">
                    <label class="required">Role</label>
                    <select name="role" class="form-control" id="roleSelect" onchange="toggleDoctorFields()">
                        <option value="STAFF">STAFF</option>
                        <option value="DOCTOR">DOCTOR</option>
                    </select>
                </div>
            </div>
            
            <div class="form-row">
                <div class="form-group">
                    <label class="required">Họ và tên</label>
                    <input type="text" name="fullName" class="form-control" required>
                </div>
                <div class="form-group">
                    <label class="required">Email (Nhận thông báo)</label>
                    <input type="email" name="email" class="form-control" required>
                </div>
                <div class="form-group">
                    <label>Số điện thoại</label>
                    <input type="text" name="phone" class="form-control">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label>Ngày sinh</label>
                    <input type="date" name="dob" class="form-control">
                </div>
                <div class="form-group">
                    <label>Giới tính</label>
                    <select name="gender" class="form-control">
                        <option value="Male">Nam</option>
                        <option value="Female">Nữ</option>
                        <option value="Other">Khác</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Số CCCD</label>
                    <input type="text" name="cccd" class="form-control">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label>Địa chỉ</label>
                    <input type="text" name="address" class="form-control">
                </div>
            </div>

            <div id="doctorFields" style="display:none;">
                <hr class="section-divider">
                <div class="card-title" style="font-size:14px;">Thông tin Bác sĩ</div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Chuyên khoa</label>
                        <input type="text" name="specialty" class="form-control" placeholder="Nội tiết - Tiểu đường">
                    </div>
                    <div class="form-group">
                        <label>Số chứng chỉ hành nghề</label>
                        <input type="text" name="licenseNo" class="form-control">
                    </div>
                    <div class="form-group">
                        <label>Học vị / Bằng cấp</label>
                        <input type="text" name="degree" class="form-control" placeholder="Thạc sĩ, Bác sĩ CKI...">
                    </div>
                    <div class="form-group">
                        <label>Nhóm tiểu đường ưu tiên</label>
                        <select name="diabetesFocus" class="form-control">
                            <option value="GENERAL">Chuyên khoa hỗ trợ</option>
                            <option value="TYPE_1">Ưu tiên Type 1</option>
                            <option value="TYPE_2">Ưu tiên Type 2</option>
                            <option value="BOTH">Type 1 &amp; Type 2</option>
                        </select>
                        <small>Chỉ dùng để gợi ý khi đặt lịch, không khóa lựa chọn.</small>
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Ảnh khuôn mặt</label>
                        <input type="file" name="faceImage" class="form-control" accept="image/png,image/jpeg,image/webp">
                    </div>
                    <div class="form-group">
                        <label>Ảnh CCCD</label>
                        <input type="file" name="cccdImage" class="form-control" accept="image/png,image/jpeg,image/webp">
                    </div>
                    <div class="form-group">
                        <label>Ảnh chứng chỉ hành nghề</label>
                        <input type="file" name="licenseImage" class="form-control" accept="image/png,image/jpeg,image/webp">
                    </div>
                </div>
                <p style="font-size:12px;color:#6c757d;margin-top:-8px;">Có thể bỏ trống và bổ sung ảnh sau tại trang "Hồ sơ" của bác sĩ. Chỉ nhận JPG/PNG/WEBP, tối đa 5MB mỗi ảnh.</p>
            </div>

            <button type="submit" class="btn btn-primary">✅ Tạo tài khoản</button>
            <a href="${pageContext.request.contextPath}/AdminDashboard" class="btn btn-outline" style="background:#6c757d;color:white;margin-left:8px;">Hủy</a>
        </form>
    </div>
</div>
<script>
    function toggleDoctorFields() {
        const role = document.getElementById('roleSelect').value;
        document.getElementById('doctorFields').style.display = (role === 'DOCTOR') ? 'block' : 'none';
    }
</script>
<jsp:include page="footer.jsp"/>
<script src="${pageContext.request.contextPath}/static/js/main.js?v=20260719-ai1"></script>
<script src="${pageContext.request.contextPath}/static/js/validate.js?v=20260719-ai1"></script>
</body>
</html>
