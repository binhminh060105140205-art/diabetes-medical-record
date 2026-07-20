<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Quản trị hệ thống — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ui7">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>

<main class="page-wrapper app-workspace">
    <div class="workspace-heading">
        <div>
            <span class="workspace-kicker">QUẢN TRỊ HỆ THỐNG</span>
            <h1>Tài khoản và vận hành</h1>
            <p>Tra cứu người dùng, kiểm soát trạng thái tài khoản và đi nhanh đến các khu vực vận hành phòng khám.</p>
        </div>
        <div class="heading-actions">
            <a class="btn btn-light" href="${pageContext.request.contextPath}/ClinicWorkflow?view=appointments">Mở điều hành khám</a>
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/AdminCreateUser">Tạo tài khoản</a>
        </div>
    </div>

    <c:url var="patientRoleUrl" value="/AdminDashboard"><c:param name="filterRole" value="PATIENT"/><c:param name="filterStatus" value="${filterStatus}"/><c:param name="sortOrder" value="${sortOrder}"/></c:url>
    <c:url var="doctorRoleUrl" value="/AdminDashboard"><c:param name="filterRole" value="DOCTOR"/><c:param name="filterStatus" value="${filterStatus}"/><c:param name="sortOrder" value="${sortOrder}"/></c:url>
    <c:url var="staffRoleUrl" value="/AdminDashboard"><c:param name="filterRole" value="STAFF"/><c:param name="filterStatus" value="${filterStatus}"/><c:param name="sortOrder" value="${sortOrder}"/></c:url>
    <c:url var="allRoleUrl" value="/AdminDashboard"><c:param name="filterStatus" value="${filterStatus}"/><c:param name="sortOrder" value="${sortOrder}"/></c:url>
    <section class="metric-grid admin-metrics" aria-label="Thống kê tài khoản">
        <a href="${patientRoleUrl}" class="metric-card action ${filterRole=='PATIENT'?'selected':''}">
            <span class="metric-icon blue">BN</span><div><small>BỆNH NHÂN</small><strong>${totalPatients}</strong><p>Xem tài khoản bệnh nhân</p></div>
        </a>
        <a href="${doctorRoleUrl}" class="metric-card action ${filterRole=='DOCTOR'?'selected':''}">
            <span class="metric-icon green">BS</span><div><small>BÁC SĨ</small><strong>${totalDoctors}</strong><p>Xem hồ sơ bác sĩ</p></div>
        </a>
        <a href="${staffRoleUrl}" class="metric-card action ${filterRole=='STAFF'?'selected':''}">
            <span class="metric-icon amber">NV</span><div><small>NHÂN VIÊN</small><strong>${totalStaffs}</strong><p>Xem tài khoản tiếp nhận</p></div>
        </a>
        <a href="${allRoleUrl}" class="metric-card action ${empty filterRole?'selected':''}">
            <span class="metric-icon blue">Σ</span><div><small>TẤT CẢ TÀI KHOẢN</small><strong>${totalUsers}</strong><p>Bỏ bộ lọc vai trò</p></div>
        </a>
    </section>

    <section class="card">
        <div class="section-header">
            <div><h2>Danh sách tài khoản</h2><p>Tìm theo họ tên, tên đăng nhập hoặc số điện thoại. Trạng thái khóa được tách khỏi thao tác xem hồ sơ.</p></div>
            <span class="data-count">Trang ${currentPage}/${totalPages} · ${totalRecords} tài khoản</span>
        </div>

        <form action="${pageContext.request.contextPath}/AdminDashboard" method="get" class="operations-toolbar admin-filter-bar">
            <div class="admin-filter-grid">
                <label class="table-filter admin-filter-search">
                    <span class="sr-only">Tìm tài khoản</span>
                    <input type="search" name="keyword" maxlength="80" placeholder="Họ tên, tên đăng nhập hoặc số điện thoại" value="${fn:escapeXml(keyword)}">
                </label>
                <label class="admin-filter-field"><span>Vai trò</span><select class="form-control" name="filterRole">
                    <option value="">Tất cả vai trò</option>
                    <option value="ADMIN" ${filterRole=='ADMIN'?'selected':''}>Quản trị viên</option>
                    <option value="STAFF" ${filterRole=='STAFF'?'selected':''}>Nhân viên tiếp nhận</option>
                    <option value="DOCTOR" ${filterRole=='DOCTOR'?'selected':''}>Bác sĩ</option>
                    <option value="PATIENT" ${filterRole=='PATIENT'?'selected':''}>Bệnh nhân</option>
                </select></label>
                <label class="admin-filter-field"><span>Trạng thái</span><select class="form-control" name="filterStatus">
                    <option value="">Tất cả trạng thái</option>
                    <option value="ACTIVE" ${filterStatus=='ACTIVE'?'selected':''}>Đang hoạt động (Active)</option>
                    <option value="INACTIVE" ${filterStatus=='INACTIVE'?'selected':''}>Đã khóa (Inactive)</option>
                </select></label>
                <label class="admin-filter-field"><span>Ngày tạo</span><select class="form-control" name="sortOrder">
                    <option value="NEWEST" ${sortOrder=='NEWEST'?'selected':''}>Mới nhất trước</option>
                    <option value="OLDEST" ${sortOrder=='OLDEST'?'selected':''}>Cũ nhất trước</option>
                </select></label>
            </div>
            <div class="table-actions">
                <button type="submit" class="btn btn-primary">Áp dụng</button>
                <c:if test="${not empty keyword||not empty filterRole||not empty filterStatus||sortOrder=='OLDEST'}"><a href="${pageContext.request.contextPath}/AdminDashboard" class="btn btn-light">Xóa bộ lọc</a></c:if>
            </div>
        </form>

        <div class="table-responsive-wrapper">
            <table>
                <thead><tr><th>#</th><th>Tài khoản</th><th>Người dùng</th><th>Liên hệ</th><th>Vai trò</th><th>Ngày tạo</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                <tbody>
                <c:forEach var="u" items="${allUsers}" varStatus="s">
                    <tr>
                        <td>${(currentPage-1)*pageSize+s.count}</td>
                        <td><code><c:out value="${u.username}"/></code></td>
                        <td><strong><c:out value="${u.fullName}"/></strong></td>
                        <td><c:out value="${u.phone}" default="—"/></td>
                        <td><span class="role-badge role-${u.role.toLowerCase()}">${u.role=='PATIENT'?'Bệnh nhân':u.role=='DOCTOR'?'Bác sĩ':u.role=='STAFF'?'Nhân viên tiếp nhận':'Quản trị viên'}</span></td>
                        <td><time><fmt:formatDate value="${u.createdAt}" pattern="dd/MM/yyyy HH:mm"/></time></td>
                        <td><span class="status-pill status-${u.status}">${u.status=='ACTIVE'?'Đang hoạt động (Active)':'Đã khóa (Inactive)'}</span></td>
                        <td>
                            <div class="row-actions">
                                <c:if test="${u.role=='PATIENT'}"><a class="primary" href="${pageContext.request.contextPath}/PatientHistory?userId=${u.userId}">Xem hồ sơ</a></c:if>
                                <c:if test="${u.role=='DOCTOR'}"><a class="primary" href="${pageContext.request.contextPath}/AdminDoctorDetail?userId=${u.userId}">Hồ sơ bác sĩ</a></c:if>
                                <c:if test="${u.username!='admin'}">
                                    <form method="post" action="${pageContext.request.contextPath}/AdminToggleStatus" class="inline-form" onsubmit="return confirm('Xác nhận ${u.status=='ACTIVE'?'khóa':'mở khóa'} tài khoản này?')">
                                        <input type="hidden" name="id" value="${u.userId}"><input type="hidden" name="filterRole" value="${filterRole}"><input type="hidden" name="filterStatus" value="${filterStatus}"><input type="hidden" name="sortOrder" value="${sortOrder}"><input type="hidden" name="keyword" value="${fn:escapeXml(keyword)}"><input type="hidden" name="page" value="${currentPage}">
                                        <button class="btn btn-sm ${u.status=='ACTIVE'?'btn-danger':'btn-success'}" type="submit">${u.status=='ACTIVE'?'Khóa':'Mở khóa'}</button>
                                    </form>
                                </c:if>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty allUsers}"><tr><td colspan="8" class="empty-state">Không tìm thấy tài khoản phù hợp.</td></tr></c:if>
                </tbody>
            </table>
        </div>

        <c:if test="${totalPages>1}">
            <nav class="pagination-container" aria-label="Phân trang tài khoản">
                <c:if test="${currentPage>1}">
                    <c:url var="firstPageUrl" value="/AdminDashboard"><c:param name="page" value="1"/><c:param name="filterRole" value="${filterRole}"/><c:param name="filterStatus" value="${filterStatus}"/><c:param name="sortOrder" value="${sortOrder}"/><c:param name="keyword" value="${keyword}"/></c:url>
                    <c:url var="previousPageUrl" value="/AdminDashboard"><c:param name="page" value="${currentPage-1}"/><c:param name="filterRole" value="${filterRole}"/><c:param name="filterStatus" value="${filterStatus}"/><c:param name="sortOrder" value="${sortOrder}"/><c:param name="keyword" value="${keyword}"/></c:url>
                    <a href="${firstPageUrl}" class="pagination-link">Đầu</a>
                    <a href="${previousPageUrl}" class="pagination-link">Trước</a>
                </c:if>
                <c:forEach begin="1" end="${totalPages}" var="i"><c:if test="${i>=currentPage-2&&i<=currentPage+2}"><c:url var="numberPageUrl" value="/AdminDashboard"><c:param name="page" value="${i}"/><c:param name="filterRole" value="${filterRole}"/><c:param name="filterStatus" value="${filterStatus}"/><c:param name="sortOrder" value="${sortOrder}"/><c:param name="keyword" value="${keyword}"/></c:url><a href="${numberPageUrl}" class="pagination-link ${currentPage==i?'active':''}">${i}</a></c:if></c:forEach>
                <c:if test="${currentPage<totalPages}">
                    <c:url var="nextPageUrl" value="/AdminDashboard"><c:param name="page" value="${currentPage+1}"/><c:param name="filterRole" value="${filterRole}"/><c:param name="filterStatus" value="${filterStatus}"/><c:param name="sortOrder" value="${sortOrder}"/><c:param name="keyword" value="${keyword}"/></c:url>
                    <c:url var="lastPageUrl" value="/AdminDashboard"><c:param name="page" value="${totalPages}"/><c:param name="filterRole" value="${filterRole}"/><c:param name="filterStatus" value="${filterStatus}"/><c:param name="sortOrder" value="${sortOrder}"/><c:param name="keyword" value="${keyword}"/></c:url>
                    <a href="${nextPageUrl}" class="pagination-link">Sau</a>
                    <a href="${lastPageUrl}" class="pagination-link">Cuối</a>
                </c:if>
            </nav>
        </c:if>
    </section>
</main>

<jsp:include page="footer.jsp"/>
</body>
</html>
