<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Dữ liệu thiết bị — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ux1">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<main class="page-wrapper">
    <div class="page-heading">
        <div><div class="eyebrow">THIẾT BỊ THEO DÕI</div><h1 class="page-title">Dữ liệu thiết bị y tế</h1><p class="text-muted">Theo dõi bản ghi mới nhất, ưu tiên cảnh báo bất thường và chỉ mở dữ liệu gốc khi cần kiểm tra kỹ thuật.</p></div>
        <a href="${pageContext.request.contextPath}/PatientDashboard" class="btn btn-light">Về sức khỏe hôm nay</a>
    </div>

    <details class="card disclosure-card device-upload-card" ${empty recentDeviceReadings?'open':''}>
        <summary><span><strong>Gửi thử dữ liệu thiết bị</strong><small>Dùng dữ liệu mẫu hoặc gửi trực tiếp đến địa chỉ tiếp nhận của hệ thống.</small></span><span class="btn btn-light btn-sm">Mở công cụ</span></summary>
        <div class="disclosure-content">
            <div class="device-upload-grid">
                <div class="form-group"><label>Loại thiết bị</label><select id="dev_type" class="form-control" onchange="fillExample()"><option value="glucometer">Máy đo đường huyết</option><option value="smartwatch">Đồng hồ thông minh</option><option value="bp_monitor">Máy đo huyết áp</option><option value="scale">Cân thông minh</option></select></div>
                <div class="form-actions device-upload-actions"><button type="button" class="btn btn-primary" onclick="uploadData()">Gửi lên hệ thống</button><button type="button" class="btn btn-light" onclick="fillExample()">Điền dữ liệu mẫu</button></div>
            </div>
            <div class="form-group"><label>Dữ liệu kỹ thuật từ thiết bị</label><textarea id="json_input" class="form-control code-input" rows="6" placeholder='{"device_type":"glucometer","patient_id":1,"glucose_mgdl":145.0,"timestamp":"2026-06-28T08:30:00"}'></textarea><small>Địa chỉ tiếp nhận: <code>POST /api/device-data/upload</code></small></div>
            <div id="upload_result" class="device-upload-result" aria-live="polite"></div>
        </div>
    </details>

    <div class="device-layout">
        <section class="card">
            <div class="section-header"><div><h2>Lịch sử đọc thiết bị</h2><p>Chỉ số bất thường được tô màu để ưu tiên rà soát.</p></div><span class="data-count">${fn:length(recentDeviceReadings)} bản ghi</span></div>
            <div class="operations-toolbar"><label class="table-filter"><span class="sr-only">Tìm dữ liệu thiết bị</span><input type="search" data-table-filter="deviceTable" placeholder="Tìm theo thời gian, thiết bị hoặc trạng thái"></label></div>
            <c:choose>
                <c:when test="${not empty recentDeviceReadings}">
                    <div class="table-scroll"><table id="deviceTable"><thead><tr><th>Thời gian</th><th>Thiết bị</th><th>Đường huyết</th><th>Nhịp tim</th><th>SpO2</th><th>Huyết áp</th><th>Trạng thái</th><th>Dữ liệu gốc</th></tr></thead><tbody>
                    <c:forEach var="dr" items="${recentDeviceReadings}"><tr data-search-row>
                        <td><span class="text-muted">${dr.measuredAt}</span></td>
                        <td><span class="device-badge badge-${dr.deviceType}">${dr.deviceType=='glucometer'?'Đường huyết':dr.deviceType=='smartwatch'?'Đồng hồ':dr.deviceType=='bp_monitor'?'Huyết áp':'Cân'}</span></td>
                        <td><c:choose><c:when test="${not empty dr.parsedGlucose}"><span class="${dr.parsedGlucose>=180||dr.parsedGlucose<70?'val-high':dr.parsedGlucose>=100?'val-warn':'val-ok'}">${dr.parsedGlucose}</span><small class="value-unit">mg/dL</small></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${not empty dr.parsedHeartRate}"><span class="${dr.parsedHeartRate>100||dr.parsedHeartRate<50?'val-high':''}">${dr.parsedHeartRate}</span><small class="value-unit">bpm</small></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${not empty dr.parsedSpo2}"><span class="${dr.parsedSpo2<95?'val-high':'val-ok'}">${dr.parsedSpo2}%</span></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${not empty dr.parsedSystolicBp}"><span class="${dr.parsedSystolicBp>=140?'val-high':''}">${dr.parsedSystolicBp}/${dr.parsedDiastolicBp}</span><small class="value-unit">mmHg</small></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                        <td><span class="status-pill ${dr.abnormal?'status-CRITICAL':'status-COMPLETED'}">${dr.abnormal?'Bất thường':'Bình thường'}</span></td>
                        <td><details class="json-disclosure"><summary class="btn btn-light btn-sm">Xem dữ liệu gốc</summary><pre><c:out value="${dr.rawJsonData}"/></pre></details></td>
                    </tr></c:forEach>
                    <tr data-filter-empty="deviceTable" hidden><td colspan="8" class="empty-filter">Không có bản ghi phù hợp.</td></tr>
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
            <details class="card inline-disclosure device-guide"><summary class="btn btn-light">Hướng dẫn định dạng kết nối</summary><div class="device-guide-list"><strong>Máy đo đường huyết</strong><code>{device_type, patient_id, glucose_mgdl, timestamp}</code><strong>Đồng hồ thông minh</strong><code>{device_type, patient_id, heart_rate, spo2, timestamp}</code><strong>Máy đo huyết áp</strong><code>{device_type, patient_id, systolic, diastolic, heart_rate, timestamp}</code><strong>Cân thông minh</strong><code>{device_type, patient_id, weight_kg, timestamp}</code></div></details>
        </aside>
    </div>
