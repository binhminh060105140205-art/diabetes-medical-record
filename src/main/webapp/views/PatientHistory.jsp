<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Lịch Sử Khám Bệnh</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">

    <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:10px;margin-bottom:16px;">
        <div>
            <h1 class="page-title" style="margin:0;">📁 Lịch Sử Khám — ${patient.fullName}</h1>
            <p class="text-muted" style="margin-top:4px;">
                BHYT: ${patient.healthInsuranceNo} &nbsp;|&nbsp; SĐT: ${patient.phone}
                &nbsp;|&nbsp; Giới tính: ${patient.gender}
            </p>
        </div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <c:if test="${sessionScope.user.role == 'STAFF'}">
                <a href="${pageContext.request.contextPath}/MedicalRecordForm?patientId=${patient.patientId}"
                   class="btn btn-primary">➕ Tạo bệnh án mới</a>
            </c:if>
            <c:choose>
                <c:when test="${sessionScope.user.role == 'ADMIN'}">
                    <a href="${pageContext.request.contextPath}/AdminDashboard"
                       class="btn btn-outline" style="background:#6c757d;color:white;">← Quay lại Admin</a>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/PatientList"
                       class="btn btn-outline" style="background:#6c757d;color:white;">← Danh sách BN</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <div class="card">
        <div class="card-title">📋 Danh sách lần khám (${fn:length(records)} lần)</div>
        <div class="table-responsive-wrapper">
        <table>
            <thead>
                <tr>
                    <th>Mã BA</th>
                    <th>Ngày khám</th>
                    <th>Lý do khám</th>
                    <th>Trạng thái</th>
                    <th>Ngày tái khám</th>
                    <th>Thao tác</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="r" items="${records}">
            <tr>
                <td><strong>#${r.recordId}</strong></td>
                <td>${r.visitDate}</td>
                <td style="max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
                    ${r.reasonForVisit}</td>
                <td>
                    <c:choose>
                        <c:when test="${r.status == 'COMPLETED'}">
                            <span style="color:green;font-weight:700;">✅ Hoàn tất</span>
                        </c:when>
                        <c:otherwise>
                            <span style="color:orange;font-weight:700;">⏳ Đang xử lý</span>
                        </c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty r.followUpDate}">
                            <span style="color:#dc3545;font-weight:600;">${r.followUpDate}</span>
                        </c:when>
                        <c:otherwise><span style="color:#94a3b8;">—</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <a href="${pageContext.request.contextPath}/RecordDetail?id=${r.recordId}"
                       class="btn btn-primary btn-sm">📄 Xem chi tiết</a>
                    <c:if test="${sessionScope.user.role == 'DOCTOR' && r.status == 'DRAFT'}">
                        <a href="${pageContext.request.contextPath}/MedicalRecordForm?recordId=${r.recordId}&tab=4"
                           class="btn btn-warning btn-sm">✍️ Kết luận</a>
                    </c:if>
                </td>
            </tr>
            </c:forEach>
            <c:if test="${empty records}">
            <tr>
                <td colspan="6" style="text-align:center;padding:32px;color:#94a3b8;">
                    Chưa có lịch sử khám nào.
                </td>
            </tr>
            </c:if>
            </tbody>
        </table>
        </div>
    </div>

</div>
<script src="${pageContext.request.contextPath}/static/js/main.js"></script>
<jsp:include page="footer.jsp"/>
</body>
</html>
