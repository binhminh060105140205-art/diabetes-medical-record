<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Lỗi — Hệ thống</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260719-ai1">
</head>
<body>
<jsp:include page="views/header.jsp"/>
<div class="page-wrapper" style="text-align:center;padding-top:80px;">
    <div style="font-size:72px;">🏥</div>
    <h1 style="font-size:48px;color:#dc3545;margin:16px 0;">
        <%= request.getAttribute("javax.servlet.error.status_code") %>
    </h1>
    <h2 style="color:#1a3c6e;margin-bottom:12px;">
        <% 
            Integer code = (Integer) request.getAttribute("javax.servlet.error.status_code");
            if (code != null && code == 404) { %>Không tìm thấy trang<% }
            else { %>Đã xảy ra lỗi hệ thống<% } %>
    </h2>
    <p style="color:#6c757d;margin-bottom:28px;">
        Vui lòng thử lại hoặc liên hệ quản trị viên.
    </p>
    <a href="${pageContext.request.contextPath}/Login" class="btn btn-primary">🏠 Về trang chủ</a>
    <a href="javascript:history.back()" class="btn btn-outline" style="background:#6c757d;color:white;margin-left:10px;">← Quay lại</a>
</div>
<jsp:include page="views/footer.jsp"/>
</body>
</html>
