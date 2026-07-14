<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Admin Dashboard</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    <style>
        .search-row{display:flex;gap:10px;margin-bottom:14px;align-items:center;flex-wrap:wrap;}
        .search-row input{flex:1;min-width:180px;}
        .paging-info{font-size:13px;color:#6c757d;margin-bottom:6px;}
    </style>
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">

    <h1 class="page-title">🔧 Quản Trị Hệ Thống</h1>

    <div class="stat-cards">
        <a href="${pageContext.request.contextPath}/AdminDashboard?filterRole=PATIENT"
           class="stat-card ${filterRole == 'PATIENT' ? 'active' : ''}">
            <div class="stat-num">${totalPatients}</div>
            <div class="stat-label">Bệnh nhân</div>
        </a>
        <a href="${pageContext.request.contextPath}/AdminDashboard?filterRole=DOCTOR"
           class="stat-card ${filterRole == 'DOCTOR' ? 'active' : ''}">
            <div class="stat-num">${totalDoctors}</div>
            <div class="stat-label">Bác sĩ</div>
        </a>
        <a href="${pageContext.request.contextPath}/AdminDashboard?filterRole=STAFF"
           class="stat-card ${filterRole == 'STAFF' ? 'active' : ''}">
            <div class="stat-num">${totalStaffs}</div>
            <div class="stat-label">Nhân viên</div>
        </a>
        <a href="${pageContext.request.contextPath}/AdminDashboard"
           class="stat-card ${empty filterRole ? 'active' : ''}">
            <div class="stat-num">👁️</div>
            <div class="stat-label">Tất cả</div>
        </a>
        <div class="stat-card" style="border-top-color:#198754;">
            <a href="${pageContext.request.contextPath}/AdminCreateUser" class="btn btn-primary"
               style="font-size:13px;padding:8px 14px;display:block;">
                ➕ Tạo tài khoản
            </a>
        </div>
    </div>

    <div class="card">
        <div class="card-title">👥 Danh sách tài khoản</div>

        <form action="${pageContext.request.contextPath}/AdminDashboard" method="get" class="search-row">
            <input type="hidden" name="filterRole" value="${filterRole}">
            <input type="text" name="keyword" class="form-control"
                   placeholder="🔍 Tìm tên, username, SĐT..."
                   value="${keyword}">
            <button type="submit" class="btn btn-primary">Tìm</button>
            <a href="${pageContext.request.contextPath}/AdminDashboard${not empty filterRole ? '?filterRole='.concat(filterRole) : ''}"
               class="btn btn-outline" style="background:#6c757d;color:white;">Xóa</a>
        </form>

        <div class="paging-info">
            Hiển thị trang ${currentPage}/${totalPages}
            (${totalRecords} tài khoản, mỗi trang ${pageSize} dòng)
        </div>

        <div class="table-responsive-wrapper">
            <table>
                <thead>
                    <tr>
                        <th style="width:40px">#</th>
                        <th>Username</th>
                        <th>Họ tên</th>
                        <th>SĐT</th>
                        <th style="width:90px">Role</th>
                        <th style="width:100px">Trạng thái</th>
                        <th style="width:180px">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="u" items="${allUsers}" varStatus="s">
                    <tr>
                        <td>${(currentPage - 1) * pageSize + s.count}</td>
                        <td><code>${u.username}</code></td>
                        <td>${u.fullName}</td>
                        <td>${u.phone}</td>
                        <td><span class="role-badge role-${u.role.toLowerCase()}">${u.role}</span></td>
                        <td>
                            <c:choose>
                                <c:when test="${u.status == 'ACTIVE'}">
                                    <span style="color:green;font-weight:700;">● ACTIVE</span>
                                </c:when>
                                <c:otherwise>
                                    <span style="color:red;font-weight:700;">● INACTIVE</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${u.role == 'PATIENT'}">
                                <a href="${pageContext.request.contextPath}/PatientHistory?userId=${u.userId}"
                                   class="btn btn-success btn-sm">📁 Lịch sử</a>
                            </c:if>
                            <c:if test="${u.role == 'DOCTOR'}">
                                <a href="${pageContext.request.contextPath}/AdminDoctorDetail?userId=${u.userId}"
                                   class="btn btn-success btn-sm">🪪 Hồ sơ</a>
                            </c:if>
                            <c:if test="${u.username != 'admin'}">
                                <form method="post" action="${pageContext.request.contextPath}/AdminToggleStatus" class="inline-form" onsubmit="return confirm('Xác nhận thay đổi trạng thái tài khoản?')">
                                    <input type="hidden" name="id" value="${u.userId}">
                                    <input type="hidden" name="filterRole" value="${filterRole}">
                                    <input type="hidden" name="page" value="${currentPage}">
                                    <button class="btn btn-sm ${u.status == 'ACTIVE' ? 'btn-danger' : 'btn-success'}">${u.status == 'ACTIVE' ? '🔒 Khóa' : '🔓 Mở khóa'}</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                    </c:forEach>
                    <c:if test="${empty allUsers}">
                    <tr><td colspan="7" style="text-align:center;color:#6c757d;padding:28px;">
                        Không tìm thấy tài khoản nào.
                    </td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>

        <c:if test="${totalPages > 1}">
        <div class="pagination-container">
            <c:if test="${currentPage > 1}">
                <a href="${pageContext.request.contextPath}/AdminDashboard?page=1${not empty filterRole ? '&filterRole='.concat(filterRole) : ''}${not empty keyword ? '&keyword='.concat(keyword) : ''}"
                   class="pagination-link">« Đầu</a>
                <a href="${pageContext.request.contextPath}/AdminDashboard?page=${currentPage - 1}${not empty filterRole ? '&filterRole='.concat(filterRole) : ''}${not empty keyword ? '&keyword='.concat(keyword) : ''}"
                   class="pagination-link">‹ Trước</a>
            </c:if>

            <c:forEach begin="1" end="${totalPages}" var="i">
                <c:if test="${i >= currentPage - 2 && i <= currentPage + 2}">
                    <a href="${pageContext.request.contextPath}/AdminDashboard?page=${i}${not empty filterRole ? '&filterRole='.concat(filterRole) : ''}${not empty keyword ? '&keyword='.concat(keyword) : ''}"
                       class="pagination-link ${currentPage == i ? 'active' : ''}">
                        ${i}
                    </a>
                </c:if>
            </c:forEach>

            <c:if test="${currentPage < totalPages}">
                <a href="${pageContext.request.contextPath}/AdminDashboard?page=${currentPage + 1}${not empty filterRole ? '&filterRole='.concat(filterRole) : ''}${not empty keyword ? '&keyword='.concat(keyword) : ''}"
                   class="pagination-link">Sau ›</a>
                <a href="${pageContext.request.contextPath}/AdminDashboard?page=${totalPages}${not empty filterRole ? '&filterRole='.concat(filterRole) : ''}${not empty keyword ? '&keyword='.concat(keyword) : ''}"
                   class="pagination-link">Cuối »</a>
            </c:if>
        </div>
        </c:if>
    </div>

</div>
<script src="${pageContext.request.contextPath}/static/js/main.js"></script>
<jsp:include page="footer.jsp"/>
</body>
</html>
