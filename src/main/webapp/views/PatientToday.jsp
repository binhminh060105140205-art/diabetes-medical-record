<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Sức khỏe hôm nay — DiaCare</title><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260721-web-audit1"><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/patient-care-path.css?v=20260721-patient1"></head>
<body><jsp:include page="header.jsp"/><jsp:include page="topnav.jsp"/>
<main class="page-wrapper patient-today-page"><div class="page-heading"><div><div class="eyebrow">THEO DÕI HẰNG NGÀY</div><h1 class="page-title">Sức khỏe hôm nay</h1></div><a class="btn btn-light" href="${pageContext.request.contextPath}/PatientJournal">Nhật ký 30 ngày</a></div>
<c:if test="${not empty msg}"><div class="alert alert-info"><c:out value="${msg}"/></div></c:if>
<c:if test="${not empty diabetesProfile}">
<section class="patient-overview">
    <div>
        <span>HỒ SƠ ĐÁI THÁO ĐƯỜNG DO BÁC SĨ XÁC NHẬN</span>
        <div>${diabetesProfile.diabetesTypeLabel}</div>
        <p>Điều trị hiện tại: ${diabetesProfile.treatmentMethodLabel}<c:if test="${not empty diabetesProfile.diagnosisDate}"> · Phát hiện: ${diabetesProfile.diagnosisDate}</c:if><c:if test="${not empty diabetesProfile.hba1cTarget}"> · Mục tiêu HbA1c: ${diabetesProfile.hba1cTarget}%</c:if></p>
    </div>
</section>
<jsp:include page="diabetesCarePath.jsp"/>
</c:if>
<c:if test="${not empty patient}">
<section class="card ai-advice-card" id="daily-advice">
    <div class="ai-advice-heading">
        <div><div class="eyebrow">TRỢ LÝ SỨC KHỎE</div><h2>Lời khuyên hôm nay</h2></div>
        <span class="ai-advice-icon" aria-hidden="true">✦</span>
    </div>
    <label class="ai-consent"><input type="checkbox" id="aiConsent"> <span>Tôi đồng ý dùng dữ liệu sức khỏe đã ẩn danh để tạo lời khuyên.</span></label>
    <div class="ai-advice-actions"><button type="button" class="btn btn-primary" id="aiAdviceButton" onclick="loadPatientAdvice()">Nhận lời khuyên</button></div>
    <div class="ai-advice-result" id="aiAdviceResult" hidden aria-live="polite">
        <div class="ai-advice-result-head"><strong id="aiAdviceSummary"></strong><span id="aiAdviceSeverity" class="ai-severity"></span></div>
        <ol id="aiAdviceItems"></ol><p id="aiAdviceDoctor" class="ai-doctor-note" hidden></p><small id="aiAdviceSource"></small>
    </div>
</section>

