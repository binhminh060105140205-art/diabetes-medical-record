<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Lỗi — Hệ thống</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260721-web-audit1">
</head>
<body class="error-body">
<jsp:include page="views/header.jsp"/>
<% Integer errorCode = (Integer) request.getAttribute(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE); %>
<div class="page-wrapper" style="text-align:center;padding-top:80px;">
    <div style="font-size:72px;">🏥</div>
    <h1 style="font-size:48px;color:#dc3545;margin:16px 0;">
        <%= errorCode != null ? errorCode : 500 %>
    </h1>
    <h2 style="color:#1a3c6e;margin-bottom:12px;">
        <% 
            if (errorCode != null && errorCode == 404) { %>Không tìm thấy trang<% }
            else { %>Đã xảy ra lỗi hệ thống<% } %>
    </h2>
    <p style="color:#6c757d;margin-bottom:28px;">
        Vui lòng thử lại hoặc liên hệ quản trị viên.
    </p>
    <a href="${pageContext.request.contextPath}/Login" class="btn btn-primary">Về trang làm việc</a>
    <button type="button" class="btn btn-outline" style="background:#6c757d;color:white;margin-left:10px;" onclick="window.history.back()">Quay lại</button>
</div>
<jsp:include page="views/footer.jsp"/>
</body>
</html>
