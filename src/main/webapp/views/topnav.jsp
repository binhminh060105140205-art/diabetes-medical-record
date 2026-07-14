<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
<nav class="topnav">
    <c:choose>
        <c:when test="${sessionScope.user.role == 'ADMIN'}">
            <a href="${pageContext.request.contextPath}/AdminDashboard">🏠 Trang chủ</a>
            <a href="${pageContext.request.contextPath}/AdminCreateUser">➕ Tạo tài khoản</a>
            <a href="${pageContext.request.contextPath}/PatientList">👥 Danh sách bệnh nhân</a>
        </c:when>
        <c:when test="${sessionScope.user.role == 'STAFF'}">
            <a href="${pageContext.request.contextPath}/StaffDashboard">🏠 Trang chủ</a>
            <a href="${pageContext.request.contextPath}/PatientList">👥 Danh sách bệnh nhân</a>
            <a href="${pageContext.request.contextPath}/PatientForm">➕ Tạo bệnh nhân</a>
        </c:when>
        <c:when test="${sessionScope.user.role == 'DOCTOR'}">
            <a href="${pageContext.request.contextPath}/DoctorDashboard">🏠 Trang chủ</a>
            <a href="${pageContext.request.contextPath}/PatientList">👥 Danh sách bệnh nhân</a>
            <a href="${pageContext.request.contextPath}/DoctorProfile">🪪 Hồ sơ minh chứng</a>
        </c:when>
        <c:when test="${sessionScope.user.role == 'PATIENT'}">
            <a href="${pageContext.request.contextPath}/PatientDashboard">🏠 Trang chủ</a>
            <a href="${pageContext.request.contextPath}/PatientAI">🤖 AI Tư Vấn</a>
        </c:when>
    </c:choose>
    <a href="${pageContext.request.contextPath}/EditProfile" style="margin-left:auto;">✏️ Sửa</a>
</nav>
