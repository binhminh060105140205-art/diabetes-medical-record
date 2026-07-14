<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Staff Dashboard</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <h1 class="page-title">📋 Trang Nhân Viên Tiếp Nhận</h1>

    <%-- Flash message --%>
    <c:if test="${not empty sessionScope.flashSuccess}">
        <div class="alert alert-success">${sessionScope.flashSuccess}</div>
        <% session.removeAttribute("flashSuccess"); %>
    </c:if>

    <%-- STAT CARDS --%>
    <div class="stat-cards">
        <div class="stat-card">
            <div class="stat-num">${totalPatients}</div>
            <div class="stat-label">Tổng bệnh nhân</div>
        </div>
    </div>

    <%-- THAO TÁC NHANH --%>
    <div class="card" style="border-left:4px solid #0d6efd;">
        <div class="card-title">⚡ Thao tác nhanh</div>
        <div style="display:flex;gap:12px;flex-wrap:wrap;">
            <a href="${pageContext.request.contextPath}/PatientForm"
               class="btn btn-success" style="font-size:15px;padding:12px 24px;">
                ➕ Tạo hồ sơ bệnh nhân mới
            </a>
            <a href="${pageContext.request.contextPath}/PatientList"
               class="btn btn-primary" style="font-size:15px;padding:12px 24px;">
                👥 Xem danh sách bệnh nhân (${totalPatients})
            </a>
        </div>
        <p class="text-muted" style="margin-top:12px;font-size:13px;">
            💡 Sau khi tạo hồ sơ, vào <strong>Danh sách bệnh nhân</strong>
            → bấm <strong>"Tạo bệnh án"</strong> để nhập thông tin khám ban đầu và chỉ số lâm sàng.
        </p>
    </div>

    <%-- DANH SÁCH BỆNH NHÂN GẦN ĐÂY --%>
    <div class="card">
        <div class="card-title">👥 Bệnh nhân gần đây</div>
        <table>
            <tr>
                <th>#</th><th>Họ và tên</th><th>Ngày sinh</th>
                <th>Giới tính</th><th>SĐT</th><th>Số BHYT</th><th>Thao tác</th>
            </tr>
            <c:forEach var="p" items="${recentPatients}" varStatus="s">
            <tr>
                <td>${s.count}</td>
                <td><strong>${p.fullName}</strong></td>
                <td>${p.dateOfBirth}</td>
                <td>${p.gender}</td>
                <td>${p.phone}</td>
                <td>${p.healthInsuranceNo}</td>
                <td>
                    <a href="${pageContext.request.contextPath}/PatientForm?id=${p.patientId}"
                       class="btn btn-warning btn-sm">✏️ Sửa</a>
                    <a href="${pageContext.request.contextPath}/ClinicWorkflow?view=appointments"
                       class="btn btn-primary btn-sm">Đặt lịch khám</a>
                    <a href="${pageContext.request.contextPath}/PatientHistory?patientId=${p.patientId}"
                       class="btn btn-success btn-sm">📁 Lịch sử</a>
                </td>
            </tr>
            </c:forEach>
            <c:if test="${empty recentPatients}">
            <tr>
                <td colspan="7" style="text-align:center;color:#6c757d;padding:30px;">
                    Chưa có bệnh nhân nào.
                    <a href="${pageContext.request.contextPath}/PatientForm">Tạo ngay →</a>
                </td>
            </tr>
            </c:if>
        </table>
    </div>
</div>
<script src="${pageContext.request.contextPath}/static/js/main.js"></script>
<jsp:include page="footer.jsp"/>
</body>
</html>
