<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<c:set var="brandTarget" value="/"/>
<c:choose>
    <c:when test="${sessionScope.user.role=='ADMIN'}"><c:set var="brandTarget" value="/AdminDashboard"/></c:when>
    <c:when test="${sessionScope.user.role=='STAFF'}"><c:set var="brandTarget" value="/StaffDashboard"/></c:when>
    <c:when test="${sessionScope.user.role=='DOCTOR'}"><c:set var="brandTarget" value="/DoctorDashboard"/></c:when>
    <c:when test="${sessionScope.user.role=='PATIENT'}"><c:set var="brandTarget" value="/PatientDashboard"/></c:when>
</c:choose>
<header class="site-header">
    <a class="header-brand" href="${pageContext.request.contextPath}${brandTarget}" aria-label="Về trang làm việc chính DiaCare">
        <span class="header-icon">+</span>
        <span class="header-title">DiaCare <small>Điều phối chăm sóc tiểu đường</small></span>
    </a>
    <div class="header-actions">
        <c:choose>
            <c:when test="${not empty sessionScope.user}">
                <span class="header-session"><span class="session-dot" aria-hidden="true"></span>Phiên làm việc an toàn</span>
                <a href="${pageContext.request.contextPath}/Settings" class="header-profile-link">
                    <span><c:out value="${sessionScope.user.fullName}"/></span>
                    <small>Cài đặt tài khoản</small>
                </a>
                <form action="${pageContext.request.contextPath}/Logout" method="post" class="header-logout-form">
                    <button type="submit" class="header-logout">Đăng xuất</button>
                </form>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/Login" class="btn btn-primary">Đăng nhập</a>
            </c:otherwise>
        </c:choose>
    </div>
</header>
