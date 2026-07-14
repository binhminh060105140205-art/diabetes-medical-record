<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Đăng nhập — Hồ Sơ Bệnh Án Tiểu Đường</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<div class="login-page">
    <div class="login-box">
        <div class="login-logo">🏥</div>
        <h2>HỒ SƠ BỆNH ÁN<br>TIỂU ĐƯỜNG</h2>

        <c:if test="${not empty requestScope.err}">
            <div class="alert alert-danger">${requestScope.err}</div>
        </c:if>

        <form action="${pageContext.request.contextPath}/Login" method="post" data-validate="login">
            <div class="form-group">
                <label class="required">Tên đăng nhập</label>
                <input type="text" name="username" class="form-control"
                       value="${requestScope.username}" placeholder="Nhập username" required autofocus>
            </div>
            <div class="form-group">
                <label class="required">Mật khẩu</label>
                <input type="password" name="password" class="form-control"
                       placeholder="Nhập mật khẩu" required>
            </div>
            <button type="submit" class="btn btn-primary" style="width:100%;margin-top:8px;">
                🔐 Đăng Nhập
            </button>
        </form>

        <p class="text-muted mt-2" style="text-align:center;font-size:12px;">
            Database cũ: admin1/1 | Bản demo mới: admin1/123456
        </p>
    </div>
</div>
<script src="${pageContext.request.contextPath}/static/js/main.js"></script>
<script src="${pageContext.request.contextPath}/static/js/validate.js"></script>
<c:if test="${not empty requestScope.lockUntil}">
    <script>
        (function() {
            const lockUntil = ${requestScope.lockUntil};
            const timerEl = document.getElementById('lock-timer');
            if (!timerEl) return;
            const interval = setInterval(() => {
                const now = Date.now();
                const remaining = lockUntil - now;
                if (remaining <= 0) {
                    clearInterval(interval);
                    window.location.reload();
                    return;
                }
                const m = Math.floor(remaining / 60000);
                const s = Math.floor((remaining / 1000) % 60);
                timerEl.innerText = m + " phút " + s + " giây";
            }, 1000);
        })();
    </script>
</c:if>
</body>
</html>
