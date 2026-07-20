<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Hồ sơ hành nghề — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ux1">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<main class="page-wrapper">
    <div class="page-heading"><div><div class="eyebrow">HỒ SƠ BÁC SĨ</div><h1 class="page-title">Minh chứng hành nghề</h1><p class="text-muted">Chỉ chọn ảnh cần thay đổi; ảnh không chọn sẽ được giữ nguyên.</p></div><a class="btn btn-light" href="${pageContext.request.contextPath}/DoctorDashboard">Quay lại tổng quan</a></div>
    <c:if test="${not empty toastMessage}"><div class="alert alert-${toastType=='danger'?'danger':'success'}"><c:out value="${toastMessage}"/></div></c:if>
    <c:if test="${empty doctor}"><div class="alert alert-danger">Không tìm thấy hồ sơ bác sĩ tương ứng với tài khoản này.</div></c:if>
    <c:if test="${not empty doctor}">
        <section class="card profile-form-card">
            <div class="section-header"><div><h2>Tài liệu xác minh</h2><p>Chấp nhận JPG, PNG hoặc WEBP; tối đa 5MB mỗi ảnh.</p></div></div>
            <form action="${pageContext.request.contextPath}/DoctorProfile" method="post" enctype="multipart/form-data">
                <div class="document-grid">
                    <label class="document-upload"><strong>Ảnh khuôn mặt</strong><c:choose><c:when test="${not empty doctor.faceImagePath}"><img src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=face" alt="Ảnh khuôn mặt hiện tại"></c:when><c:otherwise><span class="document-placeholder">Chưa có ảnh</span></c:otherwise></c:choose><input type="file" name="faceImage" accept="image/png,image/jpeg,image/webp"></label>
                    <label class="document-upload"><strong>Ảnh CCCD</strong><c:choose><c:when test="${not empty doctor.cccdImagePath}"><img src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=cccd" alt="Ảnh CCCD hiện tại"></c:when><c:otherwise><span class="document-placeholder">Chưa có ảnh</span></c:otherwise></c:choose><input type="file" name="cccdImage" accept="image/png,image/jpeg,image/webp"></label>
                    <label class="document-upload"><strong>Chứng chỉ hành nghề</strong><c:choose><c:when test="${not empty doctor.licenseImagePath}"><img src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=license" alt="Ảnh chứng chỉ hiện tại"></c:when><c:otherwise><span class="document-placeholder">Chưa có ảnh</span></c:otherwise></c:choose><input type="file" name="licenseImage" accept="image/png,image/jpeg,image/webp"></label>
                </div>
                <div class="form-actions"><button type="submit" class="btn btn-primary">Lưu ảnh đã chọn</button></div>
            </form>
        </section>
    </c:if>
</main>
<jsp:include page="footer.jsp"/>
</body>
</html>
