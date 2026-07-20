<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Quản trị hệ thống — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ui3">
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

    <section class="metric-grid admin-metrics" aria-label="Thống kê tài khoản">
        <a href="${pageContext.request.contextPath}/AdminDashboard?filterRole=PATIENT" class="metric-card action ${filterRole=='PATIENT'?'selected':''}">
            <span class="metric-icon blue">BN</span><div><small>BỆNH NHÂN</small><strong>${totalPatients}</strong><p>Xem tài khoản bệnh nhân</p></div>
        </a>
        <a href="${pageContext.request.contextPath}/AdminDashboard?filterRole=DOCTOR" class="metric-card action ${filterRole=='DOCTOR'?'selected':''}">
            <span class="metric-icon green">BS</span><div><small>BÁC SĨ</small><strong>${totalDoctors}</strong><p>Xem hồ sơ bác sĩ</p></div>
        </a>
        <a href="${pageContext.request.contextPath}/AdminDashboard?filterRole=STAFF" class="metric-card action ${filterRole=='STAFF'?'selected':''}">
            <span class="metric-icon amber">NV</span><div><small>NHÂN VIÊN</small><strong>${totalStaffs}</strong><p>Xem tài khoản tiếp nhận</p></div>
        </a>
        <a href="${pageContext.request.contextPath}/AdminDashboard" class="metric-card action ${empty filterRole?'selected':''}">
            <span class="metric-icon blue">Σ</span><div><small>TẤT CẢ TÀI KHOẢN</small><strong>${totalRecords}</strong><p>Bỏ bộ lọc vai trò</p></div>
        </a>
    </section>

    <section class="card">
        <div class="section-header">
            <div><h2>Danh sách tài khoản</h2><p>Tìm theo họ tên, tên đăng nhập hoặc số điện thoại. Trạng thái khóa được tách khỏi thao tác xem hồ sơ.</p></div>
            <span class="data-count">Trang ${currentPage}/${totalPages} · ${totalRecords} tài khoản</span>
        </div>

        <form action="${pageContext.request.contextPath}/AdminDashboard" method="get" class="operations-toolbar admin-search">
            <input type="hidden" name="filterRole" value="${filterRole}">
            <label class="table-filter">
                <span class="sr-only">Tìm tài khoản</span>
                <input type="search" name="keyword" maxlength="80" placeholder="Họ tên, tên đăng nhập hoặc số điện thoại" value="${fn:escapeXml(keyword)}">
            </label>
            <div class="table-actions">
                <button type="submit" class="btn btn-primary">Tìm kiếm</button>
                <c:if test="${not empty keyword}"><a href="${pageContext.request.contextPath}/AdminDashboard${not empty filterRole?'?filterRole='.concat(filterRole):''}" class="btn btn-light">Xóa từ khóa</a></c:if>
            </div>
        </form>

        <div class="table-responsive-wrapper">
            <table>
                <thead><tr><th>#</th><th>Tài khoản</th><th>Người dùng</th><th>Liên hệ</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                <tbody>
                <c:forEach var="u" items="${allUsers}" varStatus="s">
                    <tr>
                        <td>${(currentPage-1)*pageSize+s.count}</td>
                        <td><code><c:out value="${u.username}"/></code></td>
                        <td><strong><c:out value="${u.fullName}"/></strong></td>
                        <td><c:out value="${u.phone}" default="—"/></td>
                        <td><span class="role-badge role-${u.role.toLowerCase()}">${u.role=='PATIENT'?'Bệnh nhân':u.role=='DOCTOR'?'Bác sĩ':u.role=='STAFF'?'Nhân viên tiếp nhận':'Quản trị viên'}</span></td>
                        <td><span class="status-pill status-${u.status}">${u.status=='ACTIVE'?'Đang hoạt động':'Đã khóa'}</span></td>
                        <td>
                            <div class="row-actions">
                                <c:if test="${u.role=='PATIENT'}"><a class="primary" href="${pageContext.request.contextPath}/PatientHistory?userId=${u.userId}">Xem hồ sơ</a></c:if>
                                <c:if test="${u.role=='DOCTOR'}"><a class="primary" href="${pageContext.request.contextPath}/AdminDoctorDetail?userId=${u.userId}">Hồ sơ bác sĩ</a></c:if>
                                <c:if test="${u.username!='admin'}">
                                    <form method="post" action="${pageContext.request.contextPath}/AdminToggleStatus" class="inline-form" onsubmit="return confirm('Xác nhận ${u.status=='ACTIVE'?'khóa':'mở khóa'} tài khoản này?')">
                                        <input type="hidden" name="id" value="${u.userId}"><input type="hidden" name="filterRole" value="${filterRole}"><input type="hidden" name="page" value="${currentPage}">
                                        <button class="btn btn-sm ${u.status=='ACTIVE'?'btn-danger':'btn-success'}" type="submit">${u.status=='ACTIVE'?'Khóa':'Mở khóa'}</button>
                                    </form>
                                </c:if>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty allUsers}"><tr><td colspan="7" class="empty-state">Không tìm thấy tài khoản phù hợp.</td></tr></c:if>
                </tbody>
            </table>
        </div>

        <c:if test="${totalPages>1}">
            <nav class="pagination-container" aria-label="Phân trang tài khoản">
                <c:if test="${currentPage>1}">
                    <a href="${pageContext.request.contextPath}/AdminDashboard?page=1${not empty filterRole?'&filterRole='.concat(filterRole):''}${not empty keyword?'&keyword='.concat(keyword):''}" class="pagination-link">Đầu</a>
                    <a href="${pageContext.request.contextPath}/AdminDashboard?page=${currentPage-1}${not empty filterRole?'&filterRole='.concat(filterRole):''}${not empty keyword?'&keyword='.concat(keyword):''}" class="pagination-link">Trước</a>
                </c:if>
                <c:forEach begin="1" end="${totalPages}" var="i"><c:if test="${i>=currentPage-2&&i<=currentPage+2}"><a href="${pageContext.request.contextPath}/AdminDashboard?page=${i}${not empty filterRole?'&filterRole='.concat(filterRole):''}${not empty keyword?'&keyword='.concat(keyword):''}" class="pagination-link ${currentPage==i?'active':''}">${i}</a></c:if></c:forEach>
                <c:if test="${currentPage<totalPages}">
                    <a href="${pageContext.request.contextPath}/AdminDashboard?page=${currentPage+1}${not empty filterRole?'&filterRole='.concat(filterRole):''}${not empty keyword?'&keyword='.concat(keyword):''}" class="pagination-link">Sau</a>
                    <a href="${pageContext.request.contextPath}/AdminDashboard?page=${totalPages}${not empty filterRole?'&filterRole='.concat(filterRole):''}${not empty keyword?'&keyword='.concat(keyword):''}" class="pagination-link">Cuối</a>
                </c:if>
            </nav>
        </c:if>
    </section>
</main>

<jsp:include page="footer.jsp"/>
</body>
</html>