</main>

<script>
const CTX='${pageContext.request.contextPath}';
const PATIENT_ID=${not empty patientId?patientId:0};

function uploadData(){
    const input=document.getElementById('json_input');
    let text=input.value.trim();
    if(!text){showResult('Vui lòng nhập dữ liệu kỹ thuật.','error');return;}
    let parsed;
    try{parsed=JSON.parse(text);}catch(error){showResult('Dữ liệu kỹ thuật không đúng định dạng: '+error.message,'error');return;}
    if(!parsed.patient_id&&PATIENT_ID>0){parsed.patient_id=PATIENT_ID;text=JSON.stringify(parsed);}
    showResult('Đang gửi dữ liệu...','info');
    fetch(CTX+'/api/device-data/upload',{method:'POST',headers:{'Content-Type':'application/json;charset=UTF-8'},body:text})
        .then(response=>response.json())
        .then(data=>{if(data.success){showResult('Đã nhận bản ghi #'+data.reading_id+(data.alerts_created>0?' · '+data.alerts_created+' cảnh báo mới':''),'success');setTimeout(()=>location.reload(),1500);}else{showResult(data.error||'Không thể lưu dữ liệu.','error');}})
        .catch(()=>showResult('Không thể kết nối máy chủ.','error'));
}

function fillExample(){
    const type=document.getElementById('dev_type').value;
    const timestamp=new Date().toISOString().substring(0,19);
    const examples={glucometer:{device_type:'glucometer',patient_id:PATIENT_ID||1,glucose_mgdl:145.0,timestamp},smartwatch:{device_type:'smartwatch',patient_id:PATIENT_ID||1,heart_rate:88,spo2:97.5,timestamp},bp_monitor:{device_type:'bp_monitor',patient_id:PATIENT_ID||1,systolic:138,diastolic:88,heart_rate:76,timestamp},scale:{device_type:'scale',patient_id:PATIENT_ID||1,weight_kg:72.5,timestamp}};
    document.getElementById('json_input').value=JSON.stringify(examples[type]||examples.glucometer,null,2);
}

function showResult(message,type){
    const result=document.getElementById('upload_result');
    result.textContent=message;
    result.className='device-upload-result result-'+type;
}

function ackAlertDev(id){
    fetch(CTX+'/PatientHealth',{method:'POST',body:new URLSearchParams({action:'acknowledgeAlert',alertId:id})})
        .then(response=>response.json())
        .then(data=>{if(data.success){const alert=document.getElementById('alert-'+id);if(alert)alert.classList.add('acknowledged');}});
}
</script>
<jsp:include page="footer.jsp"/>
</body>
</html>
