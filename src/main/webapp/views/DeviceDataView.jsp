<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Dữ liệu thiết bị — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ui3">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<main class="page-wrapper">
    <div class="page-heading">
        <div><div class="eyebrow">THIẾT BỊ THEO DÕI</div><h1 class="page-title">Dữ liệu thiết bị y tế</h1><p class="text-muted">Theo dõi các chỉ số được đồng bộ từ thiết bị và ưu tiên xử lý cảnh báo bất thường.</p></div>
        <a href="${pageContext.request.contextPath}/PatientDashboard" class="btn btn-light">Về sức khỏe hôm nay</a>
    </div>

    <section class="card device-sync-note" aria-label="Trạng thái đồng bộ thiết bị">
        <span class="device-sync-icon" aria-hidden="true">↻</span>
        <div><strong>Dữ liệu được đồng bộ tự động</strong><p>Khi thiết bị đã được kết nối, chỉ số mới sẽ tự xuất hiện tại đây. Bạn không cần nhập hoặc gửi dữ liệu kỹ thuật thủ công.</p></div>
    </section>

    <div class="device-layout">
        <section class="card">
            <div class="section-header"><div><h2>Lịch sử đọc thiết bị</h2><p>Chỉ số bất thường được tô màu để ưu tiên rà soát.</p></div><span class="data-count">${fn:length(recentDeviceReadings)} bản ghi</span></div>
            <div class="operations-toolbar"><label class="table-filter"><span class="sr-only">Tìm dữ liệu thiết bị</span><input type="search" data-table-filter="deviceTable" placeholder="Tìm theo thời gian, thiết bị hoặc trạng thái"></label></div>
            <c:choose>
                <c:when test="${not empty recentDeviceReadings}">
                    <div class="table-scroll"><table id="deviceTable"><thead><tr><th>Thời gian</th><th>Thiết bị</th><th>Đường huyết</th><th>Nhịp tim</th><th>SpO2</th><th>Huyết áp</th><th>Trạng thái</th></tr></thead><tbody>
                    <c:forEach var="dr" items="${recentDeviceReadings}"><tr data-search-row>
                        <td><span class="text-muted">${dr.measuredAt}</span></td>
                        <td><span class="device-badge badge-${dr.deviceType}">${dr.deviceType=='glucometer'?'Đường huyết':dr.deviceType=='smartwatch'?'Đồng hồ':dr.deviceType=='bp_monitor'?'Huyết áp':'Cân'}</span></td>
                        <td><c:choose><c:when test="${not empty dr.parsedGlucose}"><span class="${dr.parsedGlucose>=180||dr.parsedGlucose<70?'val-high':dr.parsedGlucose>=100?'val-warn':'val-ok'}">${dr.parsedGlucose}</span><small class="value-unit">mg/dL</small></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${not empty dr.parsedHeartRate}"><span class="${dr.parsedHeartRate>100||dr.parsedHeartRate<50?'val-high':''}">${dr.parsedHeartRate}</span><small class="value-unit">bpm</small></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${not empty dr.parsedSpo2}"><span class="${dr.parsedSpo2<95?'val-high':'val-ok'}">${dr.parsedSpo2}%</span></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${not empty dr.parsedSystolicBp}"><span class="${dr.parsedSystolicBp>=140?'val-high':''}">${dr.parsedSystolicBp}/${dr.parsedDiastolicBp}</span><small class="value-unit">mmHg</small></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                        <td><span class="status-pill ${dr.abnormal?'status-CRITICAL':'status-COMPLETED'}">${dr.abnormal?'Bất thường':'Bình thường'}</span></td>
                    </tr></c:forEach>
                    <tr data-filter-empty="deviceTable" hidden><td colspan="7" class="empty-filter">Không có bản ghi phù hợp.</td></tr>
                    </tbody></table></div>
                </c:when>
                <c:otherwise><div class="empty-filter">Chưa có dữ liệu thiết bị.</div></c:otherwise>
            </c:choose>
        </section>

        <aside class="device-side">
            <section class="card">
                <div class="section-header"><div><h2>Cảnh báo gần nhất</h2><p>Xử lý mức nghiêm trọng trước.</p></div></div>
                <c:choose><c:when test="${not empty recentAlerts}"><c:forEach var="a" items="${recentAlerts}"><article class="device-alert ${a.levelClass}" id="alert-${a.alertId}"><strong><c:out value="${a.alertMessage}"/></strong><small>${a.createdAt}</small><c:if test="${!a.acknowledged}"><button type="button" class="btn btn-light btn-sm" onclick="ackAlertDev(${a.alertId})">Đánh dấu đã xem</button></c:if></article></c:forEach></c:when><c:otherwise><div class="empty-filter">Chưa có cảnh báo nào.</div></c:otherwise></c:choose>
            </section>
        </aside>
    </div>
</main>

<script>
const CTX='${pageContext.request.contextPath}';

function ackAlertDev(id){
    fetch(CTX+'/PatientHealth',{method:'POST',body:new URLSearchParams({action:'acknowledgeAlert',alertId:id})})
        .then(response=>response.json())
        .then(data=>{if(data.success){const alert=document.getElementById('alert-'+id);if(alert)alert.classList.add('acknowledged');}});
}
</script>
<jsp:include page="footer.jsp"/>
</body>
</html>
