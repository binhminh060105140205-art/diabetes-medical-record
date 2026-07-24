<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Bác sĩ — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260722-web-audit2">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<main class="page-wrapper app-workspace">
    <div class="workspace-heading">
        <div><span class="workspace-kicker">KHÔNG GIAN BÁC SĨ</span><h1>Chào buổi làm việc, <c:out value="${sessionScope.user.fullName}"/></h1></div>
        <div class="heading-actions"><a class="btn btn-primary" href="${pageContext.request.contextPath}/ClinicWorkflow?view=encounters">Mở danh sách lượt khám</a></div>
    </div>

    <section class="metric-grid doctor-metrics">
        <article class="metric-card"><span class="metric-icon blue">BN</span><div><small>BỆNH NHÂN</small><strong>${totalPatients}</strong><p>Đã được phân công</p></div></article>
        <article class="metric-card"><span class="metric-icon green">BA</span><div><small>BỆNH ÁN CỦA TÔI</small><strong>${totalMyRecords}</strong><p>Tổng hồ sơ đã phụ trách</p></div></article>
        <article class="metric-card"><span class="metric-icon blue">LH</span><div><small>LỊCH ĐƯỢC PHÂN CÔNG</small><strong>${fn:length(assignedAppointments)}</strong><p>Lịch sắp tới và hôm nay</p></div></article>
        <article class="metric-card urgent"><span class="metric-icon red">!</span><div><small>CẦN KẾT LUẬN</small><strong>${totalPending}</strong><p>Bệnh án đang chờ xử lý</p></div></article>
    </section>

    <section class="card recent-section">
        <div class="panel-heading"><div><span class="panel-eyebrow">LỊCH HẸN ĐƯỢC PHÂN CÔNG</span><h2>Bệnh nhân sắp tới</h2></div></div>
        <div class="table-scroll">
            <table class="modern-table">
                <thead><tr><th>Thời gian</th><th>Bệnh nhân</th><th>Lý do khám</th><th>Trạng thái</th><th></th></tr></thead>
                <tbody>
                <c:forEach var="a" items="${assignedAppointments}">
                    <tr>
                        <td><strong><fmt:formatDate value="${a.appointment_at}" pattern="dd/MM/yyyy HH:mm"/></strong></td>
                        <td><strong><c:out value="${a.patient_name}"/></strong><small class="table-sub"><c:out value="${a.phone}"/></small></td>
                        <td><c:out value="${a.reason}" default="Khám đái tháo đường"/></td>
                        <td><span class="status-pill status-${a.status}">${a.status=='CHECKED_IN'?'Đã tiếp nhận':'Đã xác nhận'}</span></td>
                        <td><a class="btn btn-light btn-sm" href="${pageContext.request.contextPath}/PatientHistory?patientId=${a.patient_id}">Xem hồ sơ</a></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty assignedAppointments}"><tr><td colspan="5" class="empty-state">Chưa có lịch hẹn nào được phân công.</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>

    <div class="dashboard-layout">
        <section class="card dashboard-main">
            <div class="panel-heading"><div><span class="panel-eyebrow urgent-text">ƯU TIÊN HÔM NAY</span><h2>Bệnh nhân chờ kết luận</h2></div><a href="${pageContext.request.contextPath}/ClinicWorkflow?view=encounters">Xem hàng đợi →</a></div>
            <div class="table-scroll"><table class="modern-table"><thead><tr><th>Bệnh án</th><th>Bệnh nhân</th><th>Lý do khám</th><th>Thời gian</th><th></th></tr></thead><tbody>
            <c:forEach var="r" items="${pendingRecords}"><tr><td><span class="record-code">#${r.recordId}</span></td><td><a class="patient-link" href="${pageContext.request.contextPath}/PatientHistory?patientId=${r.patientId}"><c:out value="${r.patientName}" default="Bệnh nhân #${r.patientId}"/></a></td><td><c:out value="${r.reasonForVisit}"/></td><td>${r.visitDateLabel}</td><td><div class="row-actions"><a href="${pageContext.request.contextPath}/RecordDetail?id=${r.recordId}">Xem</a><a class="primary" href="${pageContext.request.contextPath}/ClinicWorkflow?view=encounters">Tiếp tục xử lý</a></div></td></tr></c:forEach>
            <c:if test="${empty pendingRecords}"><tr><td colspan="5" class="empty-state success-empty"><strong>Không còn bệnh án chờ kết luận</strong></td></tr></c:if>
            </tbody></table></div>
        </section>

        <aside class="dashboard-side">
            <c:if test="${not empty doctor}"><section class="card doctor-identity"><div class="doctor-avatar">BS</div><h3><c:out value="${sessionScope.user.fullName}"/></h3><p><c:out value="${doctor.specialty}" default="Chưa cập nhật"/></p><dl><div><dt>Chứng chỉ</dt><dd><c:choose><c:when test="${not empty doctor.licenseNo}"><c:out value="${doctor.licenseNo}"/></c:when><c:otherwise>Chưa cập nhật</c:otherwise></c:choose></dd></div><div><dt>Học vị</dt><dd><c:choose><c:when test="${not empty doctor.degree}"><c:out value="${doctor.degree}"/></c:when><c:otherwise>Chưa cập nhật</c:otherwise></c:choose></dd></div></dl></section></c:if>
        </aside>
    </div>

    <section class="card recent-section">
        <div class="panel-heading"><div><span class="panel-eyebrow">HOẠT ĐỘNG GẦN ĐÂY</span><h2>Bệnh án đã phụ trách</h2></div></div>
        <div class="table-scroll"><table class="modern-table"><thead><tr><th>Mã bệnh án</th><th>Bệnh nhân</th><th>Ngày khám</th><th>Chẩn đoán</th><th>Trạng thái</th><th></th></tr></thead><tbody>
        <c:forEach var="r" items="${myRecords}"><tr><td><span class="record-code">#${r.recordId}</span></td><td><c:out value="${r.patientName}" default="Bệnh nhân #${r.patientId}"/></td><td>${r.visitDateLabel}</td><td><c:out value="${r.finalDiagnosis}" default="—"/></td><td><span class="status-pill status-${r.status}">${r.status=='COMPLETED'?'Đã hoàn tất':'Đang xử lý'}</span></td><td><a href="${pageContext.request.contextPath}/RecordDetail?id=${r.recordId}">Chi tiết →</a></td></tr></c:forEach>
        <c:if test="${empty myRecords}"><tr><td colspan="6" class="empty-state">Chưa có bệnh án gần đây.</td></tr></c:if>
        </tbody></table></div>
    </section>
</main>
<jsp:include page="footer.jsp"/>
</body>
</html>
