<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Lịch sử đăng nhập — DiaCare</title><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260722-web-audit2"></head>
<body><jsp:include page="header.jsp"/><jsp:include page="topnav.jsp"/>
<main class="page-wrapper app-workspace">
    <div class="workspace-heading"><div><span class="workspace-kicker">BẢO MẬT HỆ THỐNG</span><h1>Lịch sử đăng nhập</h1></div><a class="btn btn-light" href="${pageContext.request.contextPath}/AdminDashboard">Quay lại quản trị</a></div>
    <section class="card">
        <form method="get" action="${pageContext.request.contextPath}/AdminLoginHistory" class="operations-toolbar admin-filter-bar">
            <label class="table-filter"><span class="sr-only">Tìm lịch sử</span><input type="search" name="keyword" maxlength="80" value="${fn:escapeXml(keyword)}" placeholder="Tên, tài khoản hoặc địa chỉ IP"></label>
            <label class="admin-filter-field"><span>Sự kiện</span><select class="form-control" name="eventType"><option value="">Tất cả</option><option value="LOGIN" ${eventType=='LOGIN'?'selected':''}>Đăng nhập</option><option value="LOGOUT" ${eventType=='LOGOUT'?'selected':''}>Đăng xuất</option></select></label>
            <button class="btn btn-primary" type="submit">Lọc</button><a class="btn btn-light" href="${pageContext.request.contextPath}/AdminLoginHistory">Xóa lọc</a>
        </form>
        <div class="section-header"><div><h2>Danh sách sự kiện</h2></div><span class="data-count">${history.size()} sự kiện trên trang</span></div>
        <div class="table-scroll"><table><thead><tr><th>Thời gian</th><th>Người dùng</th><th>Vai trò</th><th>Sự kiện</th><th>Địa chỉ IP</th><th>Thiết bị</th></tr></thead><tbody>
        <c:forEach var="item" items="${history}"><tr><td><fmt:formatDate value="${item.occurred_at}" pattern="dd/MM/yyyy HH:mm:ss"/></td><td><strong><c:out value="${item.full_name}"/></strong><small class="table-sub"><c:out value="${item.username}"/></small></td><td><span class="role-badge role-${fn:toLowerCase(item.role)}">${item.role=='ADMIN'?'Quản trị viên':item.role=='DOCTOR'?'Bác sĩ':item.role=='STAFF'?'Nhân viên tiếp nhận':'Bệnh nhân'}</span></td><td><span class="status-pill status-${item.event_type}">${item.event_type=='LOGIN'?'Đăng nhập':'Đăng xuất'}</span></td><td><code><c:out value="${item.ip_address}" default="—"/></code></td><td><span class="user-agent"><c:out value="${item.user_agent}" default="—"/></span></td></tr></c:forEach>
        <c:if test="${empty history}"><tr><td colspan="6" class="empty-state">Chưa có sự kiện phù hợp.</td></tr></c:if>
        </tbody></table></div>
        <c:if test="${hasPrevious || hasNext}">
            <nav class="pagination-container" aria-label="Phân trang lịch sử đăng nhập">
                <c:if test="${hasPrevious}">
                    <c:url var="previousUrl" value="/AdminLoginHistory"><c:param name="page" value="${currentPage-1}"/><c:param name="eventType" value="${eventType}"/><c:param name="keyword" value="${keyword}"/></c:url>
                    <a class="pagination-link" href="${previousUrl}">Trước</a>
                </c:if>
                <span class="pagination-link active" aria-current="page">Trang ${currentPage}</span>
                <c:if test="${hasNext}">
                    <c:url var="nextUrl" value="/AdminLoginHistory"><c:param name="page" value="${currentPage+1}"/><c:param name="eventType" value="${eventType}"/><c:param name="keyword" value="${keyword}"/></c:url>
                    <a class="pagination-link" href="${nextUrl}">Sau</a>
                </c:if>
            </nav>
        </c:if>
    </section>
</main><jsp:include page="footer.jsp"/></body></html>
