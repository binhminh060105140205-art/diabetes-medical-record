<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dữ liệu thiết bị</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    <style>
        .upload-card{background:linear-gradient(135deg,#f0fdf4,#dcfce7);border:1px solid #86efac;border-radius:12px;padding:20px;margin-bottom:20px;}
        .device-badge{display:inline-block;padding:2px 9px;border-radius:10px;font-size:11px;font-weight:700;white-space:nowrap;}
        .badge-glucometer{background:#fef9c3;color:#854d0e;}
        .badge-smartwatch{background:#e0f2fe;color:#0369a1;}
        .badge-bp_monitor{background:#fce7f3;color:#9d174d;}
        .badge-scale      {background:#d1fae5;color:#065f46;}
        .val-high{color:#dc2626;font-weight:700;}
        .val-warn{color:#d97706;font-weight:700;}
        .val-ok  {color:#16a34a;}
        .json-btn{background:none;border:1px solid #e2e8f0;border-radius:5px;padding:2px 8px;font-size:11px;cursor:pointer;color:#64748b;}
        .json-panel{background:#1e293b;color:#94a3b8;font-family:monospace;font-size:11px;border-radius:6px;padding:10px;margin-top:6px;white-space:pre-wrap;word-break:break-all;display:none;}
        .alert-high  {background:#fef2f2;border-left:4px solid #dc2626;border-radius:8px;padding:10px 14px;margin-bottom:6px;}
        .alert-medium{background:#fffbeb;border-left:4px solid #d97706;border-radius:8px;padding:10px 14px;margin-bottom:6px;}
        .alert-low   {background:#eff6ff;border-left:4px solid #3b82f6;border-radius:8px;padding:10px 14px;margin-bottom:6px;}
    </style>
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <h1 class="page-title">📡 Dữ liệu từ thiết bị y tế</h1>

    <%-- Upload / Test Form --%>
    <div class="upload-card">
        <div style="font-size:16px;font-weight:800;color:#166534;margin-bottom:4px;">🔌 Upload dữ liệu thiết bị (JSON)</div>
        <p style="font-size:13px;color:#15803d;margin-bottom:14px;">
            Dán JSON từ thiết bị vào đây, hoặc kết nối thiết bị qua endpoint
            <code style="background:#fff;padding:2px 6px;border-radius:4px;">POST /api/device-data/upload</code>
        </p>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:12px;">
            <div>
                <label style="font-size:12px;font-weight:700;display:block;margin-bottom:4px;">Loại thiết bị</label>
                <select id="dev_type" class="form-control" onchange="fillExample()">
                    <option value="glucometer">Glucometer — máy đo đường huyết</option>
                    <option value="smartwatch">Smartwatch — đồng hồ thông minh</option>
                    <option value="bp_monitor">BP Monitor — máy đo huyết áp</option>
                    <option value="scale">Scale — cân thông minh</option>
                </select>
            </div>
            <div style="display:flex;align-items:flex-end;gap:8px;">
                <button class="btn btn-primary" onclick="uploadData()">📤 Gửi lên hệ thống</button>
                <button class="btn" style="background:#f1f5f9;" onclick="fillExample()">📋 Điền ví dụ</button>
            </div>
        </div>
        <label style="font-size:12px;font-weight:700;display:block;margin-bottom:4px;">JSON từ thiết bị</label>
        <textarea id="json_input" class="form-control" rows="6" style="font-family:monospace;font-size:12px;" placeholder='{"device_type":"glucometer","patient_id":1,"glucose_mgdl":145.0,"timestamp":"2026-06-28T08:30:00"}'></textarea>
        <div id="upload_result" style="margin-top:10px;font-size:13px;"></div>
    </div>

    <div style="display:grid;grid-template-columns:2fr 1fr;gap:20px;align-items:start;">

    <%-- Bảng dữ liệu thiết bị --%>
    <div class="card">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px;">
            <div class="card-title" style="margin:0;">📊 Lịch sử đọc thiết bị</div>
            <span style="font-size:12px;color:#64748b;">${recentDeviceReadings.size()} bản ghi</span>
        </div>
        <c:choose>
        <c:when test="${not empty recentDeviceReadings}">
        <div class="table-responsive-wrapper">
        <table style="font-size:12px;">
            <thead>
            <tr style="background:#f8fafc;">
                <th>Thời gian</th><th>Thiết bị</th><th>Đường huyết</th>
                <th>Nhịp tim</th><th>SpO2</th><th>Huyết áp</th>
                <th>Trạng thái</th><th>JSON</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="dr" items="${recentDeviceReadings}">
            <tr>
                <td style="white-space:nowrap;color:#64748b;">${dr.measuredAt}</td>
                <td><span class="device-badge badge-${dr.deviceType}">${dr.deviceType}</span></td>

                <td><c:choose>
                    <c:when test="${not empty dr.parsedGlucose}">
                        <c:choose>
                            <c:when test="${dr.parsedGlucose>=180||dr.parsedGlucose<70}"><span class="val-high">${dr.parsedGlucose} ⚠️</span></c:when>
                            <c:when test="${dr.parsedGlucose>=100}"><span class="val-warn">${dr.parsedGlucose}</span></c:when>
                            <c:otherwise><span class="val-ok">${dr.parsedGlucose}</span></c:otherwise>
                        </c:choose>
                        <span style="color:#94a3b8;font-size:10px;">mg/dL</span>
                    </c:when>
                    <c:otherwise><span style="color:#cbd5e1;">—</span></c:otherwise>
                </c:choose></td>

                <td><c:choose>
                    <c:when test="${not empty dr.parsedHeartRate}">
                        <c:choose>
                            <c:when test="${dr.parsedHeartRate>100||dr.parsedHeartRate<50}"><span class="val-high">${dr.parsedHeartRate} ⚠️</span></c:when>
                            <c:otherwise>${dr.parsedHeartRate}</c:otherwise>
                        </c:choose>
                        <span style="color:#94a3b8;font-size:10px;">bpm</span>
                    </c:when>
                    <c:otherwise><span style="color:#cbd5e1;">—</span></c:otherwise>
                </c:choose></td>

                <td><c:choose>
                    <c:when test="${not empty dr.parsedSpo2}">
                        <c:choose>
                            <c:when test="${dr.parsedSpo2<95}"><span class="val-high">${dr.parsedSpo2}% ⚠️</span></c:when>
                            <c:otherwise><span class="val-ok">${dr.parsedSpo2}%</span></c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:otherwise><span style="color:#cbd5e1;">—</span></c:otherwise>
                </c:choose></td>

                <td><c:choose>
                    <c:when test="${not empty dr.parsedSystolicBp}">
                        <c:choose>
                            <c:when test="${dr.parsedSystolicBp>=140}"><span class="val-high">${dr.parsedSystolicBp}/${dr.parsedDiastolicBp} ⚠️</span></c:when>
                            <c:otherwise>${dr.parsedSystolicBp}/${dr.parsedDiastolicBp}</c:otherwise>
                        </c:choose>
                        <span style="color:#94a3b8;font-size:10px;">mmHg</span>
                    </c:when>
                    <c:otherwise><span style="color:#cbd5e1;">—</span></c:otherwise>
                </c:choose></td>

                <td><c:choose>
                    <c:when test="${dr.abnormal}"><span style="color:#dc2626;font-size:11px;font-weight:700;">⚠️ Bất thường</span></c:when>
                    <c:otherwise><span style="color:#16a34a;font-size:11px;">✅ Bình thường</span></c:otherwise>
                </c:choose></td>

                <td>
                    <button class="json-btn" onclick="toggleJson(${dr.id})">{ }</button>
                    <div id="json-${dr.id}" class="json-panel">${dr.rawJsonData}</div>
                </td>
            </tr>
            </c:forEach>
            </tbody>
        </table>
        </div>
        </c:when>
        <c:otherwise>
            <p class="text-muted" style="font-size:13px;padding:8px 0;">Chưa có dữ liệu. Dùng form upload bên trên để thử nghiệm.</p>
        </c:otherwise>
        </c:choose>
    </div>

    <%-- Cột phải: Cảnh báo gần nhất --%>
    <div>
        <div class="card">
            <div class="card-title" style="font-size:14px;">🔔 Cảnh báo gần nhất</div>
            <c:choose>
            <c:when test="${not empty recentAlerts}">
                <c:forEach var="a" items="${recentAlerts}">
                <div class="${a.levelClass}" id="alert-${a.alertId}">
                    <div style="font-size:12px;font-weight:600;">${a.alertMessage}</div>
                    <div style="font-size:10px;color:#64748b;margin-top:3px;">${a.createdAt}</div>
                    <c:if test="${!a.acknowledged}">
                        <button class="btn-ack" onclick="ackAlertDev(${a.alertId})" style="background:none;border:1px solid #94a3b8;border-radius:5px;padding:2px 8px;font-size:10px;cursor:pointer;color:#64748b;margin-top:4px;">✓ Đã hiểu</button>
                    </c:if>
                </div>
                </c:forEach>
            </c:when>
            <c:otherwise><p style="font-size:13px;color:#94a3b8;">Chưa có cảnh báo nào.</p></c:otherwise>
            </c:choose>
        </div>

        <div class="card" style="margin-top:16px;">
            <div class="card-title" style="font-size:14px;">📌 Hướng dẫn kết nối</div>
            <div style="font-size:12px;color:#374151;line-height:1.8;">
                <strong>Glucometer:</strong><br>
                <code style="font-size:11px;">{device_type,patient_id,glucose_mgdl,timestamp}</code><br><br>
                <strong>Smartwatch:</strong><br>
                <code style="font-size:11px;">{device_type,patient_id,heart_rate,spo2,timestamp}</code><br><br>
                <strong>BP Monitor:</strong><br>
                <code style="font-size:11px;">{device_type,patient_id,systolic,diastolic,heart_rate,timestamp}</code><br><br>
                <strong>Scale:</strong><br>
                <code style="font-size:11px;">{device_type,patient_id,weight_kg,timestamp}</code><br><br>
                <strong>Endpoint:</strong><br>
                <code style="font-size:11px;">POST /api/device-data/upload</code>
            </div>
        </div>

        <div style="margin-top:12px;text-align:center;">
            <a href="${pageContext.request.contextPath}/PatientDashboard" class="btn btn-primary btn-sm">← Về trang chủ</a>
        </div>
    </div>
    </div><%-- end grid --%>
</div>

<script>
const CTX='${pageContext.request.contextPath}';
const PATIENT_ID=${not empty patient?patient.patientId:0};

function uploadData(){
    var txt=document.getElementById('json_input').value.trim();
    if(!txt){showResult('❌ Vui lòng nhập JSON','error');return;}
    var parsed; try{parsed=JSON.parse(txt);}catch(e){showResult('❌ JSON không hợp lệ: '+e.message,'error');return;}
    if(!parsed.patient_id&&PATIENT_ID>0){parsed.patient_id=PATIENT_ID;txt=JSON.stringify(parsed);}
    showResult('⏳ Đang gửi dữ liệu...','info');
    fetch(CTX+'/api/device-data/upload',{method:'POST',headers:{'Content-Type':'application/json;charset=UTF-8'},body:txt})
    .then(r=>r.json()).then(data=>{
        if(data.success){
            var msg='✅ Đã nhận (ID: '+data.reading_id+')';
            if(data.alerts_created>0) msg+=' — ⚠️ '+data.alerts_created+' cảnh báo mới';
            showResult(msg,'success'); setTimeout(()=>location.reload(),2000);
        } else { showResult('❌ '+(data.error||'Lỗi'),'error'); }
    }).catch(e=>showResult('❌ Lỗi kết nối','error'));
}

function fillExample(){
    var type=document.getElementById('dev_type').value;
    var now=new Date().toISOString().substring(0,19);
    var examples={
        glucometer:{device_type:'glucometer',patient_id:PATIENT_ID||1,glucose_mgdl:145.0,timestamp:now},
        smartwatch:{device_type:'smartwatch',patient_id:PATIENT_ID||1,heart_rate:88,spo2:97.5,timestamp:now},
        bp_monitor:{device_type:'bp_monitor',patient_id:PATIENT_ID||1,systolic:138,diastolic:88,heart_rate:76,timestamp:now},
        scale:{device_type:'scale',patient_id:PATIENT_ID||1,weight_kg:72.5,timestamp:now}
    };
    document.getElementById('json_input').value=JSON.stringify(examples[type]||examples.glucometer,null,2);
}

function toggleJson(id){var p=document.getElementById('json-'+id);p.style.display=p.style.display==='none'?'block':'none';}

function showResult(msg,type){
    var el=document.getElementById('upload_result');
    var c={success:'#16a34a',error:'#dc2626',info:'#0369a1'};
    el.innerHTML='<span style="color:'+(c[type]||'#000')+'">'+msg+'</span>';
}

function ackAlertDev(id){
    fetch(CTX+'/PatientHealth',{method:'POST',body:new URLSearchParams({action:'acknowledgeAlert',alertId:id})})
    .then(r=>r.json()).then(d=>{if(d.success){var el=document.getElementById('alert-'+id);if(el)el.style.opacity='0.35';}});
}
</script>
<jsp:include page="footer.jsp"/>
</body>
</html>
