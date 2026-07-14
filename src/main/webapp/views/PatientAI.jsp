<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Tư Vấn Sức Khỏe</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    <style>
        .ai-container{display:grid;grid-template-columns:1fr 370px;gap:20px;align-items:start;}
        @media(max-width:900px){.ai-container{grid-template-columns:1fr;}}
        .input-2{display:grid;grid-template-columns:1fr 1fr;gap:10px;}
        .input-3{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;}
        .field-label{font-size:12px;font-weight:700;color:#374151;margin-bottom:4px;display:block;}
        .hint{font-size:11px;margin-top:3px;}
        /* Alert cards */
        .alert-high  {background:#fef2f2;border-left:4px solid #dc2626;border-radius:8px;padding:12px 16px;margin-bottom:8px;}
        .alert-medium{background:#fffbeb;border-left:4px solid #d97706;border-radius:8px;padding:12px 16px;margin-bottom:8px;}
        .alert-low   {background:#eff6ff;border-left:4px solid #3b82f6;border-radius:8px;padding:12px 16px;margin-bottom:8px;}
        .btn-ack{background:none;border:1px solid #94a3b8;border-radius:6px;padding:2px 10px;font-size:11px;cursor:pointer;color:#64748b;margin-top:6px;}
        /* Advice box */
        .advice-box{background:linear-gradient(135deg,#0f172a,#1e3a5f);border-radius:12px;padding:20px;color:white;min-height:100px;}
        .advice-content{white-space:pre-wrap;font-size:13px;line-height:1.7;color:#e2e8f0;max-height:420px;overflow-y:auto;}
        .risk-pill{display:inline-block;padding:3px 12px;border-radius:12px;font-size:11px;font-weight:700;margin-bottom:10px;}
        .risk-low   {background:#dcfce7;color:#166534;}
        .risk-medium{background:#fef9c3;color:#854d0e;}
        .risk-high  {background:#fee2e2;color:#991b1b;}
        /* Bar chart */
        .chart-row{display:flex;gap:5px;align-items:flex-end;height:80px;}
        .chart-bar{flex:1;border-radius:3px 3px 0 0;min-width:14px;}
        .bar-ok  {background:#22c55e;}
        .bar-warn{background:#f59e0b;}
        .bar-high{background:#ef4444;}
        .bar-none{background:#e2e8f0;}
        .chart-label{font-size:9px;text-align:center;color:#94a3b8;margin-top:2px;}
        /* Device row */
        .dev-row{display:flex;align-items:center;gap:8px;padding:6px 0;border-bottom:1px solid #f1f5f9;}
        .dev-badge{background:#e0f2fe;color:#0369a1;font-size:10px;font-weight:700;padding:2px 7px;border-radius:8px;white-space:nowrap;}
        /* History rows */
        .hist-row{display:flex;align-items:center;gap:10px;padding:7px 0;border-bottom:1px solid #f1f5f9;}
        .hist-date{font-size:12px;color:#64748b;min-width:90px;}
    </style>
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <h1 class="page-title">🤖 AI Tư Vấn Sức Khỏe Hằng Ngày</h1>

    <div class="ai-container">
    <%-- ═══ CỘT TRÁI ═══ --%>
    <div>

        <%-- [NEW V3] Health Alerts chưa xem --%>
        <c:if test="${not empty unacknowledgedAlerts}">
        <div class="card" style="border:2px solid #fca5a5;margin-bottom:16px;">
            <div style="font-size:15px;font-weight:800;color:#dc2626;margin-bottom:12px;">
                🔔 ${alertCount} cảnh báo sức khỏe cần chú ý
            </div>
            <c:forEach var="a" items="${unacknowledgedAlerts}">
            <div class="${a.levelClass}" id="al-${a.alertId}">
                <div style="font-size:13px;font-weight:600;">${a.alertMessage}</div>
                <div style="font-size:11px;color:#64748b;margin-top:3px;">
                    ${a.indicatorType} &bull;
                    <c:choose><c:when test="${a.dataSource=='device'}">📡 Thiết bị</c:when><c:otherwise>✏️ Nhập tay</c:otherwise></c:choose>
                    &bull; ${a.createdAt}
                </div>
                <button class="btn-ack" onclick="ackAlert(${a.alertId})">✓ Đã hiểu</button>
            </div>
            </c:forEach>
        </div>
        </c:if>

        <%-- NHẬP CHỈ SỐ --%>
        <div class="card">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px;">
                <div style="font-size:16px;font-weight:800;">📊 Chỉ số hôm nay</div>
                <c:if test="${not empty todayLog}">
                    <span style="background:#dcfce7;color:#166534;font-size:11px;font-weight:700;padding:3px 10px;border-radius:12px;">✅ Đã nhập</span>
                </c:if>
            </div>

            <div class="input-2" style="margin-bottom:10px;">
                <div class="form-group" style="margin:0;">
                    <label class="field-label">🩸 Đường huyết (mg/dL)</label>
                    <input type="number" step="0.1" id="inp_bg" class="form-control" value="${todayLog.bloodGlucose}" placeholder="VD: 108" oninput="hintBG(this)">
                    <div id="hint_bg" class="hint"></div>
                </div>
                <div class="form-group" style="margin:0;">
                    <label class="field-label">🍽️ Thời điểm đo</label>
                    <select id="inp_meal" class="form-control">
                        <option value="">-- Chọn --</option>
                        <option value="fasting"     ${todayLog.mealType=='fasting'    ?'selected':''}>Lúc đói</option>
                        <option value="before_meal" ${todayLog.mealType=='before_meal'?'selected':''}>Trước bữa ăn</option>
                        <option value="after_meal"  ${todayLog.mealType=='after_meal' ?'selected':''}>Sau bữa ăn 2h</option>
                        <option value="bedtime"     ${todayLog.mealType=='bedtime'    ?'selected':''}>Trước ngủ</option>
                    </select>
                </div>
            </div>

            <div class="input-2" style="margin-bottom:10px;">
                <div class="form-group" style="margin:0;">
                    <label class="field-label">❤️ HA tâm thu (mmHg)</label>
                    <input type="number" id="inp_sbp" class="form-control" value="${todayLog.systolicBp}" placeholder="120">
                </div>
                <div class="form-group" style="margin:0;">
                    <label class="field-label">HA tâm trương (mmHg)</label>
                    <input type="number" id="inp_dbp" class="form-control" value="${todayLog.diastolicBp}" placeholder="80">
                </div>
            </div>

            <div class="input-3" style="margin-bottom:10px;">
                <div class="form-group" style="margin:0;">
                    <label class="field-label">⚖️ Cân nặng (kg)</label>
                    <input type="number" step="0.1" id="inp_wt" class="form-control" value="${todayLog.weight}" placeholder="60.0">
                </div>
                <div class="form-group" style="margin:0;">
                    <label class="field-label">💓 Nhịp tim (bpm)</label>
                    <input type="number" id="inp_hr" class="form-control" value="${todayLog.heartRate}" placeholder="72">
                </div>
                <div class="form-group" style="margin:0;">
                    <label class="field-label">🫁 SpO2 (%)</label>
                    <input type="number" step="0.1" id="inp_spo2" class="form-control" value="${todayLog.spo2}" placeholder="98.0">
                </div>
            </div>

            <div class="form-group">
                <label class="field-label">📋 Triệu chứng / Ghi chú</label>
                <input type="text" id="inp_sym" class="form-control" value="${todayLog.symptoms}" placeholder="Bình thường / Mệt / Chóng mặt...">
            </div>

            <button class="btn btn-primary" style="width:100%;" onclick="saveLog()">💾 Lưu chỉ số hôm nay</button>
            <div id="save_res" style="margin-top:8px;font-size:13px;text-align:center;"></div>
        </div>

        <%-- GỌI AI --%>
        <div class="card" style="margin-top:16px;">
            <div style="font-size:16px;font-weight:800;margin-bottom:4px;">✨ Nhận lời khuyên AI hôm nay</div>
            <p style="font-size:13px;color:#64748b;margin-bottom:14px;">
                AI phân tích xu hướng 7 ngày + cảnh báo → đưa ra gợi ý chăm sóc sức khỏe.
                <strong>Không thay thế ý kiến bác sĩ.</strong>
            </p>

            <c:choose>
            <c:when test="${empty todayAdvice}">
                <div class="form-group">
                    <label class="field-label">🔑 OpenAI API Key</label>
                    <div style="display:flex;gap:8px;">
                        <input type="password" id="inp_key" class="form-control" placeholder="sk-...">
                        <button type="button" onclick="toggleKey()" style="background:#f1f5f9;border:1px solid #e2e8f0;border-radius:8px;padding:0 12px;cursor:pointer;font-size:13px;white-space:nowrap;">👁</button>
                    </div>
                    <div style="font-size:11px;color:#94a3b8;margin-top:4px;">Key không lưu trên server — chỉ dùng trong phiên này.</div>
                </div>
                <button class="btn btn-primary" style="width:100%;" onclick="getAdvice()">🤖 Phân tích & Nhận lời khuyên</button>
            </c:when>
            <c:otherwise>
                <div style="background:#dcfce7;border-radius:8px;padding:10px 14px;font-size:13px;color:#166534;font-weight:600;margin-bottom:12px;">
                    ✅ Đã nhận lời khuyên hôm nay
                    <c:if test="${not empty todayAdvice.riskLevel}">
                        — <span class="risk-pill risk-${todayAdvice.riskLevel}" style="vertical-align:middle;">${todayAdvice.riskLevelIcon} ${todayAdvice.riskLevel}</span>
                    </c:if>
                </div>
                <div class="form-group">
                    <label class="field-label">🔑 Nhận lời khuyên mới (tuỳ chọn)</label>
                    <div style="display:flex;gap:8px;">
                        <input type="password" id="inp_key" class="form-control" placeholder="sk-...">
                        <button type="button" onclick="toggleKey()" style="background:#f1f5f9;border:1px solid #e2e8f0;border-radius:8px;padding:0 12px;cursor:pointer;">👁</button>
                    </div>
                </div>
                <button class="btn" style="background:#f1f5f9;width:100%;margin-bottom:8px;" onclick="getAdvice()">🔄 Cập nhật lời khuyên</button>
                <button class="btn" style="background:#f1f5f9;width:100%;" onclick="toggleAdvice()">👁 Xem lại lời khuyên hôm nay</button>
            </c:otherwise>
            </c:choose>

            <div id="advice_loading" style="display:none;margin-top:16px;">
                <div class="advice-box" style="text-align:center;padding:30px;">
                    <div style="font-size:28px;margin-bottom:8px;">🤖</div>
                    <div style="color:#93c5fd;">AI đang phân tích dữ liệu 7 ngày của bạn...</div>
                    <div style="font-size:12px;color:#64748b;margin-top:6px;">Thường mất 10–20 giây</div>
                </div>
            </div>

            <div id="advice_box" style="margin-top:16px;display:${not empty todayAdvice?'block':'none'};">
                <div class="advice-box">
                    <div id="advice_risk"><c:if test="${not empty todayAdvice}">
                        <span class="risk-pill risk-${todayAdvice.riskLevel}">${todayAdvice.riskLevelIcon} ${todayAdvice.riskLevel}</span>
                    </c:if></div>
                    <div id="advice_content" class="advice-content"><c:if test="${not empty todayAdvice}">${todayAdvice.adviceContent}</c:if></div>
                </div>
            </div>
        </div>

        <%-- Báo cáo tuần --%>
        <div class="card" style="margin-top:16px;">
            <div style="font-size:15px;font-weight:700;margin-bottom:8px;">📊 Báo cáo tổng kết tuần</div>
            <p style="font-size:13px;color:#64748b;margin-bottom:12px;">AI tóm tắt xu hướng sức khỏe 7 ngày, điểm cần cải thiện.</p>
            <div class="form-group">
                <label class="field-label">🔑 API Key (nếu chưa nhập bên trên)</label>
                <input type="password" id="inp_key2" class="form-control" placeholder="sk-...">
            </div>
            <button class="btn" style="background:#f8fafc;border:1px solid #e2e8f0;width:100%;" onclick="getWeekReport()">📋 Xem báo cáo tuần</button>
            <div id="week_report_box" style="margin-top:12px;display:none;">
                <div class="advice-box" style="background:linear-gradient(135deg,#1e3a5f,#1a4731);">
                    <div id="week_report_content" class="advice-content"></div>
                </div>
            </div>
        </div>
    </div>

    <%-- ═══ CỘT PHẢI ═══ --%>
    <div>
        <%-- Biểu đồ đường huyết 7 ngày --%>
        <div class="card">
            <div style="font-size:14px;font-weight:700;margin-bottom:12px;">📈 Đường huyết 7 ngày</div>
            <c:choose>
            <c:when test="${not empty recentLogs}">
                <div class="chart-row" id="glucose-chart"></div>
                <div style="display:flex;gap:10px;font-size:10px;margin-top:8px;flex-wrap:wrap;">
                    <span style="display:flex;align-items:center;gap:3px;"><span style="width:10px;height:10px;background:#22c55e;border-radius:2px;display:inline-block;"></span>Bình thường</span>
                    <span style="display:flex;align-items:center;gap:3px;"><span style="width:10px;height:10px;background:#f59e0b;border-radius:2px;display:inline-block;"></span>Cần chú ý</span>
                    <span style="display:flex;align-items:center;gap:3px;"><span style="width:10px;height:10px;background:#ef4444;border-radius:2px;display:inline-block;"></span>Cao</span>
                </div>
                <script>
                var chartData=[
                    <c:forEach var="log" items="${recentLogs}" varStatus="st">
                    {d:"${log.logDate}",bg:${not empty log.bloodGlucose?log.bloodGlucose:'null'},sbp:${not empty log.systolicBp?log.systolicBp:'null'}}
                    ${!st.last?',':''}
                    </c:forEach>
                ];
                </script>
            </c:when>
            <c:otherwise><p style="font-size:13px;color:#94a3b8;">Chưa có dữ liệu. Nhập chỉ số hằng ngày để xem biểu đồ.</p></c:otherwise>
            </c:choose>
        </div>

        <%-- [NEW V3] Thiết bị gần nhất --%>
        <div class="card" style="margin-top:16px;">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;">
                <div style="font-size:14px;font-weight:700;">📡 Thiết bị gần nhất</div>
                <a href="${pageContext.request.contextPath}/DeviceData" style="font-size:12px;color:#3b82f6;">Xem tất cả →</a>
            </div>
            <c:choose>
            <c:when test="${not empty recentDeviceReadings}">
                <c:forEach var="dr" items="${recentDeviceReadings}">
                <div class="dev-row">
                    <span class="dev-badge">${dr.deviceType}</span>
                    <span style="font-size:12px;font-weight:600;${dr.abnormal?'color:#dc2626':'color:#16a34a'};">
                        <c:if test="${not empty dr.parsedGlucose}">ĐH:${dr.parsedGlucose}</c:if>
                        <c:if test="${not empty dr.parsedHeartRate}"> NH:${dr.parsedHeartRate}</c:if>
                        <c:if test="${not empty dr.parsedSpo2}"> O2:${dr.parsedSpo2}%</c:if>
                        <c:if test="${not empty dr.parsedSystolicBp}"> HA:${dr.parsedSystolicBp}/${dr.parsedDiastolicBp}</c:if>
                        ${dr.abnormal?' ⚠️':' ✅'}
                    </span>
                    <span style="font-size:10px;color:#94a3b8;margin-left:auto;">${dr.measuredAt}</span>
                </div>
                </c:forEach>
            </c:when>
            <c:otherwise><p style="font-size:12px;color:#94a3b8;">Chưa có. <a href="${pageContext.request.contextPath}/DeviceData">Upload dữ liệu thiết bị →</a></p></c:otherwise>
            </c:choose>
        </div>

        <%-- Lịch sử lời khuyên AI --%>
        <c:if test="${not empty adviceHistory}">
        <div class="card" style="margin-top:16px;">
            <div style="font-size:14px;font-weight:700;margin-bottom:10px;">📚 Lịch sử lời khuyên</div>
            <c:forEach var="adv" items="${adviceHistory}">
            <div class="hist-row">
                <span class="hist-date">${adv.adviceDate}</span>
                <c:if test="${not empty adv.riskLevel}">
                    <span class="risk-pill risk-${adv.riskLevel}" style="font-size:10px;padding:2px 8px;">${adv.riskLevelIcon} ${adv.riskLevel}</span>
                </c:if>
                <span style="font-size:12px;color:#64748b;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:150px;">
                    ${adv.adviceContent}
                </span>
            </div>
            </c:forEach>
        </div>
        </c:if>

        <%-- Bệnh nhân thông tin --%>
        <c:if test="${not empty patient}">
        <div class="card" style="margin-top:16px;">
            <div style="font-size:14px;font-weight:700;margin-bottom:10px;">👤 Hồ sơ bệnh nhân</div>
            <table style="font-size:13px;box-shadow:none;">
                <tr><td style="color:#64748b;width:120px;">Họ tên</td><td><strong>${patient.fullName}</strong></td></tr>
                <tr><td style="color:#64748b;">Ngày sinh</td><td>${patient.dateOfBirth}</td></tr>
                <tr><td style="color:#64748b;">Giới tính</td><td>${patient.gender}</td></tr>
                <tr><td style="color:#64748b;">Số bảo hiểm</td><td>${patient.healthInsuranceNo}</td></tr>
            </table>
            <div style="margin-top:10px;">
                <a href="${pageContext.request.contextPath}/PatientDashboard" class="btn btn-primary btn-sm">← Về trang chủ</a>
                <a href="${pageContext.request.contextPath}/DeviceData" class="btn btn-sm" style="background:#f1f5f9;margin-left:6px;">📡 Thiết bị</a>
            </div>
        </div>
        </c:if>
    </div>
    </div><%-- end ai-container --%>
</div>

<script>
const CTX='${pageContext.request.contextPath}';

function hintBG(el){
    var v=parseFloat(el.value),hint=document.getElementById('hint_bg');
    if(!hint||isNaN(v)){if(hint)hint.innerHTML='';return;}
    if(v<70)       hint.innerHTML='<span style="color:#dc2626">🚨 Hạ đường huyết — ăn ngay 15g đường</span>';
    else if(v<=99) hint.innerHTML='<span style="color:#16a34a">✅ Bình thường lúc đói</span>';
    else if(v<=139)hint.innerHTML='<span style="color:#d97706">⚠️ Cao — kiểm soát ăn uống</span>';
    else if(v<=180)hint.innerHTML='<span style="color:#dc2626">🔴 Cao sau ăn — tăng vận động nhẹ</span>';
    else           hint.innerHTML='<span style="color:#dc2626;font-weight:700">🚨 Rất cao — liên hệ bác sĩ ngay hôm nay</span>';
}

async function saveLog(){
    var res=document.getElementById('save_res');
    res.innerHTML='<span style="color:#0369a1">⏳ Đang lưu...</span>';
    var body=new URLSearchParams({action:'saveLog'});
    var bg=document.getElementById('inp_bg').value;     if(bg)   body.append('bloodGlucose',bg);
    var sbp=document.getElementById('inp_sbp').value;   if(sbp)  body.append('systolicBp',sbp);
    var dbp=document.getElementById('inp_dbp').value;   if(dbp)  body.append('diastolicBp',dbp);
    var wt=document.getElementById('inp_wt').value;     if(wt)   body.append('weight',wt);
    var hr=document.getElementById('inp_hr').value;     if(hr)   body.append('heartRate',hr);
    var spo2=document.getElementById('inp_spo2').value; if(spo2) body.append('spo2',spo2);
    var sym=document.getElementById('inp_sym').value;   if(sym)  body.append('symptoms',sym);
    var meal=document.getElementById('inp_meal').value; if(meal) body.append('mealType',meal);
    try{
        var r=await fetch(CTX+'/PatientAI',{method:'POST',body});
        var d=await r.json();
        if(d.success){
            res.innerHTML='<span style="color:#16a34a;font-weight:600">✅ '+d.message+'</span>';
            if(d.has_alerts){res.innerHTML+='<br><span style="color:#dc2626;font-size:12px">⚠️ Có cảnh báo mới!</span>';setTimeout(()=>location.reload(),1500);}
        } else { res.innerHTML='<span style="color:#dc2626">❌ '+(d.error||'Lỗi không xác định')+'</span>'; }
    }catch(e){res.innerHTML='<span style="color:#dc2626">❌ Lỗi kết nối</span>';}
}

async function getAdvice(){
    document.getElementById('advice_box').style.display='none';
    document.getElementById('advice_loading').style.display='block';
    var body=new URLSearchParams({action:'getAdvice'});
    try{
        var r=await fetch(CTX+'/PatientAI',{method:'POST',body});
        var d=await r.json();
        document.getElementById('advice_loading').style.display='none';
        if(d.success){
            document.getElementById('advice_content').textContent=d.advice;
            var icons={low:'🟢',medium:'🟡',high:'🔴'};
            var labels={low:'Rủi ro thấp',medium:'Rủi ro trung bình',high:'Rủi ro cao'};
            var lvl=d.risk_level||'low';
            document.getElementById('advice_risk').innerHTML='<span class="risk-pill risk-'+lvl+'">'+icons[lvl]+' '+labels[lvl]+'</span>';
            document.getElementById('advice_box').style.display='block';
        } else {
            document.getElementById('advice_box').style.display='block';
            document.getElementById('advice_content').textContent='❌ '+(d.error||'Lỗi không xác định');
        }
    }catch(e){
        document.getElementById('advice_loading').style.display='none';
        document.getElementById('advice_box').style.display='block';
        document.getElementById('advice_content').textContent='❌ Lỗi kết nối';
    }
}

async function getWeekReport(){
    var box=document.getElementById('week_report_box'),cnt=document.getElementById('week_report_content');
    box.style.display='block'; cnt.textContent='⏳ AI đang tổng kết tuần...';
    var body=new URLSearchParams({action:'weekReport'});
    try{
        var r=await fetch(CTX+'/PatientAI',{method:'POST',body});
        var d=await r.json();
        cnt.textContent=d.success?d.report:('❌ '+(d.error||'Lỗi'));
    }catch(e){cnt.textContent='❌ Lỗi kết nối';}
}

function toggleAdvice(){
    var b=document.getElementById('advice_box');
    b.style.display=b.style.display==='none'?'block':'none';
}
function toggleKey(){
    var inp=document.getElementById('inp_key');
    if(inp) inp.type=inp.type==='password'?'text':'password';
}

// [NEW V3] Đánh dấu alert đã xem
function ackAlert(id){
    fetch(CTX+'/PatientAI',{method:'POST',body:new URLSearchParams({action:'acknowledgeAlert',alertId:id})})
    .then(r=>r.json()).then(d=>{if(d.success){var el=document.getElementById('al-'+id);if(el){el.style.opacity='0.35';el.style.pointerEvents='none';}}});
}

// Bar chart đường huyết
window.addEventListener('DOMContentLoaded',function(){
    if(typeof chartData==='undefined'||!chartData.length) return;
    var cont=document.getElementById('glucose-chart'); if(!cont) return;
    var maxBG=0; chartData.forEach(function(d){if(d.bg&&d.bg>maxBG)maxBG=d.bg;}); if(maxBG<200)maxBG=200;
    var rev=chartData.slice().reverse();
    rev.forEach(function(d){
        var col=document.createElement('div'); col.style.flex='1'; col.style.display='flex'; col.style.flexDirection='column'; col.style.alignItems='center';
        var bar=document.createElement('div'); var pct=d.bg?Math.max(6,Math.round((d.bg/maxBG)*100)):3;
        bar.style.width='100%'; bar.style.height=pct+'%'; bar.style.minHeight='3px'; bar.title=d.bg?(d.bg+' mg/dL'):'Không có';
        if(!d.bg) bar.className='chart-bar bar-none';
        else if(d.bg<70||d.bg>=180) bar.className='chart-bar bar-high';
        else if(d.bg>=100) bar.className='chart-bar bar-warn';
        else bar.className='chart-bar bar-ok';
        var lbl=document.createElement('div'); var pts=(d.d||'').split('-'); lbl.className='chart-label';
        lbl.textContent=pts.length>=3?pts[2]+'/'+pts[1]:d.d;
        col.appendChild(bar); col.appendChild(lbl); cont.appendChild(col);
    });
});
</script>
<jsp:include page="footer.jsp"/>
</body>
</html>
