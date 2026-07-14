<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Hồ Sơ Bệnh Nhân</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <h1 class="page-title">
        <c:choose>
            <c:when test="${editMode}">✏️ Cập nhật thông tin bệnh nhân</c:when>
            <c:otherwise>➕ Tạo hồ sơ bệnh nhân mới</c:otherwise>
        </c:choose>
    </h1>

    <c:if test="${not empty err}">
        <div class="alert alert-danger">${err}</div>
    </c:if>
    <c:if test="${not empty success}">
        <div class="alert alert-success">${success}</div>
    </c:if>

    <div class="card">
        <div class="card-title">📋 Thông tin hành chính bệnh nhân</div>
        <form action="${pageContext.request.contextPath}/PatientForm" method="post"
              onsubmit="return validatePatientForm()">
            <input type="hidden" name="patientId" value="${patient.patientId}">

            <div class="form-row">
                <div class="form-group">
                    <label class="required">Họ và tên</label>
                    <input type="text" name="fullName" id="fullName" class="form-control"
                           value="${not empty param.fullName ? param.fullName : patient.fullName}"
                           placeholder="Nguyễn Văn A" required>
                    <span class="err-msg" id="err_fullName"></span>
                </div>
                <div class="form-group">
                    <label>Ngày sinh</label>
                    <input type="date" name="dateOfBirth" class="form-control"
                           value="${patient.dateOfBirth}" max="${maxDOB}">
                    <span class="err-msg" id="err_dob"></span>
                </div>
                <div class="form-group">
                    <label>Giới tính</label>
                    <select name="gender" class="form-control">
                        <option value="Nam"  <c:if test="${patient.gender=='Nam' || empty patient.gender}">selected</c:if>>Nam</option>
                        <option value="Nữ"   <c:if test="${patient.gender=='Nữ'}">selected</c:if>>Nữ</option>
                        <option value="Khác" <c:if test="${patient.gender=='Khác'}">selected</c:if>>Khác</option>
                    </select>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label class="required">Số điện thoại</label>
                    <input type="text" name="phone" id="phone" class="form-control"
                           value="${not empty param.phone ? param.phone : patient.phone}"
                           placeholder="0912345678" required>
                    <span class="err-msg" id="err_phone"></span>
                </div>
                <div class="form-group">
                    <label>Số BHYT</label>
                    <input type="text" name="healthInsuranceNo" class="form-control"
                           value="${patient.healthInsuranceNo}"
                           placeholder="HC4012345678">
                    <span class="err-msg" id="err_bhyt"></span>
                </div>
            </div>

            <div class="form-group">
                <label>Địa chỉ</label>
                <textarea name="address" class="form-control"
                          placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố">${patient.address}</textarea>
            </div>

            <c:if test="${not editMode}">
            <div class="alert alert-info" style="margin-top:8px;">
                💡 Hệ thống tự tạo tài khoản bệnh nhân:<br>
                &nbsp;&nbsp;• Username: <strong>bn_[SĐT]</strong><br>
                &nbsp;&nbsp;• Mật khẩu mặc định: <strong>Patient@123</strong>
            </div>
            </c:if>

            <div style="margin-top:16px;display:flex;gap:10px;">
                <button type="submit" class="btn btn-primary">
                    <c:choose>
                        <c:when test="${editMode}">💾 Lưu thay đổi</c:when>
                        <c:otherwise>✅ Tạo hồ sơ & tài khoản</c:otherwise>
                    </c:choose>
                </button>
                <a href="${pageContext.request.contextPath}/PatientList"
                   class="btn btn-outline" style="background:#6c757d;color:white;">Hủy</a>
            </div>
        </form>
    </div>
</div>

<script>
function validatePatientForm() {
    let ok = true;
    document.querySelectorAll('.err-msg').forEach(e => e.textContent = '');
    document.querySelectorAll('.input-error').forEach(e => e.classList.remove('input-error'));

    const fn = document.getElementById('fullName').value.trim();
    if (fn.length < 2) {
        document.getElementById('err_fullName').textContent = '⚠ Họ tên phải có ít nhất 2 ký tự';
        document.getElementById('fullName').classList.add('input-error');
        ok = false;
    }

    const phone = document.getElementById('phone').value.trim();
    if (!/^(0|\+84)[0-9]{9}$/.test(phone)) {
        document.getElementById('err_phone').textContent = '⚠ Số điện thoại không hợp lệ (VD: 0912345678)';
        document.getElementById('phone').classList.add('input-error');
        ok = false;
    }

    if (!ok) window.scrollTo({ top: 0, behavior: 'smooth' });
    return ok;
}
</script>
<script src="${pageContext.request.contextPath}/static/js/main.js"></script>
<jsp:include page="footer.jsp"/>
</body>
</html>
