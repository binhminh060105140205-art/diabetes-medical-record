<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<header class="site-header">
    <a class="header-brand" href="${pageContext.request.contextPath}/" aria-label="Về trang chính DiaCare">
        <span class="header-icon">+</span>
        <span class="header-title">DiaCare <small>Hệ thống quản lý phòng khám</small></span>
    </a>
    <div class="header-actions">
        <c:choose>
            <c:when test="${not empty sessionScope.user}">
                <a href="${pageContext.request.contextPath}/" class="header-home-link">
                    <span aria-hidden="true">⌂</span> Về trang chính
                </a>
                <span class="header-greeting">
                    Xin chào, <strong><c:out value="${sessionScope.user.fullName}"/></strong>
                    <span class="role-badge role-${sessionScope.user.role.toLowerCase()}">${sessionScope.user.role}</span>
                </span>
                <a href="${pageContext.request.contextPath}/Logout" class="btn btn-outline">Đăng xuất</a>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/Login" class="btn btn-primary">Đăng nhập</a>
            </c:otherwise>
        </c:choose>
    </div>
</header>
