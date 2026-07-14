<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
<meta name="csrf-token" content="${sessionScope.csrfToken}">
<div class="site-header">
    <div class="header-brand">
        <span class="header-icon">+</span>
        <span class="header-title">DiaCare <small>Quản lý sức khỏe</small></span>
    </div>
    <div class="header-actions">
        <c:choose>
            <c:when test="${not empty sessionScope.user}">
                <span class="header-greeting">
                    Xin chào, <strong><c:out value="${sessionScope.user.fullName}"/></strong>
                    <span class="role-badge role-${sessionScope.user.role.toLowerCase()}">${sessionScope.user.role}</span>
                </span>
                <a href="${pageContext.request.contextPath}/Logout" class="btn btn-outline">Đăng Xuất</a>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/Login" class="btn btn-primary">Đăng Nhập</a>
            </c:otherwise>
        </c:choose>
    </div>
</div>
