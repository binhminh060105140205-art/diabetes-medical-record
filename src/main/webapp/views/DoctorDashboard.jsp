<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Doctor Dashboard</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    <style>
        /* [V3] Đã xóa hoàn toàn: ai-panel, risk-badge, warning-card styles */
        .status-badge{display:inline-block;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:700;}
        .badge-done{background:#dcfce7;color:#166534;}
        .badge-draft{background:#fef9c3;color:#854d0e;}
    </style>
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <h1 class="page-title">🩺 Trang Bác Sĩ — ${sessionScope.user.fullName}</h1>

    <%-- THÔNG TIN CÁ NHÂN BÁC SĨ (đổ ra từ dữ liệu do Admin tạo / bác sĩ tự cập nhật) --%>
    <c:if test="${not empty doctor}">
    <div class="card" style="display:flex;gap:20px;align-items:center;flex-wrap:wrap;">
        <c:choose>
            <c:when test="${not empty doctor.faceImagePath}">
                <img src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=face"
                     style="width:80px;height:80px;border-radius:50%;object-fit:cover;">
            </c:when>
            <c:otherwise>
                <div style="width:80px;height:80px;border-radius:50%;background:#f1f3f5;display:flex;align-items:center;justify-content:center;font-size:32px;">🩺</div>
            </c:otherwise>
        </c:choose>
        <div style="flex:1;min-width:200px;">
            <div style="font-size:18px;font-weight:700;">${sessionScope.user.fullName}</div>
            <div style="font-size:13px;color:#495057;margin-top:4px;">
                <c:if test="${not empty doctor.specialty}">Chuyên khoa: <strong>${doctor.specialty}</strong> &nbsp;·&nbsp; </c:if>
                <c:if test="${not empty doctor.degree}">${doctor.degree}</c:if>
            </div>
            <div style="font-size:13px;color:#6c757d;margin-top:2px;">
                <c:if test="${not empty doctor.licenseNo}">Số chứng chỉ hành nghề: ${doctor.licenseNo}</c:if>
            </div>
        </div>
        <a href="${pageContext.request.contextPath}/DoctorProfile" class="btn btn-primary btn-sm">🪪 Xem / Cập nhật hồ sơ minh chứng</a>
    </div>
    </c:if>

    <%-- STAT CARDS — [V3] không có AI warning stat --%>
    <div class="stat-cards">
        <div class="stat-card">
            <div class="stat-num">${totalPatients}</div>
            <div class="stat-label">Tổng bệnh nhân</div>
        </div>
        <div class="stat-card" style="border-top-color:#198754;">
            <div class="stat-num" style="color:#198754;">${totalMyRecords}</div>
            <div class="stat-label">Bệnh án của tôi</div>
        </div>
        <div class="stat-card" style="border-top-color:#dc3545;">
            <div class="stat-num" style="color:#dc3545;">${totalPending}</div>
            <div class="stat-label">Chờ kết luận</div>
        </div>
        <div class="stat-card" style="border-top-color:#0dcaf0;">
            <a href="${pageContext.request.contextPath}/PatientList" style="text-decoration:none;color:inherit;display:block;">
                <div class="stat-num" style="color:#0891b2;font-size:22px;">📋</div>
                <div class="stat-label">Danh sách BN</div>
            </a>
        </div>
    </div>

    <%-- BỆNH ÁN CHỜ KẾT LUẬN --%>
    <div class="card" style="border-left:4px solid #dc3545;">
        <div class="card-title" style="color:#dc3545;">
            🚨 Bệnh nhân chờ kết luận
            <span style="font-size:13px;font-weight:400;color:#6c757d;margin-left:8px;">(${totalPending} bệnh án DRAFT)</span>
        </div>
        <c:choose>
        <c:when test="${not empty pendingRecords}">
            <table>
                <thead><tr><th>Mã BA</th><th>Bệnh nhân</th><th>Ngày tạo</th><th>Lý do khám</th><th>Thao tác</th></tr></thead>
                <tbody>
                <c:forEach var="r" items="${pendingRecords}">
                <tr>
                    <td><strong>#${r.recordId}</strong></td>
                    <td><a href="${pageContext.request.contextPath}/PatientHistory?patientId=${r.patientId}" style="font-weight:600;">BN #${r.patientId}</a></td>
                    <td style="font-size:12px;color:#64748b;">${r.visitDate}</td>
                    <td style="max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${r.reasonForVisit}</td>
                    <td>
                        <a href="${pageContext.request.contextPath}/MedicalRecordForm?recordId=${r.recordId}&tab=4" class="btn btn-success btn-sm">✍️ Kết luận</a>
                        <a href="${pageContext.request.contextPath}/RecordDetail?id=${r.recordId}" class="btn btn-primary btn-sm">Xem</a>
                    </td>
                </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:when>
        <c:otherwise><p class="text-muted" style="font-size:13px;padding:8px 0;">✅ Tất cả bệnh án đã có kết luận.</p></c:otherwise>
        </c:choose>
    </div>

    <%-- BỆNH ÁN GẦN ĐÂY --%>
    <div class="card" style="margin-top:20px;">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px;">
            <div class="card-title" style="margin:0;">📋 Bệnh án gần đây (5 gần nhất)</div>
            <a href="${pageContext.request.contextPath}/PatientList" class="btn btn-primary btn-sm">Tất cả bệnh nhân</a>
        </div>
        <c:choose>
        <c:when test="${not empty myRecords}">
            <table>
                <thead><tr><th>Mã BA</th><th>BN</th><th>Ngày khám</th><th>Chẩn đoán</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                <tbody>
                <c:forEach var="r" items="${myRecords}">
                <tr>
                    <td>#${r.recordId}</td>
                    <td><a href="${pageContext.request.contextPath}/PatientHistory?patientId=${r.patientId}">BN #${r.patientId}</a></td>
                    <td style="font-size:12px;white-space:nowrap;">${r.visitDate}</td>
                    <td style="max-width:180px;font-size:13px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${r.finalDiagnosis}</td>
                    <td>
                        <c:choose>
                            <c:when test="${r.status=='Hoàn thành'}"><span class="status-badge badge-done">✅ ${r.status}</span></c:when>
                            <c:otherwise><span class="status-badge badge-draft">⏳ ${r.status}</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td><a href="${pageContext.request.contextPath}/RecordDetail?id=${r.recordId}" class="btn btn-primary btn-sm">Chi tiết</a></td>
                </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:when>
        <c:otherwise><p class="text-muted" style="font-size:13px;">Chưa có bệnh án nào.</p></c:otherwise>
        </c:choose>
    </div>

    <%--
        [V3] ĐÃ XÓA HOÀN TOÀN:
        - Panel "Cảnh báo AI" / AIWarnings
        - Panel "Phân tích rủi ro AI"
        - Bất kỳ thành phần nào liên quan Doctor AI

        Lý do: Doctor AI bị loại bỏ theo yêu cầu thiết kế V3.
        Mọi AI chỉ hoạt động ở phía bệnh nhân (PatientAI).
    --%>

</div>
<jsp:include page="footer.jsp"/>
</body>
</html>