</c:if>
<c:if test="${not empty patient}"><section class="card daily-entry-card daily-entry-full"><div class="card-title">Nhập chỉ số hôm nay <c:if test="${not empty todayLog}"><span class="status-pill status-COMPLETED">Đã nhập</span></c:if></div>
<div class="form-group"><label>Thời điểm đo đường huyết</label><select id="log_meal" class="form-control"><option value="">Chọn thời điểm</option><option value="FASTING" ${todayLog.mealType=='FASTING'?'selected':''}>Lúc đói</option><option value="AFTER_MEAL" ${todayLog.mealType=='AFTER_MEAL'?'selected':''}>Sau ăn</option><option value="BEDTIME" ${todayLog.mealType=='BEDTIME'?'selected':''}>Trước khi ngủ</option><option value="OTHER" ${todayLog.mealType=='OTHER'?'selected':''}>Khác</option></select></div>
<div class="form-group"><label>Đường huyết (mg/dL)</label><input type="number" min="20" max="600" step="0.1" id="log_bg" class="form-control" value="${todayLog.bloodGlucose}" placeholder="Ví dụ: 105"></div>
<div class="two-column"><div class="form-group"><label>Huyết áp tâm thu</label><input type="number" min="60" max="260" id="log_sbp" class="form-control" value="${todayLog.systolicBp}" placeholder="120"></div><div class="form-group"><label>Huyết áp tâm trương</label><input type="number" min="30" max="180" id="log_dbp" class="form-control" value="${todayLog.diastolicBp}" placeholder="80"></div></div>
<div class="form-group"><label>Cân nặng (kg)</label><input type="number" min="20" max="300" step="0.1" id="log_weight" class="form-control" value="${todayLog.weight}" placeholder="65"></div>
<div class="form-group"><label>Triệu chứng hôm nay</label><div class="checkbox-grid"><label><input type="checkbox" name="symptom" value="Không có triệu chứng"> Không có</label><label><input type="checkbox" name="symptom" value="Mệt mỏi"> Mệt mỏi</label><label><input type="checkbox" name="symptom" value="Khát nhiều"> Khát nhiều</label><label><input type="checkbox" name="symptom" value="Chóng mặt"> Chóng mặt</label><label><input type="checkbox" name="symptom" value="Run tay/vã mồ hôi"> Run tay/vã mồ hôi</label><c:if test="${diabetesProfile.diabetesType=='TYPE_1'}"><label><input type="checkbox" name="symptom" value="Buồn nôn hoặc đau bụng"> Buồn nôn/đau bụng</label><label><input type="checkbox" name="symptom" value="Thở nhanh hoặc thở sâu"> Thở nhanh/thở sâu</label></c:if><c:if test="${diabetesProfile.diabetesType=='TYPE_2'}"><label><input type="checkbox" name="symptom" value="Tê bì bàn chân"> Tê bì bàn chân</label><label><input type="checkbox" name="symptom" value="Phù chân"> Phù chân</label></c:if></div></div>
<div class="form-group"><label>Ghi chú</label><textarea id="log_note" class="form-control" maxlength="1000" placeholder="Thông tin khác muốn bác sĩ biết"><c:out value="${todayLog.note}"/></textarea></div>
<button type="button" onclick="saveLog()" class="btn btn-primary" id="saveLogBtn">Lưu chỉ số hôm nay</button><div id="logResult" class="form-hint" aria-live="polite"></div></section></c:if>
</main><span id="savedSymptoms" hidden><c:out value="${todayLog.symptoms}"/></span><jsp:include page="footer.jsp"/>
<script>
const CTX='${pageContext.request.contextPath}';
const symptomInputs=[...document.querySelectorAll('[name=symptom]')];
const savedSymptoms=new Set(document.getElementById('savedSymptoms').textContent.split(',').map(value=>value.trim()).filter(Boolean));
symptomInputs.forEach(input=>{
    input.checked=savedSymptoms.has(input.value);
    input.addEventListener('change',()=>{
        const none=symptomInputs.find(item=>item.value==='Không có triệu chứng');
        if (input===none && input.checked) symptomInputs.forEach(item=>{if(item!==none)item.checked=false;});
        if (input!==none && input.checked && none) none.checked=false;
    });
});
async function saveLog(){
    const button=document.getElementById('saveLogBtn');
    const result=document.getElementById('logResult');
    const meal=document.getElementById('log_meal');
    const glucose=document.getElementById('log_bg');
    const systolic=document.getElementById('log_sbp');
    const diastolic=document.getElementById('log_dbp');
    const weight=document.getElementById('log_weight');
    const note=document.getElementById('log_note');
    const fields=[meal,glucose,systolic,diastolic,weight];
    const invalid=fields.find(field=>field.value && !field.checkValidity());
    if (invalid) { invalid.reportValidity(); return; }
    if (glucose.value && !meal.value) { result.textContent='Vui lòng chọn thời điểm đo đường huyết.'; meal.focus(); return; }
    if (meal.value && !glucose.value) { result.textContent='Đã chọn thời điểm đo thì cần nhập đường huyết.'; glucose.focus(); return; }
    const symptoms=symptomInputs.filter(input=>input.checked).map(input=>input.value).join(', ');
    if (!glucose.value&&!systolic.value&&!diastolic.value&&!weight.value&&!symptoms&&!note.value.trim()) { result.textContent='Cần nhập ít nhất một chỉ số hoặc ghi chú.'; return; }
    button.disabled=true; result.textContent='Đang lưu...';
    try{
        const body=new URLSearchParams({action:'saveLog',mealType:meal.value,bloodGlucose:glucose.value,systolicBp:systolic.value,diastolicBp:diastolic.value,weight:weight.value,symptoms,note:note.value});
        const response=await fetch(CTX+'/PatientHealth',{method:'POST',body});
        const data=await response.json();
        result.textContent=data.success?'Đã lưu chỉ số hôm nay.':(data.error||'Không thể lưu dữ liệu.');
    }catch(error){ result.textContent='Không thể kết nối máy chủ.'; }
    finally{ button.disabled=false; }
}
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
        document.getElementById('aiAdviceSource').textContent=(data.source==='OPENAI'?'Lời khuyên từ hệ thống phân tích tự động':'Lời khuyên từ bộ quy tắc an toàn nội bộ')+(data.cached?' · đã lưu trong ngày':'');
    }catch(error){summary.textContent=error.message;items.replaceChildren();}
    finally{button.disabled=false;button.textContent='Nhận lời khuyên';}
}
</script></body></html>
