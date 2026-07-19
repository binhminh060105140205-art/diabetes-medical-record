<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Sức khỏe hôm nay — DiaCare</title><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260719-ai1"></head>
<body><jsp:include page="header.jsp"/><jsp:include page="topnav.jsp"/>
<main class="page-wrapper"><div class="page-heading"><div><div class="eyebrow">THEO DÕI HẰNG NGÀY</div><h1 class="page-title">Sức khỏe hôm nay</h1><p class="text-muted">Nhập một lần mỗi ngày. Đây là nhật ký theo dõi, không tự chẩn đoán hay đổi thuốc.</p></div><a class="btn btn-outline-dark" href="${pageContext.request.contextPath}/PatientJournal">Xem nhật ký 30 ngày</a></div>
<c:if test="${not empty msg}"><div class="alert alert-info"><c:out value="${msg}"/></div></c:if>
<c:if test="${not empty diabetesProfile}">
<section class="patient-overview">
    <div>
        <span>HỒ SƠ TIỂU ĐƯỜNG DO BÁC SĨ XÁC NHẬN</span>
        <div>${diabetesProfile.diabetesTypeLabel}</div>
        <p>Điều trị hiện tại: ${diabetesProfile.treatmentMethodLabel}<c:if test="${not empty diabetesProfile.diagnosisDate}"> · Phát hiện: ${diabetesProfile.diagnosisDate}</c:if><c:if test="${not empty diabetesProfile.hba1cTarget}"> · Mục tiêu HbA1c: ${diabetesProfile.hba1cTarget}%</c:if></p>
        <c:choose>
            <c:when test="${diabetesProfile.diabetesType=='TYPE_1'}"><p>Ưu tiên theo dõi đường huyết và dùng insulin đúng đơn bác sĩ.</p></c:when>
            <c:when test="${diabetesProfile.diabetesType=='TYPE_2'}"><p>Ưu tiên theo dõi đường huyết, cân nặng, huyết áp và dùng thuốc đúng đơn.</p></c:when>
            <c:otherwise><p>Bác sĩ sẽ xác nhận loại tiểu đường trong lần khám phù hợp.</p></c:otherwise>
        </c:choose>
    </div>
    <a class="patient-overview-action" href="${pageContext.request.contextPath}/PatientHistory">Xem hồ sơ khám</a>
</section>
</c:if>
<c:if test="${not empty patient}">
<section class="card ai-advice-card" id="daily-advice">
    <div class="ai-advice-heading">
        <div><div class="eyebrow">TRỢ LÝ SỨC KHỎE</div><h2>Lời khuyên dành cho hôm nay</h2>
        <p>Hệ thống phân tích dữ liệu hợp lệ đã được ẩn danh. Kết quả chỉ hỗ trợ theo dõi, không thay thế bác sĩ.</p></div>
        <span class="ai-advice-icon" aria-hidden="true">✦</span>
    </div>
    <label class="ai-consent"><input type="checkbox" id="aiConsent"> <span>Tôi đồng ý gửi nhóm tuổi, loại tiểu đường, chỉ số tóm tắt 7 ngày, triệu chứng chuẩn hóa và cờ bệnh nền. Không gửi tên, ngày sinh đầy đủ, số điện thoại, địa chỉ, CCCD, BHYT, tài khoản hoặc tên/liều thuốc.</span></label>
    <div class="ai-advice-actions"><button type="button" class="btn btn-primary" id="aiAdviceButton" onclick="loadPatientAdvice()">Nhận lời khuyên</button><span class="form-hint">Chỉ gọi khi bạn bấm nút; kết quả giống nhau trong ngày được dùng lại để tải nhanh hơn.</span></div>
    <div class="ai-advice-result" id="aiAdviceResult" hidden aria-live="polite">
        <div class="ai-advice-result-head"><strong id="aiAdviceSummary"></strong><span id="aiAdviceSeverity" class="ai-severity"></span></div>
        <ol id="aiAdviceItems"></ol><p id="aiAdviceDoctor" class="ai-doctor-note" hidden></p><small id="aiAdviceSource"></small>
    </div>
