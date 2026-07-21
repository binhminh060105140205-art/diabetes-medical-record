<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Lỗi — Hệ thống</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260722-web-audit2">
</head>
<body class="error-body">
<jsp:include page="views/header.jsp"/>
<%
    Integer errorCode = (Integer) request.getAttribute(
            jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE);
    int status = errorCode == null ? 500 : errorCode;
    String title;
    String guidance;
    if (status == 400) {
        title = "Dữ liệu gửi lên không hợp lệ";
        guidance = "Kiểm tra lại thông tin đã nhập rồi thử lại.";
    } else if (status == 403) {
        title = "Bạn không có quyền truy cập";
        guidance = "Hãy quay lại khu vực làm việc đúng với tài khoản hiện tại.";
    } else if (status == 404) {
        title = "Không tìm thấy trang hoặc dữ liệu";
        guidance = "Nội dung có thể đã được chuyển, xóa hoặc không còn tồn tại.";
    } else if (status == 409) {
        title = "Dữ liệu đã thay đổi trạng thái";
        guidance = "Tải lại danh sách và thực hiện trên dữ liệu đang còn hiệu lực.";
    } else {
        title = "Hệ thống chưa thể xử lý yêu cầu";
        guidance = "Vui lòng thử lại. Nếu lỗi tiếp tục xảy ra, hãy liên hệ quản trị viên.";
    }
%>
<c:choose>
    <c:when test="${sessionScope.user.role=='ADMIN'}"><c:set var="errorHome" value="/AdminDashboard"/></c:when>
    <c:when test="${sessionScope.user.role=='STAFF'}"><c:set var="errorHome" value="/StaffDashboard"/></c:when>
    <c:when test="${sessionScope.user.role=='DOCTOR'}"><c:set var="errorHome" value="/DoctorDashboard"/></c:when>
    <c:when test="${sessionScope.user.role=='PATIENT'}"><c:set var="errorHome" value="/PatientDashboard"/></c:when>
    <c:otherwise><c:set var="errorHome" value="/Login"/></c:otherwise>
</c:choose>
<div class="page-wrapper" style="text-align:center;padding-top:80px;">
    <div style="font-size:72px;">🏥</div>
    <h1 style="font-size:48px;color:#dc3545;margin:16px 0;">
        <%= status %>
    </h1>
    <h2 style="color:#1a3c6e;margin-bottom:12px;">
        <%= title %>
    </h2>
    <p style="color:#6c757d;margin-bottom:28px;">
        <%= guidance %>
    </p>
    <a href="${pageContext.request.contextPath}${errorHome}" class="btn btn-primary">Về trang làm việc</a>
    <button type="button" class="btn btn-outline" style="background:#6c757d;color:white;margin-left:10px;" onclick="window.history.back()">Quay lại</button>
</div>
<jsp:include page="views/footer.jsp"/>
</body>
</html>
