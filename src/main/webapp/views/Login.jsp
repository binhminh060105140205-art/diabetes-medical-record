<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Đăng nhập — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260721-web-audit1">
</head>
<body class="auth-body">
<main class="auth-shell">
    <section class="auth-visual">
        <a class="auth-brand" href="${pageContext.request.contextPath}/">
            <span>+</span><div><strong>DiaCare</strong><small>Chăm sóc tiểu đường toàn diện</small></div>
        </a>
        <div class="auth-copy">
            <div class="auth-label">HỒ SƠ SỨC KHỎE ĐIỆN TỬ</div>
            <h1>Mỗi dữ liệu đúng chỗ.<br>Mỗi quyết định đúng lúc.</h1>
            <p>Nền tảng kết nối tiếp nhận, bác sĩ, xét nghiệm và người bệnh trong một quy trình ngoại trú liền mạch.</p>
            <ul>
                <li><span>✓</span> Hồ sơ tập trung và phân quyền an toàn</li>
                <li><span>✓</span> Lịch hẹn, hàng đợi và lượt khám rõ ràng</li>
                <li><span>✓</span> Kết quả xét nghiệm liên kết trực tiếp bệnh án</li>
            </ul>
        </div>
        <div class="auth-support">Hỗ trợ: <strong>diabetesclinic@gmail.com</strong></div>
    </section>

    <section class="auth-panel">
        <div class="auth-form-wrap">
            <a class="auth-back" href="${pageContext.request.contextPath}/">← Về trang chủ</a>
            <div class="auth-heading">
                <span class="auth-mobile-logo">+</span>
                <h2>Chào mừng trở lại</h2>
                <p>Đăng nhập để tiếp tục vào hệ thống DiaCare.</p>
            </div>

            <c:if test="${not empty sessionScope.registrationSuccess}">
                <div class="alert alert-success"><c:out value="${sessionScope.registrationSuccess}"/></div>
                <c:remove var="registrationSuccess" scope="session"/>
            </c:if>
            <c:if test="${not empty requestScope.err}">
                <div class="alert alert-danger"><span>!</span><c:out value="${requestScope.err}"/></div>
            </c:if>

            <form action="${pageContext.request.contextPath}/Login" method="post" data-validate="login" class="auth-form" novalidate>
                <div class="form-group">
                    <label class="required" for="username">Tên đăng nhập</label>
                    <div class="input-with-icon">
                        <span>👤</span>
                        <input id="username" type="text" name="username" class="form-control"
                               value="${fn:escapeXml(requestScope.username)}" placeholder="Nhập tên đăng nhập"
                               minlength="4" maxlength="50" pattern="[A-Za-z0-9._-]+"
                               autocomplete="username" required autofocus>
                    </div>
                </div>
                <div class="form-group">
                    <div class="label-row"><label class="required" for="password">Mật khẩu</label></div>
                    <div class="input-with-icon">
                        <span>⌑</span>
                        <input id="password" type="password" name="password" class="form-control"
                               placeholder="Nhập mật khẩu" maxlength="72" autocomplete="current-password" required>
                        <button type="button" class="password-toggle" aria-label="Hiện mật khẩu"
                                onclick="const p=document.getElementById('password');p.type=p.type==='password'?'text':'password';this.textContent=p.type==='password'?'Hiện':'Ẩn'">Hiện</button>
                    </div>
                </div>
                <c:if test="${not empty requestScope.lockUntil}">
                    <div class="lock-message">Tài khoản tạm khóa. Thử lại sau <strong id="lock-timer"></strong>.</div>
                </c:if>
                <button type="submit" class="btn btn-primary auth-submit">Đăng nhập an toàn <span>→</span></button>
            </form>

            <div class="auth-register-link">Chưa có hồ sơ? <a href="${pageContext.request.contextPath}/Register"><strong>Đăng ký tài khoản bệnh nhân</strong></a>.</div>
            <div class="auth-note"><span>🔒</span><p>Tài khoản tạm khóa 15 phút sau 5 lần nhập sai liên tiếp.</p></div>
        </div>
        <footer>© 2026 DiaCare</footer>
    </section>
</main>
<script src="${pageContext.request.contextPath}/static/js/main.js?v=20260721-web-audit2"></script>
<script src="${pageContext.request.contextPath}/static/js/validate.js?v=20260721-web-audit2"></script>
<c:if test="${not empty requestScope.lockUntil}">
    <script>
        (function () {
            const lockUntil = ${requestScope.lockUntil};
            const element = document.getElementById('lock-timer');
            const updateTimer = () => {
                const left = lockUntil - Date.now();
                if (left <= 0) {
                    clearInterval(timer);
                    element.textContent = '0 giây';
                    return;
                }
                element.textContent = Math.floor(left / 60000) + ' phút '
                    + Math.floor(left / 1000 % 60) + ' giây';
            };
            const timer = setInterval(updateTimer, 1000);
            updateTimer();
        })();
    </script>
</c:if>
</body>
</html>