</section>
</c:if>
<c:if test="${not empty patient}"><div class="two-column"><section class="card"><div class="card-title">Nhập chỉ số hôm nay <c:if test="${not empty todayLog}"><span class="status-pill status-COMPLETED">Đã nhập</span></c:if></div>
<div class="form-group"><label>Thời điểm đo đường huyết</label><select id="log_meal" class="form-control"><option value="">Chọn thời điểm</option><option value="FASTING" ${todayLog.mealType=='FASTING'?'selected':''}>Lúc đói</option><option value="AFTER_MEAL" ${todayLog.mealType=='AFTER_MEAL'?'selected':''}>Sau ăn</option><option value="BEDTIME" ${todayLog.mealType=='BEDTIME'?'selected':''}>Trước khi ngủ</option><option value="OTHER" ${todayLog.mealType=='OTHER'?'selected':''}>Khác</option></select></div>
<div class="form-group"><label>Đường huyết (mg/dL)</label><input type="number" min="20" max="600" step="0.1" id="log_bg" class="form-control" value="${todayLog.bloodGlucose}" placeholder="Ví dụ: 105"></div>
<div class="two-column"><div class="form-group"><label>Huyết áp tâm thu</label><input type="number" min="60" max="260" id="log_sbp" class="form-control" value="${todayLog.systolicBp}" placeholder="120"></div><div class="form-group"><label>Huyết áp tâm trương</label><input type="number" min="30" max="180" id="log_dbp" class="form-control" value="${todayLog.diastolicBp}" placeholder="80"></div></div>
<div class="form-group"><label>Cân nặng (kg)</label><input type="number" min="20" max="300" step="0.1" id="log_weight" class="form-control" value="${todayLog.weight}" placeholder="65"></div>
<div class="form-group"><label>Triệu chứng hôm nay</label><div class="checkbox-grid"><label><input type="checkbox" name="symptom" value="Không có triệu chứng"> Không có</label><label><input type="checkbox" name="symptom" value="Mệt mỏi"> Mệt mỏi</label><label><input type="checkbox" name="symptom" value="Khát nhiều"> Khát nhiều</label><label><input type="checkbox" name="symptom" value="Chóng mặt"> Chóng mặt</label><label><input type="checkbox" name="symptom" value="Run tay/vã mồ hôi"> Run tay/vã mồ hôi</label></div></div>
<div class="form-group"><label>Ghi chú</label><textarea id="log_note" class="form-control" maxlength="1000" placeholder="Thông tin khác muốn bác sĩ biết"><c:out value="${todayLog.note}"/></textarea></div>
<button type="button" onclick="saveLog()" class="btn btn-primary" id="saveLogBtn">Lưu chỉ số hôm nay</button><div id="logResult" class="form-hint"></div></section>
<section class="card"><div class="card-title">Truy cập nhanh</div><div class="quick-actions"><a class="btn btn-primary" href="${pageContext.request.contextPath}/PatientAppointments">Đặt lịch khám</a><a class="btn btn-outline-dark" href="${pageContext.request.contextPath}/PatientJournal">Nhật ký sức khỏe</a><a class="btn btn-outline-dark" href="${pageContext.request.contextPath}/PatientHistory">Hồ sơ khám</a></div><c:if test="${not empty latestRecord}"><hr><h3>Bệnh án gần nhất</h3><p>Ngày khám: ${latestRecord.visitDate}</p><p>Chẩn đoán: <c:out value="${latestRecord.finalDiagnosis}" default="Chưa kết luận"/></p></c:if></section></div></c:if>
</main><span id="savedSymptoms" hidden><c:out value="${todayLog.symptoms}"/></span><jsp:include page="footer.jsp"/><script src="${pageContext.request.contextPath}/static/js/main.js?v=20260719-ai1"></script>
<script>
const CTX='${pageContext.request.contextPath}',savedSymptoms=document.getElementById('savedSymptoms').textContent;
document.querySelectorAll('[name=symptom]').forEach(x=>x.checked=savedSymptoms.includes(x.value));
async function saveLog(){const b=document.getElementById('saveLogBtn'),r=document.getElementById('logResult'),symptoms=[...document.querySelectorAll('[name=symptom]:checked')].map(x=>x.value).join(', ');b.disabled=true;r.textContent='Đang lưu...';try{const body=new URLSearchParams({action:'saveLog',mealType:log_meal.value||'',bloodGlucose:log_bg.value||'',systolicBp:log_sbp.value||'',diastolicBp:log_dbp.value||'',weight:log_weight.value||'',symptoms,note:log_note.value||''});const res=await fetch(CTX+'/PatientHealth',{method:'POST',body});const data=await res.json();r.textContent=data.success?'Đã lưu chỉ số hôm nay.':(data.error||'Không thể lưu dữ liệu.');}catch(e){r.textContent='Không thể kết nối máy chủ.'}finally{b.disabled=false}}
async function loadPatientAdvice(){
    const consent=document.getElementById('aiConsent'),button=document.getElementById('aiAdviceButton'),box=document.getElementById('aiAdviceResult'),summary=document.getElementById('aiAdviceSummary'),items=document.getElementById('aiAdviceItems');
    if(!consent.checked){box.hidden=false;summary.textContent='Vui lòng đọc và đánh dấu đồng ý trước khi tiếp tục.';items.replaceChildren();return;}
    button.disabled=true;button.textContent='Đang phân tích...';box.hidden=false;summary.textContent='Đang tạo lời khuyên an toàn cho hôm nay...';items.replaceChildren();
    try{
        const response=await fetch(CTX+'/api/patient/ai-advice',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({consent:true})});
        const data=await response.json();if(!response.ok)throw new Error(data.error||'Không thể tạo lời khuyên.');
        summary.textContent=data.summary;const severity=document.getElementById('aiAdviceSeverity');severity.textContent=data.severity==='high'?'Nên liên hệ bác sĩ':data.severity==='medium'?'Cần chú ý':'Ổn định';severity.className='ai-severity ai-severity-'+data.severity;
        items.replaceChildren(...data.advice.map(value=>{const li=document.createElement('li');li.textContent=value;return li;}));
        const doctor=document.getElementById('aiAdviceDoctor');doctor.hidden=!data.doctorRecommendation;doctor.textContent=data.doctorRecommendation?'Nếu cảm thấy không khỏe hoặc triệu chứng tiếp diễn, hãy liên hệ bác sĩ/phòng khám.':'';
        document.getElementById('aiAdviceSource').textContent=(data.source==='OPENAI'?'Lời khuyên được hỗ trợ bởi AI':'Lời khuyên an toàn từ bộ quy tắc nội bộ')+(data.cached?' · đã lưu trong ngày':'');
    }catch(error){summary.textContent=error.message;items.replaceChildren();}
    finally{button.disabled=false;button.textContent='Nhận lời khuyên';}
}
</script></body></html>
