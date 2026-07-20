<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Đăng ký — DiaCare</title><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ux1"></head>
<body class="register-body">
<main class="register-shell">
    <section class="register-intro">
        <a class="auth-brand" href="${pageContext.request.contextPath}/"><span>+</span><div><strong>DiaCare</strong><small>Cổng thông tin bệnh nhân</small></div></a>
        <div><span class="eyebrow">ĐĂNG KÝ TRỰC TUYẾN</span><h1>Tạo hồ sơ để chủ động đặt lịch và theo dõi kết quả.</h1><p>Dành cho bệnh nhân chưa từng có hồ sơ tại DiaCare. Nếu đã từng khám, vui lòng liên hệ lễ tân để tránh tạo hồ sơ trùng.</p><ul><li>✓ Đăng ký một lần</li><li>✓ Đặt lịch trực tuyến</li><li>✓ Xem lịch sử khám và kết quả</li></ul></div>
        <a class="btn btn-outline-light" href="${pageContext.request.contextPath}/Login">Tôi đã có tài khoản</a>
    </section>
    <section class="register-panel">
        <div class="register-form-wrap">
            <div class="page-heading"><div><span class="eyebrow">TÀI KHOẢN BỆNH NHÂN</span><h2>Thông tin đăng ký</h2><p class="text-muted">Điền thông tin cá nhân trước, sau đó tạo thông tin đăng nhập.</p></div></div>
            <c:if test="${not empty err}"><div class="alert alert-danger"><c:out value="${err}"/></div></c:if>
            <form method="post" action="${pageContext.request.contextPath}/Register" data-validate="register" class="registration-form">
                <div class="section-header"><div><h2>1. Thông tin bệnh nhân</h2><p>Các trường có dấu * là bắt buộc.</p></div></div>
                <div class="form-grid register-grid">
                    <div class="form-group"><label class="required">Họ và tên</label><input class="form-control" name="fullName" maxlength="100" value="${param.fullName}" autocomplete="name" required></div>
                    <div class="form-group"><label class="required">Số điện thoại</label><input class="form-control" name="phone" maxlength="13" value="${param.phone}" placeholder="0912345678" autocomplete="tel" required></div>
                    <div class="form-group"><label class="required">Thư điện tử</label><input class="form-control" type="email" name="email" maxlength="100" value="${param.email}" autocomplete="email" required></div>
                    <div class="form-group"><label class="required">Ngày sinh</label><input class="form-control" type="date" name="dateOfBirth" value="${param.dateOfBirth}" required></div>
                    <div class="form-group"><label class="required">Giới tính</label><select class="form-control" name="gender" required><option value="">Chọn giới tính</option><option>Nam</option><option>Nữ</option><option>Khác</option></select></div>
                    <div class="form-group"><label>Số BHYT</label><input class="form-control" name="healthInsuranceNo" maxlength="20" value="${param.healthInsuranceNo}" placeholder="Nếu có"></div>
                    <div class="form-group form-span"><label>Địa chỉ</label><input class="form-control" name="address" maxlength="255" value="${param.address}" placeholder="Số nhà, đường, quận/huyện, tỉnh/thành phố"></div>
                </div>
                <div class="account-section"><div class="section-header"><div><h2>2. Thông tin đăng nhập</h2><p>Ghi nhớ tên đăng nhập để sử dụng sau khi đăng ký.</p></div></div><div class="form-grid register-grid">
                    <div class="form-group"><label class="required">Tên đăng nhập</label><input class="form-control" name="username" minlength="4" maxlength="30" pattern="[A-Za-z0-9_]+" value="${param.username}" autocomplete="username" required><small>Chữ, số hoặc dấu gạch dưới.</small></div>
                    <div class="form-group"><label class="required">Mật khẩu</label><input class="form-control" type="password" name="password" minlength="8" maxlength="72" autocomplete="new-password" required></div>
                    <div class="form-group"><label class="required">Nhập lại mật khẩu</label><input class="form-control" type="password" name="confirmPassword" minlength="8" maxlength="72" autocomplete="new-password" required></div>
                </div></div>
                <div class="form-actions"><button class="btn btn-primary btn-lg" type="submit">Tạo tài khoản</button><a href="${pageContext.request.contextPath}/" class="btn btn-light">Về trang chủ</a></div>
            </form>
        </div>
    </section>
</main>
<script src="${pageContext.request.contextPath}/static/js/main.js?v=20260720-ux1"></script><script src="${pageContext.request.contextPath}/static/js/validate.js?v=20260720-ux1"></script>
</body>
</html>
