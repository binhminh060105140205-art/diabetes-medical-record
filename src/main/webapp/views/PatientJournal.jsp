<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Nhật ký sức khỏe — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ux1">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<main class="page-wrapper">
    <div class="page-heading">
        <div><div class="eyebrow">THEO DÕI CÁ NHÂN</div><h1 class="page-title">Nhật ký sức khỏe</h1><p class="text-muted">Các chỉ số trong 30 ngày gần nhất, sắp theo ngày để dễ theo dõi xu hướng.</p></div>
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/PatientDashboard">Nhập chỉ số hôm nay</a>
    </div>
    <div class="stats-row">
        <div class="stat-card"><strong>${empty avgGlucose?'—':avgGlucose}</strong><span>Đường huyết trung bình</span></div>
        <div class="stat-card"><strong>${empty avgSystolic?'—':avgSystolic}</strong><span>Huyết áp tâm thu trung bình</span></div>
    </div>
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
