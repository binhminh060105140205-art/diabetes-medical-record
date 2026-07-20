<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Nhật ký sức khỏe — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ui8">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<main class="page-wrapper">
    <div class="page-heading">
        <div><div class="eyebrow">THEO DÕI CÁ NHÂN</div><h1 class="page-title">Nhật ký sức khỏe</h1><p class="text-muted">Các chỉ số trong 30 ngày gần nhất, sắp theo ngày để dễ theo dõi xu hướng.</p></div>
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/PatientDashboard">Nhập chỉ số hôm nay</a>
    </div>
    <section class="card journal-summary-card">
        <div class="section-header"><div><h2>Tóm tắt chỉ số 30 ngày</h2><p>Chỉ số được tổng hợp từ nhật ký bạn đã nhập, không thay thế kết luận của bác sĩ.</p></div><span class="data-count">${measurementDays} ngày có dữ liệu</span></div>
        <div class="stats-row">
            <div class="stat-card"><strong>${empty latestLog.bloodGlucose?'—':latestLog.bloodGlucose}</strong><span>Đường huyết gần nhất</span><small>Trung bình: ${empty avgGlucose?'—':avgGlucose} mg/dL · ${glucoseTrend}</small></div>
            <div class="stat-card"><strong>${empty latestLog.systolicBp?'—':latestLog.systolicBp}/${empty latestLog.diastolicBp?'—':latestLog.diastolicBp}</strong><span>Huyết áp gần nhất</span><small>Trung bình: ${empty avgSystolic?'—':avgSystolic}/${empty avgDiastolic?'—':avgDiastolic} mmHg</small></div>
            <div class="stat-card"><strong>${empty latestLog.weight?'—':latestLog.weight}</strong><span>Cân nặng gần nhất</span><small>Trung bình: ${empty avgWeight?'—':avgWeight} kg</small></div>
            <div class="stat-card"><strong>${measurementDays}</strong><span>Ngày đã ghi nhận</span><small>Trong 30 ngày gần nhất</small></div>
        </div>
    </section>
    <section class="card">
        <div class="section-header"><div><h2>Lịch sử chỉ số</h2><p>Dữ liệu do bạn tự ghi; hãy mang theo khi tái khám nếu có bất thường kéo dài.</p></div></div>
        <div class="table-scroll">
            <table>
                <thead><tr><th>Ngày</th><th>Đường huyết</th><th>Huyết áp</th><th>Cân nặng</th><th>Triệu chứng</th><th>Ghi chú</th></tr></thead>
                <tbody>
                <c:forEach var="log" items="${logs}"><tr><td><strong>${log.logDate}</strong></td><td>${empty log.bloodGlucose?'—':log.bloodGlucose} mg/dL</td><td>${empty log.systolicBp?'—':log.systolicBp}/${empty log.diastolicBp?'—':log.diastolicBp}</td><td>${empty log.weight?'—':log.weight} kg</td><td><c:out value="${log.symptoms}" default="—"/></td><td><c:out value="${log.note}" default="—"/></td></tr></c:forEach>
                <c:if test="${empty logs}"><tr><td colspan="6" class="empty-state">Chưa có dữ liệu. Hãy nhập chỉ số tại trang Sức khỏe hôm nay.</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>
</main>
<jsp:include page="footer.jsp"/>
</body>
</html>
