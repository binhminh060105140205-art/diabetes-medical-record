<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Hồ sơ cá nhân — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ux1">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<main class="page-wrapper">
    <div class="page-heading"><div><div class="eyebrow">TÀI KHOẢN CÁ NHÂN</div><h1 class="page-title">Hồ sơ và cài đặt</h1><p class="text-muted">Cập nhật thông tin liên hệ. Chỉ nhập mật khẩu khi bạn thực sự muốn đổi mật khẩu.</p></div></div>
    <c:if test="${not empty successMessage}"><div class="alert alert-success"><c:out value="${successMessage}"/></div></c:if>
    <c:if test="${not empty errorMessage}"><div class="alert alert-danger"><c:out value="${errorMessage}"/></div></c:if>
    <section class="card profile-form-card">
        <div class="section-header"><div><h2>Thông tin cá nhân</h2><p>Các trường có dấu * cần được điền đầy đủ.</p></div></div>
        <form action="${pageContext.request.contextPath}/EditProfile" method="post">
            <div class="form-row">
                <div class="form-group"><label class="required">Tên đăng nhập</label><input class="form-control" type="text" name="username" value="${profileUser.username}" minlength="4" maxlength="50" pattern="[A-Za-z0-9._-]+" required></div>
                <div class="form-group"><label>Mật khẩu mới</label><input class="form-control" type="password" name="password" minlength="8" maxlength="72" autocomplete="new-password" placeholder="Để trống nếu không đổi"><small>Tối thiểu 8 ký tự.</small></div>
            </div>
            <div class="form-row">
                <div class="form-group"><label class="required">Họ và tên</label><input class="form-control" type="text" name="fullName" value="${profileUser.fullName}" minlength="2" maxlength="100" required></div>
                <div class="form-group"><label>Thư điện tử</label><input class="form-control" type="email" name="email" value="${profileUser.email}"></div>
                <div class="form-group"><label>Số điện thoại</label><input class="form-control" type="tel" name="phone" value="${profileUser.phone}" pattern="(0|\+84)[0-9]{9}" maxlength="12"></div>
            </div>
            <div class="form-row">
                <div class="form-group"><label>CCCD / CMND</label><input class="form-control" type="text" name="cccd" value="${profileUser.cccd}" inputmode="numeric" pattern="([0-9]{9}|[0-9]{12})" maxlength="12"></div>
                <div class="form-group"><label>Ngày sinh</label><input class="form-control" type="date" name="dob" value="${profileUser.dob}"></div>
                <div class="form-group"><label>Giới tính</label><select class="form-control" name="gender"><option value="">Chọn giới tính</option><option value="Nam" ${profileUser.genderLabel=='Nam'?'selected':''}>Nam</option><option value="Nữ" ${profileUser.genderLabel=='Nữ'?'selected':''}>Nữ</option><option value="Khác" ${profileUser.genderLabel=='Khác'?'selected':''}>Khác</option></select></div>
            </div>
            <div class="form-actions"><button type="submit" class="btn btn-primary">Lưu thay đổi</button></div>
        </form>
    </section>
</main>
<jsp:include page="footer.jsp"/>
</body>
</html>
