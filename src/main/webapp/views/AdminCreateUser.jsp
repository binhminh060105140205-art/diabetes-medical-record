<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Tạo Tài Khoản</title>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ui7">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <div class="page-heading"><div><div class="eyebrow">QUẢN LÝ NHÂN SỰ</div><h1 class="page-title">Tạo tài khoản nhân viên hoặc bác sĩ</h1><p class="text-muted">Thông tin hành nghề chỉ xuất hiện khi chọn vai trò Bác sĩ.</p></div><a class="btn btn-light" href="${pageContext.request.contextPath}/AdminDashboard">Quay lại tài khoản</a></div>

    <c:if test="${not empty toastMessage}">
        <div class="alert alert-success">${toastMessage}</div>
    </c:if>
    <c:if test="${not empty err}">
        <div class="alert alert-danger"><c:out value="${err}"/></div>
    </c:if>

    <div class="card">
        <form action="${pageContext.request.contextPath}/AdminCreateUser" method="post" enctype="multipart/form-data" data-validate="createUser">
            <div class="form-row">
                <div class="form-group">
                    <label class="required">Tên đăng nhập</label>
                    <input type="text" name="username" class="form-control" minlength="4" maxlength="30" pattern="[A-Za-z0-9_]+" autocomplete="username" required>
                </div>
                <div class="form-group">
                    <label class="required">Mật khẩu</label>
                    <input type="password" name="password" class="form-control" minlength="8" maxlength="72" autocomplete="new-password" required>
                </div>
                <div class="form-group">
                    <label class="required">Vai trò</label>
                    <select name="role" class="form-control" id="roleSelect" onchange="toggleDoctorFields()">
                        <option value="STAFF" ${param.role!='DOCTOR'?'selected':''}>Nhân viên tiếp nhận</option>
                        <option value="DOCTOR" ${param.role=='DOCTOR'?'selected':''}>Bác sĩ</option>
                    </select>
                </div>
            </div>
            
            <div class="form-row">
                <div class="form-group">
                    <label class="required">Họ và tên</label>
                    <input type="text" name="fullName" class="form-control" minlength="2" maxlength="100" autocomplete="name" required>
                </div>
                <div class="form-group">
                    <label class="required">Email/Gmail nhận thông báo</label>
                    <input type="email" name="email" class="form-control" maxlength="100" autocomplete="email" required>
                </div>
                <div class="form-group">
                    <label class="required">Số điện thoại</label>
                    <input type="tel" name="phone" class="form-control" pattern="(0|\+84)[0-9]{9}" maxlength="12" autocomplete="tel" required>
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
                        <option value="Nam">Nam</option>
                        <option value="Nữ">Nữ</option>
                        <option value="Khác">Khác</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Số CCCD</label>
                    <input type="text" name="cccd" class="form-control" inputmode="numeric" pattern="[0-9]{12}" maxlength="12">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label>Địa chỉ</label>
                    <input type="text" name="address" class="form-control" maxlength="255" autocomplete="street-address">
                </div>
            </div>

            <div id="doctorFields" class="doctor-fields">
                <hr class="section-divider">
                <div class="card-title section-title-small">Thông tin bác sĩ</div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Chuyên khoa</label>
                        <input type="text" name="specialty" class="form-control" value="Nội tiết - Đái tháo đường" readonly>
                        <small>Hệ thống chỉ quản lý bác sĩ điều trị đái tháo đường.</small>
                    </div>
                    <div class="form-group">
                        <label class="required">Số chứng chỉ hành nghề</label>
                        <input type="text" name="licenseNo" class="form-control" maxlength="50" data-doctor-required>
                    </div>
                    <div class="form-group">
                        <label>Học vị / Bằng cấp</label>
                        <input type="text" name="degree" class="form-control" maxlength="50" placeholder="Thạc sĩ, Bác sĩ CKI...">
                    </div>
                    <div class="form-group">
                        <label>Nhóm tiểu đường ưu tiên</label>
                        <select name="diabetesFocus" class="form-control">
                            <option value="BOTH" selected>Theo dõi cả típ 1 và típ 2</option>
                            <option value="TYPE_1">Ưu tiên đái tháo đường típ 1</option>
                            <option value="TYPE_2">Ưu tiên đái tháo đường típ 2</option>
                        </select>
                        <small>Chỉ chọn riêng một típ khi bác sĩ có phạm vi chuyên môn ưu tiên rõ ràng.</small>
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
                <p class="form-hint file-hint">Có thể bỏ trống và bổ sung ảnh sau tại trang Hồ sơ của bác sĩ. Chỉ nhận JPG/PNG/WEBP, tối đa 5MB mỗi ảnh.</p>
            </div>

            <div class="form-actions"><button type="submit" class="btn btn-primary">Tạo tài khoản</button><a href="${pageContext.request.contextPath}/AdminDashboard" class="btn btn-light">Hủy</a></div>
        </form>
    </div>
</div>
<script>
    function toggleDoctorFields() {
        const role = document.getElementById('roleSelect').value;
        const doctorSelected = role === 'DOCTOR';
        document.getElementById('doctorFields').style.display = doctorSelected ? 'block' : 'none';
        document.querySelectorAll('[data-doctor-required]').forEach(function (field) {
            field.required = doctorSelected;
        });
    }
    toggleDoctorFields();
</script>
<jsp:include page="footer.jsp"/>
<script src="${pageContext.request.contextPath}/static/js/validate.js?v=20260720-ui3"></script>
</body>
</html>
