<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trang Bệnh Nhân</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    <style>
        .log-inp-label{font-size:12px;font-weight:700;color:#374151;margin-bottom:4px;display:block;}
        .trend-high{color:#dc2626;font-weight:700;}
        .trend-warn{color:#d97706;font-weight:700;}
        .trend-ok  {color:#16a34a;font-weight:700;}
        .section-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px;}
        .readonly-badge{background:#f1f5f9;color:#64748b;font-size:11px;padding:3px 10px;border-radius:12px;font-weight:600;}
        .avg-bar{display:flex;gap:16px;flex-wrap:wrap;margin-bottom:12px;}
        .avg-item{background:#eff6ff;border-radius:8px;padding:10px 16px;text-align:center;border:1px solid #bfdbfe;}
        .avg-item .av{font-size:20px;font-weight:800;color:#1d4ed8;}
        .avg-item .al{font-size:11px;color:#64748b;margin-top:2px;}
        /* [NEW V3] Alert cards */
        .alert-high  {background:#fef2f2;border-left:4px solid #dc2626;border-radius:8px;padding:12px 16px;margin-bottom:8px;}
        .alert-medium{background:#fffbeb;border-left:4px solid #d97706;border-radius:8px;padding:12px 16px;margin-bottom:8px;}
        .alert-low   {background:#eff6ff;border-left:4px solid #3b82f6;border-radius:8px;padding:12px 16px;margin-bottom:8px;}
        .btn-ack{background:none;border:1px solid #94a3b8;border-radius:6px;padding:3px 10px;font-size:11px;cursor:pointer;color:#64748b;margin-top:6px;}
        .btn-ack:hover{background:#f1f5f9;}
        /* [NEW V3] Device row */
        .device-row{display:flex;gap:8px;align-items:center;padding:7px 0;border-bottom:1px solid #f1f5f9;}
        .device-badge{background:#e0f2fe;color:#0369a1;font-size:10px;font-weight:700;padding:2px 7px;border-radius:8px;white-space:nowrap;}
    </style>
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper app-workspace patient-workspace">
    <div class="workspace-heading"><div><span class="workspace-kicker">CỔNG THÔNG TIN NGƯỜI BỆNH</span><h1>Chào bạn, <c:out value="${sessionScope.user.fullName}"/></h1><p>Theo dõi chỉ số, kết quả khám và kế hoạch chăm sóc của bạn tại một nơi.</p></div><div class="heading-actions"><a class="btn btn-light" href="${pageContext.request.contextPath}/PatientHistory">Hồ sơ khám bệnh</a><a class="btn btn-primary" href="#daily-health">＋ Cập nhật chỉ số</a></div></div>

    <c:if test="${not empty msg}"><div class="alert alert-info">${msg}</div></c:if>
    <c:if test="${not empty sessionScope.flashSuccess}">
        <div class="alert alert-success">${sessionScope.flashSuccess}</div>
        <% session.removeAttribute("flashSuccess"); %>
    </c:if>

    <%-- Health overview --%>
    <div class="patient-overview">
        <div>
            <span>TỔNG QUAN SỨC KHỎE</span><div>Theo dõi đều đặn, kiểm soát chủ động</div>
            <p>Cập nhật đường huyết, huyết áp và cân nặng để tạo nhật ký liên tục cho lần tái khám.</p>
        </div>
        <a href="#daily-health" class="patient-overview-action">Mở nhật ký sức khỏe →
            <c:if test="${alertCount > 0}">
                <span style="background:#dc2626;border-radius:50%;padding:1px 6px;font-size:11px;margin-left:6px;">${alertCount}</span>
            </c:if>
        </a>
    </div>

    <%-- [NEW V3] Health Alerts --%>
    <c:if test="${not empty unacknowledgedAlerts}">
    <div class="card" style="margin-bottom:20px;">
        <div class="section-header">
            <div class="card-title" style="margin:0;">🔔 Cảnh báo sức khỏe
                <span style="background:#dc2626;color:white;font-size:11px;border-radius:10px;padding:2px 8px;margin-left:8px;">${alertCount}</span>
            </div>
        </div>
        <c:forEach var="alert" items="${unacknowledgedAlerts}">
        <div class="${alert.levelClass}" id="alert-${alert.alertId}">
            <div style="font-size:13px;font-weight:600;color:#1e293b;">${alert.alertMessage}</div>
            <div style="font-size:11px;color:#64748b;margin-top:4px;">${alert.indicatorType} • ${alert.dataSource} • ${alert.createdAt}</div>
            <button class="btn-ack" onclick="ackAlert(${alert.alertId})">✓ Đã hiểu</button>
        </div>
        </c:forEach>
    </div>
    </c:if>

    <div id="daily-health" class="patient-dashboard-grid">

    <%-- CỘT TRÁI: NHẬP CHỈ SỐ --%>
    <div>
        <div class="card">
            <div class="section-header">
                <div class="card-title" style="margin:0;">📊 Nhập chỉ số hôm nay</div>
                <c:if test="${not empty todayLog}"><span class="readonly-badge">✅ Đã nhập hôm nay</span></c:if>
            </div>
            <p class="text-muted" style="font-size:13px;margin-bottom:14px;">Đo bằng máy tại nhà và nhập chỉ số để theo dõi xu hướng sức khỏe mỗi ngày.</p>

            <div class="form-group">
                <label class="log-inp-label">🩸 Đường huyết (mg/dL) <span style="font-size:11px;color:#888;font-weight:400;">BT: 70–99</span></label>
                <input type="number" step="0.1" id="log_bg" class="form-control" value="${todayLog.bloodGlucose}" placeholder="VD: 105" oninput="hintBG(this)">
                <span id="hint_bg" style="font-size:12px;"></span>
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
                <div class="form-group">
                    <label class="log-inp-label">❤️ HA tâm thu (mmHg)</label>
                    <input type="number" id="log_sbp" class="form-control" value="${todayLog.systolicBp}" placeholder="120">
                </div>
                <div class="form-group">
                    <label class="log-inp-label">HA tâm trương (mmHg)</label>
                    <input type="number" id="log_dbp" class="form-control" value="${todayLog.diastolicBp}" placeholder="80">
                </div>
            </div>
            <div class="form-group">
                <label class="log-inp-label">⚖️ Cân nặng (kg)</label>
                <input type="number" step="0.1" id="log_weight" class="form-control" value="${todayLog.weight}" placeholder="VD: 65">
            </div>
            <div class="form-group">
                <label class="log-inp-label">🤒 Triệu chứng hôm nay</label>
                <input type="text" id="log_symptoms" class="form-control" value="${todayLog.symptoms}" placeholder="Mệt mỏi, khát nước, chóng mặt...">
            </div>
            <div class="form-group">
                <label class="log-inp-label">📝 Ghi chú thêm</label>
                <textarea id="log_note" class="form-control" style="min-height:55px;" placeholder="VD: Ăn nhiều tối qua...">${todayLog.note}</textarea>
            </div>
            <button onclick="saveLog()" class="btn btn-primary" style="width:100%;" id="saveLogBtn">💾 Lưu chỉ số hôm nay</button>
            <div id="logResult" style="margin-top:8px;font-size:13px;text-align:center;"></div>
        </div>

        <%-- Trung bình 7 ngày --%>
        <c:if test="${not empty avg7BG or not empty avg7BP}">
        <div class="card" style="margin-top:0;">
            <div class="card-title" style="font-size:14px;">📈 Trung bình 7 ngày gần nhất</div>
            <div class="avg-bar">
                <c:if test="${not empty avg7BG}"><div class="avg-item"><div class="av">${avg7BG}</div><div class="al">Đường huyết TB (mg/dL)</div></div></c:if>
                <c:if test="${not empty avg7BP}"><div class="avg-item"><div class="av">${avg7BP}</div><div class="al">HA tâm thu TB (mmHg)</div></div></c:if>
            </div>
        </div>
        </c:if>

        <%-- Lịch sử 7 ngày --%>
        <div class="card" style="margin-top:0;">
            <div class="card-title" style="font-size:14px;">📅 Nhật ký 7 ngày gần nhất</div>
            <table style="font-size:13px;">
                <tr><th>Ngày</th><th>Đường huyết</th><th>Huyết áp</th><th>Cân nặng</th></tr>
                <c:forEach var="log" items="${recentLogs}">
                <tr>
                    <td>${log.logDate}</td>
                    <td><c:choose>
                        <c:when test="${log.bloodGlucose != null}">
                            <c:choose>
                                <c:when test="${log.bloodGlucose >= 126}"><span class="trend-high">${log.bloodGlucose}</span></c:when>
                                <c:when test="${log.bloodGlucose >= 100}"><span class="trend-warn">${log.bloodGlucose}</span></c:when>
                                <c:otherwise><span class="trend-ok">${log.bloodGlucose}</span></c:otherwise>
                            </c:choose>
                        </c:when>
                        <c:otherwise><span style="color:#cbd5e1">—</span></c:otherwise>
                    </c:choose></td>
                    <td><c:choose>
                        <c:when test="${log.systolicBp != null}">
                            <c:choose>
                                <c:when test="${log.systolicBp >= 140}"><span class="trend-high">${log.systolicBp}/${log.diastolicBp}</span></c:when>
                                <c:otherwise>${log.systolicBp}/${log.diastolicBp}</c:otherwise>
                            </c:choose>
                        </c:when>
                        <c:otherwise><span style="color:#cbd5e1">—</span></c:otherwise>
                    </c:choose></td>
                    <td><c:choose><c:when test="${log.weight != null}">${log.weight} kg</c:when><c:otherwise><span style="color:#cbd5e1">—</span></c:otherwise></c:choose></td>
                </tr>
                </c:forEach>
                <c:if test="${empty recentLogs}">
                <tr><td colspan="4" style="text-align:center;color:#94a3b8;padding:16px;">Chưa có dữ liệu. Bắt đầu nhập chỉ số hằng ngày!</td></tr>
                </c:if>
            </table>
        </div>
    </div>

    <%-- CỘT PHẢI --%>
    <div>
        <%-- [NEW V3] Dữ liệu thiết bị --%>
        <div class="card">
            <div class="section-header">
                <div class="card-title" style="margin:0;">📡 Dữ liệu thiết bị gần nhất</div>
                <a href="${pageContext.request.contextPath}/DeviceData" style="font-size:12px;color:#3b82f6;">Xem tất cả →</a>
            </div>
            <c:choose>
            <c:when test="${not empty recentDeviceReadings}">
                <c:forEach var="dr" items="${recentDeviceReadings}">
                <div class="device-row">
                    <span class="device-badge">${dr.deviceType}</span>
                    <span style="font-weight:600;font-size:13px;${dr.abnormal?'color:#dc2626;':'color:#16a34a;'}">
                        <c:if test="${not empty dr.parsedGlucose}">ĐH:${dr.parsedGlucose}mg/dL</c:if>
                        <c:if test="${not empty dr.parsedHeartRate}"> NH:${dr.parsedHeartRate}bpm</c:if>
                        <c:if test="${not empty dr.parsedSpo2}"> O2:${dr.parsedSpo2}%</c:if>
                        <c:if test="${not empty dr.parsedSystolicBp}"> HA:${dr.parsedSystolicBp}/${dr.parsedDiastolicBp}</c:if>
                        ${dr.abnormal?' ⚠️':' ✅'}
                    </span>
                    <span style="color:#94a3b8;font-size:11px;margin-left:auto;">${dr.measuredAt}</span>
                </div>
                </c:forEach>
            </c:when>
            <c:otherwise><p class="text-muted" style="font-size:13px;">Chưa có dữ liệu. <a href="${pageContext.request.contextPath}/DeviceData">Kết nối thiết bị →</a></p></c:otherwise>
            </c:choose>
        </div>

        <%-- Kết quả khám gần nhất --%>
        <c:if test="${not empty latestRecord}">
        <div class="card" style="margin-top:16px;">
            <div class="section-header">
                <div class="card-title" style="margin:0;">📋 Kết quả khám gần nhất</div>
                <span class="readonly-badge">👁 Chỉ xem</span>
            </div>
            <table style="box-shadow:none;font-size:13px;">
                <tr><th style="width:140px">Ngày khám</th><td>${latestRecord.visitDate}</td></tr>
                <tr><th>Lý do</th><td>${latestRecord.reasonForVisit}</td></tr>
                <c:if test="${latestRecord.status == 'COMPLETED'}">
                <tr><th>Chẩn đoán</th><td><strong>${latestRecord.finalDiagnosis}</strong></td></tr>
                <tr><th>Đơn thuốc</th><td style="white-space:pre-wrap;">${latestRecord.prescriptionNote}</td></tr>
                <tr><th>Lời dặn</th><td>${latestRecord.advice}</td></tr>
                <tr><th>Tái khám</th><td><strong style="color:#dc3545;">${latestRecord.followUpDate}</strong></td></tr>
                </c:if>
                <c:if test="${latestRecord.status == 'DRAFT'}">
                <tr><td colspan="2"><div class="alert alert-info" style="margin:0;">⏳ Bác sĩ đang xem xét kết quả.</div></td></tr>
                </c:if>
            </table>
            <div style="margin-top:12px;">
                <a href="${pageContext.request.contextPath}/RecordDetail?id=${latestRecord.recordId}" class="btn btn-primary btn-sm">📄 Xem đầy đủ hồ sơ</a>
            </div>
        </div>
        </c:if>

        <%-- Lịch sử khám --%>
        <div class="card" style="margin-top:16px;">
            <div class="section-header">
                <div class="card-title" style="margin:0;">📁 Lịch sử khám bệnh</div>
                <span class="readonly-badge">👁 Chỉ xem</span>
            </div>
            <table style="font-size:13px;">
                <tr><th>Ngày khám</th><th>Lý do</th><th>Trạng thái</th><th></th></tr>
                <c:forEach var="r" items="${records}">
                <tr>
                    <td>${r.visitDate}</td>
                    <td style="max-width:140px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${r.reasonForVisit}</td>
                    <td><c:choose><c:when test="${r.status=='COMPLETED'}"><span style="color:green;">✅</span></c:when><c:otherwise><span style="color:orange;">⏳</span></c:otherwise></c:choose></td>
                    <td><a href="${pageContext.request.contextPath}/RecordDetail?id=${r.recordId}" class="btn btn-primary btn-sm">Xem</a></td>
                </tr>
                </c:forEach>
                <c:if test="${empty records}"><tr><td colspan="4" style="text-align:center;color:#94a3b8;padding:20px;">Chưa có lịch sử khám.</td></tr></c:if>
            </table>
        </div>

    </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/static/js/main.js"></script>
<script>
const CTX = '${pageContext.request.contextPath}';
function hintBG(el) {
    var v = parseFloat(el.value); var hint = document.getElementById('hint_bg');
    if (!hint || !v) { hint.textContent=''; return; }
    if (v<70)        hint.innerHTML='<span style="color:#dc2626">⚠ Thấp — có thể hạ đường huyết</span>';
    else if (v<=99)  hint.innerHTML='<span style="color:#16a34a">✓ Bình thường</span>';
    else if (v<=125) hint.innerHTML='<span style="color:#d97706">⚠ Tiền tiểu đường</span>';
    else             hint.innerHTML='<span style="color:#dc2626">⛔ Cao — ghi nhận và theo dõi</span>';
}
async function saveLog() {
    var btn=document.getElementById('saveLogBtn'), res=document.getElementById('logResult');
    btn.disabled=true; btn.textContent='⏳ Đang lưu...';
    var body=new URLSearchParams({action:'saveLog',
        bloodGlucose:document.getElementById('log_bg').value||'',
        systolicBp:document.getElementById('log_sbp').value||'',
        diastolicBp:document.getElementById('log_dbp').value||'',
        weight:document.getElementById('log_weight').value||'',
        symptoms:document.getElementById('log_symptoms').value||'',
        note:document.getElementById('log_note').value||''});
    try {
        var r=await fetch(CTX+'/PatientHealth',{method:'POST',body});
        var data=await r.json();
        if(data.success){
            res.innerHTML='<span style="color:#16a34a;font-weight:600;">✅ '+data.message+'</span>';
            if(data.has_alerts){
                res.innerHTML+='<br><span style="color:#dc2626;font-size:12px;">⚠️ Có cảnh báo mới!</span>';
                setTimeout(()=>location.reload(),1500);
            } else { setTimeout(()=>location.reload(),1000); }
        } else { res.innerHTML='<span style="color:#dc2626;">❌ '+(data.error||'Lỗi')+'</span>'; }
    } catch(e) { res.innerHTML='<span style="color:#dc2626;">❌ Lỗi kết nối server</span>'; }
    btn.disabled=false; btn.textContent='💾 Lưu chỉ số hôm nay';
}
// [NEW V3] Đánh dấu cảnh báo đã xem
function ackAlert(alertId) {
    fetch(CTX+'/PatientHealth',{method:'POST',body:new URLSearchParams({action:'acknowledgeAlert',alertId})})
    .then(r=>r.json()).then(data=>{
        if(data.success){var el=document.getElementById('alert-'+alertId);if(el){el.style.opacity='0.35';el.style.pointerEvents='none';}}
    });
}
</script>
<jsp:include page="footer.jsp"/>
</body>
</html>
