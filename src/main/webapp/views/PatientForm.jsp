<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Hồ Sơ Bệnh Nhân</title>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260722-web-audit2">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <div class="page-heading"><div><div class="eyebrow">HỒ SƠ BỆNH NHÂN</div><h1 class="page-title">Cập nhật thông tin bệnh nhân</h1><p class="text-muted">Kiểm tra số điện thoại và thông tin nhận diện trước khi lưu.</p></div><a class="btn btn-light" href="${pageContext.request.contextPath}/StaffDashboard#patients">Quay lại danh sách</a></div>

    <c:if test="${not empty err}">
        <div class="alert alert-danger"><c:out value="${err}"/></div>
    </c:if>
    <c:if test="${not empty success}">
        <div class="alert alert-success"><c:out value="${success}"/></div>
    </c:if>

    <div class="card">
        <div class="card-title">Thông tin hành chính</div>
        <form action="${pageContext.request.contextPath}/PatientForm" method="post" data-validate="patient" novalidate>
            <input type="hidden" name="patientId" value="${patient.patientId}">

            <div class="form-row">
                <div class="form-group">
                    <label class="required">Họ và tên</label>
                    <input type="text" name="fullName" id="fullName" class="form-control"
                           value="${fn:escapeXml(not empty param.fullName ? param.fullName : patient.fullName)}"
                           minlength="2" maxlength="100" placeholder="Nguyễn Văn A" required>
                    <span class="err-msg" id="err_fullName"></span>
                </div>
                <div class="form-group">
                    <label class="required">Ngày sinh</label>
                    <input type="date" name="dateOfBirth" class="form-control"
                           value="${fn:escapeXml(not empty param.dateOfBirth ? param.dateOfBirth : patient.dateOfBirth)}"
                           min="1900-01-01" max="${maxDOB}" required>
                    <span class="err-msg" id="err_dob"></span>
                </div>
                <div class="form-group">
                    <label>Giới tính</label>
                    <c:set var="selectedGender" value="${not empty param.gender ? param.gender : patient.genderLabel}"/>
                    <select name="gender" class="form-control">
                        <option value="Nam" ${empty selectedGender||selectedGender=='Nam'?'selected':''}>Nam</option>
                        <option value="Nữ" ${selectedGender=='Nữ'?'selected':''}>Nữ</option>
                        <option value="Khác" ${selectedGender=='Khác'?'selected':''}>Khác</option>
                    </select>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label class="required">Số điện thoại</label>
                    <input type="tel" name="phone" id="phone" class="form-control"
                           value="${fn:escapeXml(not empty param.phone ? param.phone : patient.phone)}"
                           pattern="(0|\+84)[0-9]{9}" maxlength="12" placeholder="0912345678" required>
                    <span class="err-msg" id="err_phone"></span>
                </div>
                <div class="form-group">
                    <label>Số BHYT</label>
                    <input type="text" name="healthInsuranceNo" class="form-control"
                           value="${fn:escapeXml(not empty param.healthInsuranceNo ? param.healthInsuranceNo : patient.healthInsuranceNo)}"
                           pattern="[A-Za-z0-9]{10,20}" maxlength="20" placeholder="HC4012345678">
                    <span class="err-msg" id="err_bhyt"></span>
                </div>
            </div>

            <div class="form-group">
                <label class="required">Địa chỉ</label>
                <textarea name="address" class="form-control"
                          maxlength="255" placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố" required><c:out value="${not empty param.address ? param.address : patient.address}"/></textarea>
            </div>

            <c:if test="${not editMode}">
            <div class="account-section"><div class="card-title">Tài khoản đăng nhập</div><p class="text-muted">Có Email/Gmail: hệ thống đưa thông tin vào hàng đợi gửi tự động. Không có email: cấp trực tiếp cho bệnh nhân.</p><div class="form-row"><div class="form-group"><label class="required">Tên đăng nhập</label><input class="form-control" name="username" minlength="4" maxlength="30" pattern="[A-Za-z0-9_]+" value="${fn:escapeXml(param.username)}" placeholder="Ví dụ: nguyenvana" required></div><div class="form-group"><label class="required">Mật khẩu tạm thời</label><input class="form-control" type="password" name="password" minlength="8" maxlength="72" autocomplete="new-password" required></div><div class="form-group"><label>Email/Gmail nhận tài khoản</label><input class="form-control" type="email" name="email" maxlength="100" value="${fn:escapeXml(param.email)}" placeholder="benhnhan@gmail.com"></div></div></div>
            </c:if>

            <div class="form-actions">
                <button type="submit" class="btn btn-primary">
                    <c:choose>
                        <c:when test="${editMode}">Lưu thay đổi</c:when>
                        <c:otherwise>Tạo hồ sơ và tài khoản</c:otherwise>
                    </c:choose>
                </button>
                <a href="${pageContext.request.contextPath}/StaffDashboard#patients" class="btn btn-light">Hủy</a>
            </div>
        </form>
    </div>
</div>

<script src="${pageContext.request.contextPath}/static/js/validate.js?v=20260722-validation1"></script>
<jsp:include page="footer.jsp"/>
</body>
</html>
